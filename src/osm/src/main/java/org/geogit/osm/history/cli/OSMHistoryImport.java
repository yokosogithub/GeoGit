/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.osm.history.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.relation.Relation;
import javax.xml.namespace.QName;

import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.history.internal.Change;
import org.geogit.osm.history.internal.Changeset;
import org.geogit.osm.history.internal.HistoryDownloader;
import org.geogit.osm.history.internal.Node;
import org.geogit.osm.history.internal.Primitive;
import org.geogit.osm.history.internal.Way;
import org.geogit.repository.Repository;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.geometry.BoundingBox;
import org.opengis.util.ProgressListener;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.internal.Lists;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 *
 */
@Parameters(commandNames = "import-history", commandDescription = "Import OpenStreetmap history")
public class OSMHistoryImport extends AbstractCommand implements CLICommand {

    /** FeatureType namespace */
    private static final String NAMESPACE = "www.openstreetmap.org";

    /** NODE */
    private static final String NODE_TYPE_NAME = "node";

    /** WAY */
    private static final String WAY_TYPE_NAME = "way";

    private static final GeometryFactory GEOMF = new GeometryFactory();

    @ParametersDelegate
    public HistoryImportArgs args = new HistoryImportArgs();

    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());
        checkArgument(args.numThreads > 0 && args.numThreads < 7,
                "numthreads must be between 1 and 6");

        ConsoleReader console = cli.getConsole();

        final String osmAPIUrl = resolveAPIURL();
        console.println("Obtaining OSM changesets " + args.startIndex + " to " + args.endIndex
                + " from " + osmAPIUrl);

        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true)
                .setNameFormat("osm-history-fetch-thread-%d").build();
        final ExecutorService executor = Executors.newFixedThreadPool(args.numThreads,
                threadFactory);
        final File targetDir = resolveTargetDir();
        console.println("Downloading to " + targetDir.getAbsolutePath());
        console.println("Files will " + (args.keepFiles ? "" : " not ")
                + "be kept on the download directory.");
        console.flush();

        HistoryDownloader downloader;
        downloader = new HistoryDownloader(osmAPIUrl, targetDir, args.startIndex, args.endIndex,
                executor, args.keepFiles);
        try {
            importOsmHistory(cli, console, downloader);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private File resolveTargetDir() throws IOException {
        final File targetDir;
        if (args.saveFolder == null) {
            try {
                File tmp = new File(System.getProperty("java.io.tmpdir"), "changesets.osm");
                tmp.mkdirs();
                targetDir = tmp;
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        } else {
            if (!args.saveFolder.exists() && !args.saveFolder.mkdirs()) {
                throw new IllegalArgumentException("Unable to create directory "
                        + args.saveFolder.getAbsolutePath());
            }
            targetDir = args.saveFolder;
        }
        return targetDir;
    }

    private String resolveAPIURL() {
        String osmAPIUrl;
        if (args.useTestApiEndpoint) {
            osmAPIUrl = HistoryImportArgs.DEVELOPMENT_API_ENDPOINT;
        } else if (args.apiUrl.isEmpty()) {
            osmAPIUrl = HistoryImportArgs.DEFAULT_API_ENDPOINT;
        } else {
            osmAPIUrl = args.apiUrl.get(0);
        }
        return osmAPIUrl;
    }

    private void importOsmHistory(GeogitCLI cli, ConsoleReader console, HistoryDownloader downloader)
            throws IOException, InterruptedException {
        Optional<Changeset> set;
        while ((set = downloader.fetchNextChangeset()).isPresent()) {
            Changeset changeset = set.get();

            String desc = "downloading changes of changeset " + changeset.getId() + "...";
            console.print(desc);
            console.flush();

            // ProgressListener listener = cli.getProgressListener();
            // listener.dispose();
            // listener.setCanceled(false);
            // listener.progress(0f);
            // listener.setDescription(desc);
            // listener.started();

            Iterator<Change> changes = changeset.getChanges().get();
            console.print("inserting...");
            insertAndAddChanges(cli, changes);
            // listener.progress(100f);
            // listener.complete();

            commit(cli, changeset);
        }
    }

    /**
     * @param cli
     * @param changeset
     * @throws IOException
     */
    private void commit(GeogitCLI cli, Changeset changeset) throws IOException {
        ConsoleReader console = cli.getConsole();
        console.print("Committing changeset " + changeset.getId() + "...");
        console.flush();

        CommitOp command = cli.getGeogit().command(CommitOp.class);
        command.setAllowEmpty(true);
        String message = "";
        if (changeset.getComment().isPresent()) {
            message = changeset.getComment().get() + "\nchangeset " + changeset.getId();
        } else {
            message = "changeset " + changeset.getId();
        }
        command.setMessage(message);
        command.setAuthor(changeset.getUserName(), null);
        command.setTimestamp(changeset.getClosed());
        ProgressListener listener = cli.getProgressListener();
        listener.dispose();
        listener.setCanceled(false);
        listener.progress(0f);
        listener.started();
        command.setProgressListener(listener);
        try {
            command.call();
            listener.complete();
            console.println("done.");
            console.flush();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private final AtomicBoolean hasMore = new AtomicBoolean();

    private final BlockingQueue<Change> queue = new LinkedBlockingQueue<Change>(100);

    /**
     * @param cli
     * @param changes
     * @throws IOException
     */
    private void insertAndAddChanges(GeogitCLI cli, final Iterator<Change> changes)
            throws IOException {
        if (!changes.hasNext()) {
            return;
        }
        final GeoGIT geogit = cli.getGeogit();
        final Repository repository = geogit.getRepository();
        final WorkingTree workTree = repository.getWorkingTree();

        Map<Long, Coordinate> thisChangePointCache = new LinkedHashMap<Long, Coordinate>() {
            /** serialVersionUID */
            private static final long serialVersionUID = 1277795218777240552L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Coordinate> eldest) {
                return size() == 10000;
            }
        };

        final ExecutorService executor = Executors.newFixedThreadPool(2);
        hasMore.set(true);

        executor.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    while (changes.hasNext()) {
                        Change change = changes.next();
                        queue.put(change);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                } finally {
                    hasMore.set(false);
                }
            }
        });

        int cnt = 0;
        while (hasMore.get() || queue.size() > 0) {
            Change change;
            try {
                change = queue.poll(100, TimeUnit.MILLISECONDS);
                if (change == null) {
                    continue;
                }
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }
            final String featurePath = featurePath(change);
            if (featurePath == null) {
                continue;// ignores relations
            }
            cnt++;
            if (Change.Type.delete.equals(change.getType())) {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        String parentPath = NodeRef.parentPath(featurePath);
                        String fid = NodeRef.nodeFromPath(featurePath);
                        workTree.delete(parentPath, fid);
                    }
                });
            } else {
                final Primitive primitive = change.getNode().isPresent() ? change.getNode().get()
                        : change.getWay().get();
                final Geometry geom = parseGeometry(geogit, primitive, thisChangePointCache);
                if (geom instanceof Point) {
                    thisChangePointCache.put(Long.valueOf(primitive.getId()),
                            ((Point) geom).getCoordinate());
                }
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        RevFeature feature = new OsmRevFeature(primitive, geom);
                        String parentPath = NodeRef.parentPath(featurePath);
                        workTree.insert(parentPath, feature);
                    }
                });
            }
        }

        executor.shutdown();
        try {
            while (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                ;
            }
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }

        ConsoleReader console = cli.getConsole();
        console.print("Inserted " + cnt + " changes, staging...");
        console.flush();

        geogit.command(AddOp.class).call();

        console.println("done.");
        console.flush();
    }

    /**
     * @param primitive
     * @param thisChangePointCache
     * @return
     */
    private Geometry parseGeometry(GeoGIT geogit, Primitive primitive,
            Map<Long, Coordinate> thisChangePointCache) {

        if (primitive instanceof Relation) {
            return null;
        }

        if (primitive instanceof Node) {
            Optional<Point> location = ((Node) primitive).getLocation();
            return location.orNull();
        }

        final Way way = (Way) primitive;
        final ImmutableList<Long> nodes = way.getNodes();

        final ObjectSerialisingFactory serialFactory = geogit.getRepository()
                .getSerializationFactory();
        StagingArea index = geogit.getRepository().getIndex();

        List<Coordinate> coordinates = Lists.newArrayList(nodes.size());
        for (Long nodeId : nodes) {
            Coordinate coord = thisChangePointCache.get(nodeId);
            if (coord == null) {
                String fid = String.valueOf(nodeId);
                String path = NodeRef.appendChild(NODE_TYPE_NAME, fid);
                Optional<NodeRef> ref = index.findStaged(path);
                if (!ref.isPresent()) {
                    ref = geogit.command(FindTreeChild.class).setChildPath(path).call();
                }
                if (ref.isPresent()) {
                    NodeRef nodeRef = ref.get();
                    ObjectReader<RevFeature> reader;
                    reader = serialFactory.createFeatureReader(NODE_REV_TYPE, fid);
                    RevFeature feature = index.getDatabase().get(nodeRef.getObjectId(), reader);
                    Point p = (Point) ((SimpleFeature) feature.feature()).getAttribute("location");
                    if (p != null) {
                        coord = p.getCoordinate();
                        thisChangePointCache.put(Long.valueOf(nodeId), coord);
                    }
                }
            }
            if (coord != null) {
                coordinates.add(coord);
            }
        }
        if (coordinates.size() < 2) {
            return null;
        }
        return GEOMF.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
    }

    /**
     * @param change
     * @return
     */
    private String featurePath(Change change) {
        if (change.getRelation().isPresent()) {
            return null;// ignore relations for the time being
        }
        if (change.getNode().isPresent()) {
            String fid = String.valueOf(change.getNode().get().getId());
            return NodeRef.appendChild(NODE_TYPE_NAME, fid);
        }
        String fid = String.valueOf(change.getWay().get().getId());
        return NodeRef.appendChild(WAY_TYPE_NAME, fid);
    }

    private static SimpleFeatureType NodeType;

    private static SimpleFeatureType WayType;

    private static SimpleFeatureType RelationType;

    private synchronized static SimpleFeatureType nodeType() {
        if (NodeType == null) {
            String typeSpec = "visible:Boolean,version:Integer,timestamp:java.lang.Long,location:Point:4326";
            try {
                NodeType = DataUtilities.createType(NAMESPACE, NODE_TYPE_NAME, typeSpec);
            } catch (SchemaException e) {
                throw Throwables.propagate(e);
            }
        }
        return NodeType;
    }

    private synchronized static SimpleFeatureType wayType() {
        if (WayType == null) {
            String typeSpec = "visible:Boolean,version:Integer,timestamp:java.lang.Long,way:LineString:4326";
            try {
                WayType = DataUtilities.createType(NAMESPACE, NODE_TYPE_NAME, typeSpec);
            } catch (SchemaException e) {
                throw Throwables.propagate(e);
            }
        }
        return WayType;
    }

    public static class OsmRevFeatureTypeNode extends RevFeatureType {

        private final QName name;

        /**
         * @param parsed
         */
        public OsmRevFeatureTypeNode() {
            super(nodeType());
            Name typeName = nodeType().getName();
            name = new QName(typeName.getNamespaceURI(), typeName.getLocalPart());
        }

        @Override
        public QName getName() {
            return name;
        }
    }

    public static class OsmRevFeatureTypeWay extends RevFeatureType {

        private final QName name;

        /**
         * @param parsed
         */
        public OsmRevFeatureTypeWay() {
            super(wayType());
            Name typeName = wayType().getName();
            name = new QName(typeName.getNamespaceURI(), typeName.getLocalPart());
        }

        @Override
        public QName getName() {
            return name;
        }
    }

    private static final RevFeatureType NODE_REV_TYPE = new OsmRevFeatureTypeNode();

    private static final RevFeatureType WAY_REV_TYPE = new OsmRevFeatureTypeWay();

    public static class OsmRevFeature extends RevFeature {

        private RevFeatureType featureType;

        /**
         * @param feature
         * @param geom
         */
        public OsmRevFeature(Primitive feature, Geometry geom) {
            super(toFeature(feature, geom));
            if (feature instanceof Node) {
                featureType = NODE_REV_TYPE;
            } else if (feature instanceof Way) {
                featureType = WAY_REV_TYPE;
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public SimpleFeature feature() {
            return (SimpleFeature) super.feature();
        }

        @Override
        public RevFeatureType getFeatureType() {
            return featureType;
        }

        @Override
        public BoundingBox getBounds() {
            return feature().getBounds();
        }

        @Override
        public QName getName() {
            return getFeatureType().getName();
        }

        @Override
        public String getFeatureId() {
            return feature().getID();
        }

        @Override
        public boolean isUseProvidedFid() {
            return true;
        }

        private static SimpleFeature toFeature(Primitive feature, Geometry geom) {

            SimpleFeatureType ft = feature instanceof Node ? nodeType() : wayType();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(ft);

            // "visible:Boolean,version:Int,timestamp:long,[location:Point | way:LineString];
            builder.set("visible", Boolean.valueOf(feature.isVisible()));
            builder.set("version", Integer.valueOf(feature.getVersion()));
            builder.set("timestamp", Long.valueOf(feature.getTimestamp()));

            if (feature instanceof Node) {
                builder.set("location", geom);
            } else if (feature instanceof Way) {
                builder.set("way", geom);
            } else {
                throw new IllegalArgumentException();
            }

            String fid = String.valueOf(feature.getId());
            SimpleFeature simpleFeature = builder.buildFeature(fid);
            return simpleFeature;
        }
    }
}
