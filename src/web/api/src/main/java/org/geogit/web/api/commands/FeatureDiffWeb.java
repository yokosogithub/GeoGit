/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.commands;

import java.util.HashMap;
import java.util.Map;

import org.geogit.api.CommandLocator;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.diff.AttributeDiff;
import org.geogit.api.plumbing.diff.FeatureDiff;
import org.geogit.api.plumbing.diff.GenericAttributeDiffImpl;
import org.geogit.api.plumbing.diff.GeometryAttributeDiff;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Geometry;

/**
 * This is the interface for the FeatureDiff command. It is used by passing a path to a feature, an
 * oldCommitId and a newCommitId. It returns the differences in the attributes of a feature between
 * the two supplied commits.
 * 
 * Web interface for {@link FeatureDiff}
 */

public class FeatureDiffWeb extends AbstractWebAPICommand {

    private String path;

    private String newTreeish;

    private String oldTreeish;

    private boolean all;

    /**
     * Mutator of the path variable
     * 
     * @param path - the path to the feature
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Mutator for the newTreeish
     * 
     * @param newTreeish - the id of the newer commit
     */
    public void setNewTreeish(String newTreeish) {
        this.newTreeish = newTreeish;
    }

    /**
     * Mutator for the oldTreeish
     * 
     * @param oldTreeish - the id of the older commit
     */
    public void setOldTreeish(String oldTreeish) {
        this.oldTreeish = oldTreeish;
    }

    /**
     * Mutator for all attributes bool
     * 
     * @param all - true to show all attributes not just changed ones
     */
    public void setAll(boolean all) {
        this.all = all;
    }

    /**
     * Helper function to parse the given commit id's feature information
     * 
     * @param id - the id to parse out
     * @param geogit - an instance of geogit to run commands with
     * @return (Optional)NodeRef - the NodeRef that contains the metadata id and id needed to get
     *         the feature and featuretype
     * 
     * @throws CommandSpecException - if the treeid couldn't be resolved
     */
    private Optional<NodeRef> parseID(ObjectId id, CommandLocator geogit) {
        Optional<RevObject> object = geogit.command(RevObjectParse.class).setObjectId(id).call();

        if (object.isPresent()) {
            RevTree tree = (RevTree) object.get();
            return geogit.command(FindTreeChild.class).setParent(tree).setChildPath(path).call();
        } else {
            throw new CommandSpecException("Couldn't resolve treeId");
        }
    }

    /**
     * Runs the command and builds the appropriate response
     * 
     * @param context - the context to use for this command
     * 
     * @throws CommandSpecException
     */
    @Override
    public void run(CommandContext context) {
        if (path == null || path.trim().isEmpty()) {
            throw new CommandSpecException("No path for feature name specifed");
        }

        final CommandLocator geogit = this.getCommandLocator(context);
        ObjectId newId = geogit.command(ResolveTreeish.class).setTreeish(newTreeish).call().get();

        ObjectId oldId = geogit.command(ResolveTreeish.class).setTreeish(oldTreeish).call().get();

        RevFeature newFeature = null;
        RevFeatureType newFeatureType = null;

        RevFeature oldFeature = null;
        RevFeatureType oldFeatureType = null;

        final Map<PropertyDescriptor, AttributeDiff> diffs;

        Optional<NodeRef> ref = parseID(newId, geogit);

        Optional<RevObject> object;

        // need these to determine if the feature was added or removed so I can build the diffs
        // myself until the FeatureDiff supports null values
        boolean removed = false;
        boolean added = false;

        if (ref.isPresent()) {
            object = geogit.command(RevObjectParse.class).setObjectId(ref.get().getMetadataId())
                    .call();
            if (object.isPresent() && object.get() instanceof RevFeatureType) {
                newFeatureType = (RevFeatureType) object.get();
            } else {
                throw new CommandSpecException("Couldn't resolve newCommit's featureType");
            }
            object = geogit.command(RevObjectParse.class).setObjectId(ref.get().objectId()).call();
            if (object.isPresent() && object.get() instanceof RevFeature) {
                newFeature = (RevFeature) object.get();
            } else {
                throw new CommandSpecException("Couldn't resolve newCommit's feature");
            }
        } else {
            removed = true;
        }

        if (!oldId.equals(ObjectId.NULL)) {
            ref = parseID(oldId, geogit);

            if (ref.isPresent()) {
                object = geogit.command(RevObjectParse.class)
                        .setObjectId(ref.get().getMetadataId()).call();
                if (object.isPresent() && object.get() instanceof RevFeatureType) {
                    oldFeatureType = (RevFeatureType) object.get();
                } else {
                    throw new CommandSpecException("Couldn't resolve oldCommit's featureType");
                }
                object = geogit.command(RevObjectParse.class).setObjectId(ref.get().objectId())
                        .call();
                if (object.isPresent() && object.get() instanceof RevFeature) {
                    oldFeature = (RevFeature) object.get();
                } else {
                    throw new CommandSpecException("Couldn't resolve oldCommit's feature");
                }
            } else {
                added = true;
            }
        } else {
            added = true;
        }

        if (removed) {
            Map<PropertyDescriptor, AttributeDiff> tempDiffs = new HashMap<PropertyDescriptor, AttributeDiff>();
            ImmutableList<PropertyDescriptor> attributes = oldFeatureType.sortedDescriptors();
            ImmutableList<Optional<Object>> values = oldFeature.getValues();
            for (int index = 0; index < attributes.size(); index++) {
                Optional<Object> value = values.get(index);
                if (Geometry.class.isAssignableFrom(attributes.get(index).getType().getBinding())) {
                    Optional<Geometry> temp = Optional.absent();
                    if (value.isPresent() || all) {
                        tempDiffs.put(
                                attributes.get(index),
                                new GeometryAttributeDiff(Optional.fromNullable((Geometry) value
                                        .orNull()), temp));
                    }
                } else {
                    if (value.isPresent() || all) {
                        tempDiffs.put(attributes.get(index), new GenericAttributeDiffImpl(value,
                                Optional.absent()));
                    }
                }
            }
            diffs = tempDiffs;
        } else if (added) {
            Map<PropertyDescriptor, AttributeDiff> tempDiffs = new HashMap<PropertyDescriptor, AttributeDiff>();
            ImmutableList<PropertyDescriptor> attributes = newFeatureType.sortedDescriptors();
            ImmutableList<Optional<Object>> values = newFeature.getValues();
            for (int index = 0; index < attributes.size(); index++) {
                Optional<Object> value = values.get(index);
                if (Geometry.class.isAssignableFrom(attributes.get(index).getType().getBinding())) {
                    Optional<Geometry> temp = Optional.absent();
                    if (value.isPresent() || all) {
                        tempDiffs.put(attributes.get(index), new GeometryAttributeDiff(temp,
                                Optional.fromNullable((Geometry) value.orNull())));
                    }
                } else {
                    if (value.isPresent() || all) {
                        tempDiffs.put(attributes.get(index),
                                new GenericAttributeDiffImpl(Optional.absent(), value));
                    }
                }
            }
            diffs = tempDiffs;
        } else {
            FeatureDiff diff = new FeatureDiff(path, newFeature, oldFeature, newFeatureType,
                    oldFeatureType, all);
            diffs = diff.getDiffs();
        }

        context.setResponseContent(new CommandResponse() {

            @Override
            public void write(ResponseWriter out) throws Exception {
                out.start();
                out.writeFeatureDiffResponse(diffs);
                out.finish();
            }
        });
    }
}
