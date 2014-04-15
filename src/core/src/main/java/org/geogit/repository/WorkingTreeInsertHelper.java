/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.geogit.api.CommandLocator;
import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindOrCreateSubtree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.storage.ObjectDatabase;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.geometry.BoundingBox;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Envelope;

class WorkingTreeInsertHelper {

    private final ObjectDatabase indexDatabase;

    private final CommandLocator commandLocator;

    private final RevTree workHead;

    private Function<Feature, String> treePathResolver;

    private final Map<String, RevTreeBuilder2> treeBuilders = Maps.newHashMap();

    private final ExecutorService executorService;

    public WorkingTreeInsertHelper(ObjectDatabase db, CommandLocator cmdLocator, RevTree workHead,
            final Function<Feature, String> treePathResolver, final ExecutorService executorService) {

        this.indexDatabase = db;
        this.commandLocator = cmdLocator;
        this.workHead = workHead;
        this.treePathResolver = treePathResolver;
        this.executorService = executorService;
    }

    public List<String> getTreeNames() {
        return new ArrayList<String>(treeBuilders.keySet());
    }

    public Node put(final ObjectId revFeatureId, final Feature feature) {

        final RevTreeBuilder2 treeBuilder = getTreeBuilder(feature);

        String fid = feature.getIdentifier().getID();
        BoundingBox bounds = feature.getBounds();
        FeatureType type = feature.getType();

        final Node node = treeBuilder.putFeature(revFeatureId, fid, bounds, type);
        return node;
    }

    public void remove(FeatureToDelete feature) {
        final RevTreeBuilder2 treeBuilder = getTreeBuilder(feature);

        String fid = feature.getIdentifier().getID();
        treeBuilder.removeFeature(fid);
    }

    private RevTreeBuilder2 getTreeBuilder(final Feature feature) {

        final String treePath = treePathResolver.apply(feature);
        RevTreeBuilder2 builder = treeBuilders.get(treePath);
        if (builder == null) {
            FeatureType type = feature.getType();
            builder = createBuilder(treePath, type);
            treeBuilders.put(treePath, builder);
        }
        return builder;
    }

    private NodeRef findOrCreateTree(final String treePath, final FeatureType type) {

        RevTree tree = commandLocator.command(FindOrCreateSubtree.class).setChildPath(treePath)
                .setIndex(true).setParent(workHead).setParentPath(NodeRef.ROOT).call();

        ObjectId metadataId = ObjectId.NULL;
        if (type != null) {
            RevFeatureType revFeatureType = RevFeatureType.build(type);
            if (tree.isEmpty()) {
                indexDatabase.put(revFeatureType);
            }
            metadataId = revFeatureType.getId();
        }
        Envelope bounds = SpatialOps.boundsOf(tree);
        Node node = Node.create(NodeRef.nodeFromPath(treePath), tree.getId(), metadataId,
                TYPE.TREE, bounds);

        String parentPath = NodeRef.parentPath(treePath);
        return new NodeRef(node, parentPath, ObjectId.NULL);
    }

    private RevTreeBuilder2 createBuilder(String treePath, FeatureType type) {

        final NodeRef treeRef = findOrCreateTree(treePath, type);
        final ObjectId treeId = treeRef.objectId();
        final RevTree origTree = treeId.isNull() ? RevTree.EMPTY : indexDatabase.getTree(treeId);

        ObjectId defaultMetadataId = treeRef.getMetadataId();

        RevTreeBuilder2 builder;
        builder = new RevTreeBuilder2(indexDatabase, origTree, defaultMetadataId, executorService);
        return builder;
    }

    public Map<NodeRef, RevTree> buildTrees() {

        final Map<NodeRef, RevTree> result = Maps.newConcurrentMap();

        List<AsyncBuildTree> tasks = Lists.newArrayList();

        for (Entry<String, RevTreeBuilder2> builderEntry : treeBuilders.entrySet()) {
            final String treePath = builderEntry.getKey();
            final RevTreeBuilder2 builder = builderEntry.getValue();
            tasks.add(new AsyncBuildTree(treePath, builder, result));
        }
        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
        return result;
    }

    private class AsyncBuildTree implements Callable<Void> {

        private String treePath;

        private RevTreeBuilder2 builder;

        private Map<NodeRef, RevTree> target;

        AsyncBuildTree(final String treePath, final RevTreeBuilder2 builder,
                final Map<NodeRef, RevTree> target) {

            this.treePath = treePath;
            this.builder = builder;
            this.target = target;
        }

        @Override
        public Void call() {
            RevTree tree = builder.build();

            Node treeNode;
            {
                ObjectId treeMetadataId = builder.getDefaultMetadataId();
                String name = NodeRef.nodeFromPath(treePath);
                ObjectId oid = tree.getId();
                Envelope bounds = SpatialOps.boundsOf(tree);
                treeNode = Node.create(name, oid, treeMetadataId, RevObject.TYPE.TREE, bounds);
            }

            final String parentPath = NodeRef.parentPath(treePath);
            final ObjectId parentMetadataId;
            if (NodeRef.ROOT.equals(parentPath)) {
                parentMetadataId = ObjectId.NULL;
            } else {
                Optional<NodeRef> parentRef = commandLocator.command(FindTreeChild.class)
                        .setChildPath(parentPath).setIndex(true).setParent(workHead)
                        .setParentPath(NodeRef.ROOT).call();

                parentMetadataId = parentRef.isPresent() ? parentRef.get().getMetadataId()
                        : ObjectId.NULL;
            }
            NodeRef newTreeRef = new NodeRef(treeNode, parentPath, parentMetadataId);
            target.put(newTreeRef, tree);
            return null;
        }

    }

}