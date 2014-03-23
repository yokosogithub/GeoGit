/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.osm.internal;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.CommandLocator;
import org.geogit.api.NodeRef;
import org.geogit.api.Platform;
import org.geogit.api.ProgressListener;
import org.geogit.api.SubProgressListener;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.repository.FeatureToDelete;
import org.geogit.repository.WorkingTree;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.common.ChangeAction;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlChangeReader;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;

/**
 * Reads a OSM diff file and apply the changes to the current repo.
 * 
 * Changes are filtered to restrict additions to just those new features within the bbox of the
 * current OSM data in the repo, honoring the filter that might have been used to import that
 * preexistent data
 * 
 */

public class OSMApplyDiffOp extends AbstractGeoGitOp<Optional<OSMReport>> {

    /**
     * The file to import
     */
    private File file;

    private Platform platform;

    @Inject
    public OSMApplyDiffOp(Platform platform) {
        this.platform = platform;
    }

    public OSMApplyDiffOp setDiffFile(File file) {
        this.file = file;
        return this;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Optional<OSMReport> call() {
        checkNotNull(file);
        Preconditions.checkArgument(file.exists(), "File does not exist: " + file);

        ProgressListener progressListener = getProgressListener();
        progressListener.setDescription("Applying OSM diff file to GeoGit repo...");

        OSMReport report = parseDiffFileAndInsert();

        return Optional.fromNullable(report);

    }

    public OSMReport parseDiffFileAndInsert() {
        final WorkingTree workTree = getWorkTree();

        final int queueCapacity = 100 * 1000;
        final int timeout = 1;
        final TimeUnit timeoutUnit = TimeUnit.SECONDS;
        // With this iterator and the osm parsing happening on a separate thread, we follow a
        // producer/consumer approach so that the osm parse thread produces features into the
        // iterator's queue, and WorkingTree.insert consumes them on this thread
        QueueIterator<Feature> target = new QueueIterator<Feature>(queueCapacity, timeout,
                timeoutUnit);

        XmlChangeReader reader = new XmlChangeReader(file, true, resolveCompressionMethod(file));

        ProgressListener progressListener = getProgressListener();
        ConvertAndImportSink sink = new ConvertAndImportSink(target, getCommandLocator(),
                getWorkTree(), platform, new SubProgressListener(progressListener, 100));
        reader.setChangeSink(sink);

        Thread readerThread = new Thread(reader, "osm-diff-reader-thread");
        readerThread.start();

        // used to set the task status name, but report no progress so it does not interfere
        // with the progress reported by the reader thread
        SubProgressListener noProgressReportingListener = new SubProgressListener(progressListener,
                0) {
            @Override
            public void setProgress(float progress) {
                // no-op
            }
        };

        Function<Feature, String> parentTreePathResolver = new Function<Feature, String>() {
            @Override
            public String apply(Feature input) {
                return input.getType().getName().getLocalPart();
            }
        };

        workTree.insert(parentTreePathResolver, target, noProgressReportingListener, null, null);

        OSMReport report = new OSMReport(sink.getCount(), sink.getNodeCount(), sink.getWayCount(),
                sink.getUnprocessedCount(), sink.getLatestChangeset(), sink.getLatestTimestamp());
        return report;
    }

    private CompressionMethod resolveCompressionMethod(File file) {
        String fileName = file.getName();
        if (fileName.endsWith(".gz")) {
            return CompressionMethod.GZip;
        } else if (fileName.endsWith(".bz2")) {
            return CompressionMethod.BZip2;
        }
        return CompressionMethod.None;
    }

    /**
     * A sink that processes OSM changes and translates the to the repository working tree
     * 
     */
    static class ConvertAndImportSink implements ChangeSink {

        private static final Function<WayNode, Long> NODELIST_TO_ID_LIST = new Function<WayNode, Long>() {
            @Override
            public Long apply(WayNode input) {
                return Long.valueOf(input.getNodeId());
            }
        };

        private int count = 0;

        private int nodeCount;

        private int wayCount;

        private int unableToProcessCount = 0;

        private EntityConverter converter = new EntityConverter();

        private long latestChangeset;

        private long latestTimestamp;

        private PointCache pointCache;

        private QueueIterator<Feature> target;

        private ProgressListener progressListener;

        private WorkingTree workTree;

        private Geometry bbox;

        public ConvertAndImportSink(QueueIterator<Feature> target, CommandLocator cmdLocator,
                WorkingTree workTree, Platform platform, ProgressListener progressListener) {
            super();
            this.target = target;
            this.workTree = workTree;
            this.progressListener = progressListener;
            this.latestChangeset = 0;
            this.latestTimestamp = 0;
            this.pointCache = new BDBJEPointCache(platform);
            Optional<NodeRef> waysNodeRef = cmdLocator.command(FindTreeChild.class)
                    .setChildPath(OSMUtils.WAY_TYPE_NAME).setParent(workTree.getTree()).call();
            Optional<NodeRef> nodesNodeRef = cmdLocator.command(FindTreeChild.class)
                    .setChildPath(OSMUtils.NODE_TYPE_NAME).setParent(workTree.getTree()).call();
            checkArgument(waysNodeRef.isPresent() || nodesNodeRef.isPresent(),
                    "There is no OSM data currently in the repository");
            Envelope envelope = new Envelope();
            if (waysNodeRef.isPresent()) {
                waysNodeRef.get().expand(envelope);
            }
            if (nodesNodeRef.isPresent()) {
                nodesNodeRef.get().expand(envelope);
            }
            bbox = new GeometryFactory().toGeometry(envelope);
        }

        public long getUnprocessedCount() {
            return unableToProcessCount;
        }

        public long getCount() {
            return count;
        }

        public long getNodeCount() {
            return nodeCount;
        }

        public long getWayCount() {
            return wayCount;
        }

        @Override
        public void complete() {
            progressListener.setProgress(count);
            progressListener.complete();
            target.finish();
            pointCache.dispose();
        }

        @Override
        public void release() {
            pointCache.dispose();
        }

        @Override
        public void process(ChangeContainer container) {
            final EntityContainer entityContainer = container.getEntityContainer();
            final Entity entity = entityContainer.getEntity();
            final ChangeAction changeAction = container.getAction();
            if (changeAction.equals(ChangeAction.Delete)) {
                SimpleFeatureType ft = entity instanceof Node ? OSMUtils.nodeType() : OSMUtils
                        .wayType();
                String id = Long.toString(entity.getId());
                target.put(new FeatureToDelete(ft, id));
                return;
            }
            if (changeAction.equals(ChangeAction.Modify)) {
                // Check that the feature to modify exist. If so, we will just treat it as an
                // addition, overwriting the previous feature
                SimpleFeatureType ft = entity instanceof Node ? OSMUtils.nodeType() : OSMUtils
                        .wayType();
                String path = ft.getName().getLocalPart();
                Optional<org.geogit.api.Node> opt = workTree.findUnstaged(path);
                if (!opt.isPresent()) {
                    return;
                }
            }

            if (++count % 10 == 0) {
                progressListener.setProgress(count);
            }
            latestChangeset = Math.max(latestChangeset, entity.getChangesetId());
            latestTimestamp = Math.max(latestTimestamp, entity.getTimestamp().getTime());
            Geometry geom = null;
            switch (entity.getType()) {
            case Node:
                nodeCount++;
                geom = parsePoint((Node) entity);
                break;
            case Way:
                wayCount++;
                geom = parseLine((Way) entity);
                break;
            default:
                return;
            }
            if (geom != null) {
                if (changeAction.equals(ChangeAction.Create) && geom.within(bbox)
                        || changeAction.equals(ChangeAction.Modify)) {
                    Feature feature = converter.toFeature(entity, geom);
                    target.put(feature);
                }
            }
        }

        /**
         * returns the latest timestamp of all the entities processed so far
         * 
         * @return
         */
        public long getLatestTimestamp() {
            return latestTimestamp;
        }

        /**
         * returns the id of the latest changeset of all the entities processed so far
         * 
         * @return
         */
        public long getLatestChangeset() {
            return latestChangeset;
        }

        public boolean hasProcessedEntities() {
            return latestChangeset != 0;
        }

        @Override
        public void initialize(Map<String, Object> map) {
        }

        private final GeometryFactory GEOMF = new GeometryFactory(
                new PackedCoordinateSequenceFactory());

        protected Geometry parsePoint(Node node) {
            Coordinate coord = new Coordinate(node.getLongitude(), node.getLatitude());
            Point pt = GEOMF.createPoint(coord);
            pointCache.put(Long.valueOf(node.getId()), coord);
            return pt;
        }

        /**
         * @return {@code null} if the way nodes cannot be found, or its list of nodes is too short,
         *         the parsed {@link LineString} otherwise
         */
        @Nullable
        protected Geometry parseLine(Way way) {
            final List<WayNode> nodes = way.getWayNodes();

            if (nodes.size() < 2) {
                unableToProcessCount++;
                return null;
            }

            final List<Long> ids = Lists.transform(nodes, NODELIST_TO_ID_LIST);

            Coordinate[] coordinates = pointCache.get(ids);
            return GEOMF.createLineString(coordinates);
        }
    }

}
