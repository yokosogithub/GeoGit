/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.osm.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.RevFeature;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import crosby.binary.osmosis.OsmosisReader;

/**
 * Imports data from OSM, whether from a URL that represents an endpoint that supports the OSM
 * overpass api, or from a file with OSM data
 * 
 */
public class OSMImportOp extends AbstractGeoGitOp<Optional<OSMDownloadReport>> {

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

    @Override
    public Optional<OSMDownloadReport> call() {

        checkNotNull(urlOrFilepath);

        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true)
                .setNameFormat("osm-data-download-thread-%d").build();
        final ExecutorService executor = Executors.newFixedThreadPool(3, threadFactory);

        File file;
        if (urlOrFilepath.startsWith("http")) {
            getProgressListener().setDescription("Downloading data...");
            checkNotNull(filter);
            OSMDownloader downloader = new OSMDownloader(urlOrFilepath, executor,
                    getProgressListener());

            if (downloadFile == null) {
                try {
                    downloadFile = File.createTempFile("osm-geogit", ".xml");
                } catch (IOException e) {
                    Throwables.propagate(e);
                }
            } else {
                downloadFile = downloadFile.getAbsoluteFile();
            }

            Future<File> data = downloader.download(filter, downloadFile);

            try {
                file = data.get();
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            } catch (ExecutionException e) {
                throw Throwables.propagate(e);
            }
        } else {
            file = new File(urlOrFilepath);
            Preconditions.checkArgument(file.exists(), "File does not exist: " + urlOrFilepath);
        }

        getProgressListener().setDescription("Importing into GeoGit repo...");

        EntityConverter converter = new EntityConverter();

        OSMDownloadReport report = parseDataFileAndInsert(file, converter);

        if (urlOrFilepath.startsWith("http") && !keepFile) {
            downloadFile.delete();
        }

