/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.data;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

import org.geogit.api.Bounded;
import org.geogit.api.Bucket;
import org.geogit.api.CommandLocator;
import org.geogit.api.FeatureBuilder;
import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.Ref;
import org.geogit.api.RevFeature;
import org.geogit.api.RevObject;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.api.plumbing.LsTreeOp.Strategy;
import org.geogit.api.plumbing.RevObjectParse;
import org.geotools.data.FeatureReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.filter.visitor.SpatialFilterVisitor;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.vividsolutions.jts.geom.Envelope;

/**
 *
 */
public class GeogitFeatureReader<T extends FeatureType, F extends Feature> implements
        FeatureReader<T, F> {

    private SimpleFeatureType schema;

    private FeatureBuilder featureBuilder;

    private Stats stats;

    private RevObjectParse parseRevFeatureCommand;

    private UnmodifiableIterator<SimpleFeature> features;

    private static class Stats {
        public int featureHits, featureMisses, treeHits, treeMisses, bucketHits, bucketMisses;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Hits/misses:\n");
            sb.append("Trees: ").append(treeHits).append("/").append(treeMisses).append("\n");
            sb.append("Features: ").append(featureHits).append("/").append(featureMisses)
                    .append("\n");
            sb.append("Buckets: ").append(bucketHits).append("/").append(bucketMisses).append("\n");
            return sb.toString();
        }
    }

    /**
     * @param commandLocator
     * @param schema
     * @param typeTree
     * @param filter
     * @param queryBounds
     */
    public GeogitFeatureReader(final CommandLocator commandLocator, final SimpleFeatureType schema,
            final NodeRef treeRef, final Filter origFilter, @Nullable final String branch) {

        this.schema = schema;
        this.stats = new Stats();

        final String branchRef = branch == null ? Ref.WORK_HEAD : branch;
        String refSpec = branchRef + ":" + treeRef.path();

        final Filter filter = reprojectFilter(origFilter);
        final Envelope queryBounds = getQueryBounds(filter);

        Predicate<Bounded> refBoundsFilter = Predicates.alwaysTrue();
        if (!queryBounds.isNull()) {
            refBoundsFilter = new Predicate<Bounded>() {
                private final Envelope env = queryBounds;

                @Override
                public boolean apply(final Bounded bounded) {
                    boolean intersects = bounded.intersects(env);
                    if (bounded instanceof Bucket) {
                        // {
                        // Envelope e = new Envelope();
                        // bounded.expand(e);
                        // stats.geoms.add(JTS.toGeometry(e));
                        // }
                        if (intersects)
                            stats.bucketHits++;
                        else
                            stats.bucketMisses++;
                    } else {
                        Node node;
                        if (bounded instanceof NodeRef) {
                            node = ((NodeRef) bounded).getNode();
                        } else {
                            node = (Node) bounded;
                        }
                        if (node.getType().equals(RevObject.TYPE.TREE)) {
                            if (intersects)
                                stats.treeHits++;
                            else
                                stats.treeMisses++;
                        } else {
                            if (intersects)
                                stats.featureHits++;
                            else
                                stats.featureMisses++;
                        }
                    }
                    return intersects;
                }
            };
        }

        final Iterator<NodeRef> featureRefs = commandLocator.command(LsTreeOp.class)
                .setStrategy(Strategy.FEATURES_ONLY).setReference(refSpec)
                .setBoundsFilter(refBoundsFilter).call();

        this.featureBuilder = new FeatureBuilder(schema);
        this.parseRevFeatureCommand = commandLocator.command(RevObjectParse.class);

        final Iterator<SimpleFeature> featuresUnfiltered = Iterators.transform(featureRefs,
                new Function<NodeRef, SimpleFeature>() {

                    @Override
                    public SimpleFeature apply(final NodeRef featureRef) {
                        Optional<RevFeature> revFeature = parseRevFeatureCommand.setObjectId(
                                featureRef.objectId()).call(RevFeature.class);
                        Preconditions.checkState(revFeature.isPresent());

                        String id = featureRef.name();
                        Feature feature = featureBuilder.build(id, revFeature.get());
                        return (SimpleFeature) feature;
                    }
                });
        this.features = Iterators.filter(featuresUnfiltered, new Predicate<SimpleFeature>() {

            @Override
            public boolean apply(SimpleFeature feature) {
                return filter.evaluate(feature);
            }
        });
    }

    private Envelope getQueryBounds(Filter filter) {

        final Envelope queryBounds = new Envelope();
        Envelope bounds = (Envelope) filter.accept(new ExtractBounds(), queryBounds);
        if (bounds != null) {
            queryBounds.expandToInclude(bounds);
        }
        return queryBounds;
    }

    /**
     * @param filter
     * @return
     */
    private Filter reprojectFilter(Filter filter) {
        if (hasSpatialFilter(filter)) {
            FilterFactory2 factory = CommonFactoryFinder.getFilterFactory2();
            filter = (Filter) filter.accept(new ReprojectingFilterVisitor(factory, schema), null);
        }
        return filter;
    }

    private boolean hasSpatialFilter(Filter filter) {
        SpatialFilterVisitor spatialFilterVisitor = new SpatialFilterVisitor();
        filter.accept(spatialFilterVisitor, null);
        return spatialFilterVisitor.hasSpatialFilter();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getFeatureType() {
        return (T) schema;
    }

    @Override
    public void close() throws IOException {
        //
        System.err.println(stats.toString());
        // GeometryCollection collection = new
        // GeometryFactory().createGeometryCollection(stats.geoms
        // .toArray(new Geometry[stats.geoms.size()]));
        // System.err.println(collection);
    }

    @Override
    public boolean hasNext() throws IOException {
        return features.hasNext();
    }

    @SuppressWarnings("unchecked")
    @Override
    public F next() throws IOException, IllegalArgumentException, NoSuchElementException {
        return (F) features.next();
    }

}
