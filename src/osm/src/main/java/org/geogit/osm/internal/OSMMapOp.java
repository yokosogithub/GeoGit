/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.osm.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.FeatureBuilder;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.api.plumbing.LsTreeOp.Strategy;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.osm.internal.log.OSMMappingLogEntry;
import org.geogit.osm.internal.log.WriteOSMMappingEntries;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;

/**
 * Creates new data in a geogit repository, based on the current OSM data in the repository and a
 * mapping that defines the schema to use for creating new features and the destination trees.
 * 
 * The source data used is the working tree data in the "node" and "way" trees.
 * 
 */
public class OSMMapOp extends AbstractGeoGitOp<RevTree> {

    /**
     * The mapping to use
     */
    private Mapping mapping;

    /**
     * The message to use for the commit to create
     */
    private String message;

    /**
     * Sets the mapping to use
     * 
     * @param mapping the mapping to use
     * @return {@code this}
     */
    public OSMMapOp setMapping(Mapping mapping) {
        this.mapping = mapping;
        return this;
    }

    /**
     * Sets the message to use for the commit that is created
     * 
     * @param message the commit message
     * @return {@code this}
     */
    public OSMMapOp setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public RevTree call() {

        checkNotNull(mapping);

        long staged = getIndex().countStaged(null).getCount();
        long unstaged = getWorkTree().countUnstaged(null).getCount();
        Preconditions.checkState((staged == 0 && unstaged == 0),
                "You must have a clean working tree and index to perform a mapping.");

        ObjectId oldTreeId = getWorkTree().getTree().getId();

        Iterator<Feature> nodes;
        if (mapping.canUseNodes()) {
            nodes = getFeatures("WORK_HEAD:node");
        } else {
            nodes = Iterators.emptyIterator();
        }
        Iterator<Feature> ways;
        if (mapping.canUseWays()) {
            ways = getFeatures("WORK_HEAD:way");
        } else {
            ways = Iterators.emptyIterator();
        }
        Iterator<Feature> iterator = Iterators.concat(nodes, ways);

        if (iterator.hasNext()) {
            FeatureMapFlusher insertsByParent = new FeatureMapFlusher(getWorkTree());
            while (iterator.hasNext()) {
                Feature feature = iterator.next();
                Optional<MappedFeature> newFeature = mapping.map(feature);
                if (newFeature.isPresent()) {
                    String path = newFeature.get().getPath();
                    SimpleFeature sf = (SimpleFeature) newFeature.get().getFeature();
                    insertsByParent.put(path, sf);
                }
            }
            insertsByParent.flushAll();

            ObjectId newTreeId = getWorkTree().getTree().getId();
            // If the mapping generates the same mapped features that already exist, we do nothing
            if (!newTreeId.equals(oldTreeId)) {
                command(AddOp.class).call();
                command(CommitOp.class).setMessage(message).call();
                command(WriteOSMMappingEntries.class).setMapping(mapping)
                        .setMappingLogEntry(new OSMMappingLogEntry(oldTreeId, newTreeId)).call();
            }

        }

        return getWorkTree().getTree();

    }

    private Iterator<Feature> getFeatures(String ref) {
        Optional<ObjectId> id = command(RevParse.class).setRefSpec(ref).call();
        if (!id.isPresent()) {
            return Iterators.emptyIterator();
        }
        LsTreeOp op = command(LsTreeOp.class).setStrategy(Strategy.DEPTHFIRST_ONLY_FEATURES)
                .setReference(ref);

        Iterator<NodeRef> iterator = op.call();

        Function<NodeRef, Feature> nodeRefToFeature = new Function<NodeRef, Feature>() {

            private final Map<String, FeatureBuilder> builders = //
            ImmutableMap.<String, FeatureBuilder> of(//
                    OSMUtils.NODE_TYPE_NAME, //
                    new FeatureBuilder(RevFeatureType.build(OSMUtils.nodeType())), //
                    OSMUtils.WAY_TYPE_NAME,//
                    new FeatureBuilder(RevFeatureType.build(OSMUtils.wayType())));

            private final RevObjectParse parseCommand = command(RevObjectParse.class);

            @Override
            @Nullable
            public Feature apply(@Nullable NodeRef ref) {
                RevFeature revFeature = parseCommand.setObjectId(ref.objectId())
                        .call(RevFeature.class).get();
                final String parentPath = ref.getParentPath();
                FeatureBuilder featureBuilder = builders.get(parentPath);
                String fid = ref.name();
                Feature feature = featureBuilder.build(fid, revFeature);
                return feature;
            }

        };
        return Iterators.transform(iterator, nodeRefToFeature);
    }
}