        return Optional.fromNullable(report);

    }

    private OSMDownloadReport parseDataFileAndInsert(File file, final EntityConverter converter) {

        boolean pbf = false;
        CompressionMethod compression = CompressionMethod.None;

        if (file.getName().endsWith(".pbf")) {
            pbf = true;
        } else if (file.getName().endsWith(".gz")) {
            compression = CompressionMethod.GZip;
        } else if (file.getName().endsWith(".bz2")) {
            compression = CompressionMethod.BZip2;
        }

        RunnableSource reader;

        if (pbf) {
            try {
                reader = new OsmosisReader(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                // should not reach this, because we have already checked existence
                throw new IllegalArgumentException("File does not exist: " + urlOrFilepath);
            }
        } else {
            reader = new XmlReader(file, true, compression);
        }

        ConvertAndImportSink sink = new ConvertAndImportSink(converter);
        reader.setSink(sink);

        Thread readerThread = new Thread(reader);
        readerThread.start();

        while (readerThread.isAlive()) {
            try {
                readerThread.join();
            } catch (InterruptedException e) {
                return null;
            }
        }

        if (sink.getCount() == 0) {
            throw new EmptyOSMDownloadException();
        }

        OSMDownloadReport report = new OSMDownloadReport(sink.getCount(),
                sink.getUnprocessedCount(), sink.getLatestChangeset(), sink.getLatestTimestamp());
        return report;

    }

    /**
     * A sink that processes OSM entities by converting them to GeoGit features and inserting them
     * into the repository working tree
     * 
     */
    class ConvertAndImportSink implements Sink {

        int count = 0;

        int unableToProcessCount = 0;

        private EntityConverter converter;

        private long latestChangeset;

        private long latestTimestamp;

        private FeatureMapFlusher insertsByParent;

        Map<Long, Coordinate> pointCache;

        private boolean firstFeature = true;

        public ConvertAndImportSink(EntityConverter converter) {
            super();
            this.converter = converter;
            this.latestChangeset = 0;
            this.latestTimestamp = 0;
            this.insertsByParent = new FeatureMapFlusher(getWorkTree());
            pointCache = new LinkedHashMap<Long, Coordinate>() {
                /** serialVersionUID */
                private static final long serialVersionUID = 1277795218777240552L;

                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Coordinate> eldest) {
                    return size() == 100000;
                }
            };
        }

        public long getUnprocessedCount() {
            return unableToProcessCount;
        }

        public long getCount() {
            return count;
        }

        public void process(EntityContainer entityContainer) {
            Entity entity = entityContainer.getEntity();
            latestChangeset = Math.max(latestChangeset, entity.getChangesetId());
            latestTimestamp = Math.max(latestTimestamp, entity.getTimestamp().getTime());
            if (entity instanceof Relation || entity instanceof Bound) {
                getProgressListener().progress(count++);
                return;
            }
            Geometry geom = parseGeometry(entity);
            getProgressListener().progress(count++);
            if (geom != null) {
                Feature feature = converter.toFeature(entity, geom);
                if (mapping != null) {
                    Optional<MappedFeature> mapped = mapping.map(feature);
                    if (mapped.isPresent()) {
                        clean();
                        insertsByParent.put(mapped.get().getPath(), (SimpleFeature) mapped.get()
                                .getFeature());
                    }
                    if (!noRaw) {
                        String path = feature.getType().getName().getLocalPart();
                        clean();
                        insertsByParent.put(path, (SimpleFeature) feature);
                    }
                } else {
                    String path = feature.getType().getName().getLocalPart();
                    clean();
                    insertsByParent.put(path, (SimpleFeature) feature);
                }
            }
        }

        private void clean() {
            if (!add && firstFeature) {
                getWorkTree().delete(OSMUtils.NODE_TYPE_NAME);
                getWorkTree().delete(OSMUtils.WAY_TYPE_NAME);
                firstFeature = false;
            }

        }

        public FeatureMapFlusher getFeaturesMap() {
            return insertsByParent;
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

        public void release() {
        }

        public void complete() {
            insertsByParent.flushAll();
        }

        public void initialize(Map<String, Object> map) {
        }

        private final GeometryFactory GEOMF = new GeometryFactory();

        /**
         * Returns the geometry corresponding to an entity. A point in the case of a node, a
         * lineString for a way. Returns null if it could not create the geometry.
         * 
         * This will be the case if the entity is a way but the corresponding nodes cannot be found,
         * and also if the entity is of a type other than Node of Way
         * 
         * @param entity the entity to extract the geometry from
         * @return
         */
        protected Geometry parseGeometry(Entity entity) {

            if (entity instanceof Node) {
                Node node = ((Node) entity);
                Coordinate coord = new Coordinate(node.getLongitude(), node.getLatitude());
                Point pt = GEOMF.createPoint(coord);
                pointCache.put(Long.valueOf(node.getId()), coord);
                return pt;
            }

            final Way way = (Way) entity;
            final List<WayNode> nodes = way.getWayNodes();

            List<Coordinate> coordinates = Lists.newArrayList();
            for (WayNode node : nodes) {
                long nodeId = node.getNodeId();
                Coordinate coord = pointCache.get(nodeId);
                if (coord == null) {
                    String fid = String.valueOf(nodeId);
                    String path = NodeRef.appendChild(OSMUtils.NODE_TYPE_NAME, fid);
                    Optional<org.geogit.api.Node> ref = getWorkTree().findUnstaged(path);
                    if (ref.isPresent()) {
                        org.geogit.api.Node nodeRef = ref.get();
                        RevFeature revFeature = getIndex().getDatabase().getFeature(
                                nodeRef.getObjectId());
                        Point pt = null;
                        ImmutableList<Optional<Object>> values = revFeature.getValues();
                        for (Optional<Object> opt : values) {
                            if (opt.isPresent()) {
                                Object value = opt.get();
                                if (value instanceof Point) {
                                    pt = (Point) value;
                                }
                            }
                        }

                        if (pt != null) {
                            coord = pt.getCoordinate();
                            pointCache.put(Long.valueOf(nodeId), coord);
                        }
                    }
                }
                if (coord != null) {
                    coordinates.add(coord);
                }
            }
            if (coordinates.size() < 2) {
                unableToProcessCount++;
                return null;
            }

            return GEOMF.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
        }
    }

}
