/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.osm.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.ProgressListener;
import org.geogit.api.SubProgressListener;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.osm.internal.log.AddOSMLogEntry;
import org.geogit.osm.internal.log.OSMLogEntry;
import org.geogit.osm.internal.log.OSMMappingLogEntry;
import org.geogit.osm.internal.log.WriteOSMFilterFile;
import org.geogit.osm.internal.log.WriteOSMMappingEntries;
import org.geogit.repository.WorkingTree;
import org.opengis.feature.Feature;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.google.inject.Inject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;

import crosby.binary.osmosis.OsmosisReader;

/**
 * Imports data from OSM, whether from a URL that represents an endpoint that supports the OSM
 * overpass api, or from a file with OSM data
 * 
 */

public class OSMImportOp extends AbstractGeoGitOp<Optional<OSMReport>> {

    /**
     * The filter to use if calling the overpass API
     */
    private String filter;

    /**
     * The URL of file to use for importing
     */
    private String urlOrFilepath;

    private File downloadFile;

    private boolean keepFile;

    private boolean add;

    private Mapping mapping;

    private boolean noRaw;

    private String message;

    private Platform platform;

    @Inject
    public OSMImportOp(Platform platform) {
        this.platform = platform;
    }

    /**
     * Sets the filter to use. It uses the overpass Query Language
     * 
     * @param filter the filter to use
     * @return {@code this}
     */
    public OSMImportOp setFilter(String filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Sets the message to use if a commit is created
     * 
     * @param message the commit message
     * @return {@code this}
     */
    public OSMImportOp setMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Sets the file to which download the response of the OSM server
     * 
     * @param saveFile
     * @return {@code this}
     */
    public OSMImportOp setDownloadFile(File saveFile) {
        this.downloadFile = saveFile;
        return this;
    }

    /**
     * Sets whether, in the case of using a mapping, the raw unmapped data should also be imported
     * or not
     * 
     * @param noRaw True if the raw data should not be imported, but only the mapped data
     * @return {@code this}
     */
    public OSMImportOp setNoRaw(boolean noRaw) {
        this.noRaw = noRaw;
        return this;
    }

    public OSMImportOp setMapping(Mapping mapping) {
        this.mapping = mapping;
        return this;
    }

    /**
     * Sets whether to keep the downloaded file or not
     * 
     * @param keepFiles
     * @return {@code this}
     */
    public OSMImportOp setKeepFile(boolean keepFile) {
        this.keepFile = keepFile;
        return this;
    }

    /**
     * Sets whether to add new data to existing one, or to remove existing data before importing
     * 
     * @param add
     * @return {@code this}
     */
    public OSMImportOp setAdd(boolean add) {
        this.add = add;
        return this;
    };

    /**
     * Sets the source of OSM data. Can be the URL of an endpoint supporting the overpass API, or a
     * filepath
     * 
     * @param urlOrFilepath
     * @return{@code this}
     */
    public OSMImportOp setDataSource(String urlOrFilepath) {
        this.urlOrFilepath = urlOrFilepath;
        return this;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Optional<OSMReport> call() {

        checkNotNull(urlOrFilepath);

        ObjectId oldTreeId = getWorkTree().getTree().getId();

        File osmDataFile = null;
        final InputStream osmDataStream;
        if (urlOrFilepath.startsWith("http")) {
            osmDataStream = downloadFile();
        } else {
            osmDataFile = new File(urlOrFilepath);
            Preconditions.checkArgument(osmDataFile.exists(), "File does not exist: "
                    + urlOrFilepath);
            try {
                osmDataStream = new BufferedInputStream(new FileInputStream(osmDataFile),
                        1024 * 1024);
            } catch (FileNotFoundException e) {
                throw Throwables.propagate(e);
            }
        }

        ProgressListener progressListener = getProgressListener();
        progressListener.setDescription("Importing into GeoGit repo...");

        EntityConverter converter = new EntityConverter();

        OSMReport report;
        try {
            report = parseDataFileAndInsert(osmDataFile, osmDataStream, converter);
        } finally {
            Closeables.closeQuietly(osmDataStream);
        }

        if (report != null) {
            ObjectId newTreeId = getWorkTree().getTree().getId();
            if (!noRaw) {
                if (mapping != null || filter != null) {
                    progressListener.setDescription("Staging features...");
                    command(AddOp.class).setProgressListener(progressListener).call();
                    progressListener.setDescription("Committing features...");
                    command(CommitOp.class).setMessage(message)
                            .setProgressListener(progressListener).call();
                    OSMLogEntry entry = new OSMLogEntry(newTreeId, report.getLatestChangeset(),
                            report.getLatestTimestamp());
                    command(AddOSMLogEntry.class).setEntry(entry).call();
                    if (filter != null) {
                        command(WriteOSMFilterFile.class).setEntry(entry).setFilterCode(filter)
                                .call();
                    }
                    if (mapping != null) {
                        command(WriteOSMMappingEntries.class).setMapping(mapping)
                                .setMappingLogEntry(new OSMMappingLogEntry(oldTreeId, newTreeId))
                                .call();
                    }
                }
            }
        }

        return Optional.fromNullable(report);

    }

    private InputStream downloadFile() {

        ProgressListener listener = getProgressListener();
        checkNotNull(filter);
        OSMDownloader downloader = new OSMDownloader(urlOrFilepath, listener);
        listener.setDescription("Connecting to " + urlOrFilepath + "...");
        File destination = null;
        if (keepFile) {
            destination = this.downloadFile;
            if (destination == null) {
                try {
                    destination = File.createTempFile("osm-geogit", ".xml");
                } catch (IOException e) {
                    Throwables.propagate(e);
                }
            } else {
                destination = destination.getAbsoluteFile();
            }
        }
        try {
            InputStream dataStream = downloader.download(filter, destination);
            if (keepFile) {
                listener.setDescription("Downloaded data will be kept in "
                        + destination.getAbsolutePath());
            }
            return dataStream;
        } catch (Exception e) {
            throw Throwables.propagate(Throwables.getRootCause(e));
        }
    }

    private OSMReport parseDataFileAndInsert(@Nullable File file, final InputStream dataIn,
            final EntityConverter converter) {

        final boolean pbf;
        final CompressionMethod compression;
        if (file == null) {
            pbf = false;
            compression = CompressionMethod.None;
        } else {
            pbf = file.getName().endsWith(".pbf");
            compression = resolveCompressionMethod(file);
        }

        RunnableSource reader;
        if (pbf) {
            reader = new OsmosisReader(dataIn);
        } else {
            reader = new org.geogit.osm.internal.XmlReader(dataIn, true, compression);
        }

        final WorkingTree workTree = getWorkTree();
        if (!add) {
            workTree.delete(OSMUtils.NODE_TYPE_NAME);
            workTree.delete(OSMUtils.WAY_TYPE_NAME);
        }

        final int queueCapacity = 100 * 1000;
        final int timeout = 1;
        final TimeUnit timeoutUnit = TimeUnit.SECONDS;
        // With this iterator and the osm parsing happening on a separate thread, we follow a
        // producer/consumer approach so that the osm parse thread produces featrures into the
        // iterator's queue, and WorkingTree.insert consumes them on this thread
        QueueIterator<Feature> iterator = new QueueIterator<Feature>(queueCapacity, timeout,
                timeoutUnit);

        ProgressListener progressListener = getProgressListener();
        ConvertAndImportSink sink = new ConvertAndImportSink(converter, iterator, platform,
                mapping, noRaw, new SubProgressListener(progressListener, 100));
        reader.setSink(sink);

        Thread readerThread = new Thread(reader, "osm-import-reader-thread");
        readerThread.start();

        Function<Feature, String> parentTreePathResolver = new Function<Feature, String>() {
            @Override
            public String apply(Feature input) {
                if (input instanceof MappedFeature) {
                    return ((MappedFeature) input).getPath();
                }
                return input.getType().getName().getLocalPart();
            }
        };

        // used to set the task status name, but report no progress so it does not interfere
        // with the progress reported by the reader thread
        SubProgressListener noPorgressReportingListener = new SubProgressListener(progressListener,
                0) {
            @Override
            public void setProgress(float progress) {
                // no-op
            }
        };
        workTree.insert(parentTreePathResolver, iterator, noPorgressReportingListener, null, null);

        if (sink.getCount() == 0) {
            throw new EmptyOSMDownloadException();
        }

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
     * A sink that processes OSM entities by converting them to GeoGit features and inserting them
     * into the repository working tree
     * 
     */
    static class ConvertAndImportSink implements Sink {

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

        private EntityConverter converter;

        private long latestChangeset;

        private long latestTimestamp;

        private PointCache pointCache;

        private QueueIterator<Feature> target;

        private ProgressListener progressListener;

        private Mapping mapping;

        private boolean noRaw;

        private Stopwatch sw;

        public ConvertAndImportSink(EntityConverter converter, QueueIterator<Feature> target,
                Platform platform, Mapping mapping, boolean noRaw, ProgressListener progressListener) {
            super();
            this.converter = converter;
            this.target = target;
            this.mapping = mapping;
            this.noRaw = noRaw;
            this.progressListener = progressListener;
            this.latestChangeset = 0;
            this.latestTimestamp = 0;
            this.pointCache = new BDBJEPointCache(platform);
            this.sw = new Stopwatch().start();
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
            sw.stop();
            progressListener.setDescription(String
                    .format("%,d entities processed in %s", count, sw));
        }

        @Override
        public void release() {
            pointCache.dispose();
        }

        @Override
        public void process(EntityContainer entityContainer) {
            Entity entity = entityContainer.getEntity();
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

                @Nullable
                Feature feature = converter.toFeature(entity, geom);
                if (mapping != null && feature != null) {
                    Optional<MappedFeature> mapped = mapping.map(feature);
                    if (mapped.isPresent()) {
                        MappedFeature mappedFeature = mapped.get();
                        target.put(mappedFeature);
                    }
                }
                if (feature == null || noRaw) {
                    return;
                }
                target.put(feature);

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
