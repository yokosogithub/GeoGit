/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;

import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeBuilder;
import org.geogit.storage.ObjectDatabase;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.geometry.BoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Envelope;

class RevTreeBuilder2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(RevTreeBuilder2.class);

    private NodeIndex nodeIndex;

    private final ObjectDatabase db;

    private final RevTree original;

    private final ExecutorService executorService;

    private final Map<Name, RevFeatureType> revFeatureTypes = Maps.newConcurrentMap();

    private final ObjectId defaultMetadataId;

    /**
     * Copy constructor
     */
    public RevTreeBuilder2(final ObjectDatabase db, @Nullable final RevTree origTree,
            final ObjectId defaultMetadataId, final ExecutorService executorService) {

        this.db = db;
        this.original = origTree;
        this.executorService = executorService;
        this.defaultMetadataId = defaultMetadataId;
    }

    public ObjectId getDefaultMetadataId() {
        return defaultMetadataId;
    }

    /**
     * Adds or replaces an element in the tree with the given key.
     * <p>
     * <!-- Implementation detail: If the number of cached entries (entries held directly by this
     * tree) reaches {@link #DEFAULT_NORMALIZATION_THRESHOLD}, this tree will {@link #normalize()}
     * itself.
     * 
     * -->
     * 
     * @param key non null
     * @param value non null
     */
    public synchronized RevTreeBuilder2 put(final Node node) {
        Preconditions.checkNotNull(node, "node can't be null");
        if (this.nodeIndex == null) {
            this.nodeIndex = new NodeIndex(executorService);
        }
        nodeIndex.add(node);
        return this;
    }

    /**
     * Traverses the nodes in the {@link NodeIndex}, deletes the ones with {@link ObjectId#NULL
     * NULL} ObjectIds, and adds the ones with non "NULL" ids.
     * 
     * @return the new tree, not saved to the object database. Any bucket tree though is saved when
     *         this method returns.
     */
    public RevTree build() {
        if (nodeIndex == null) {
            return original.builder(db).build();
        }

        Stopwatch sw = new Stopwatch().start();
        RevTreeBuilder builder;
        try {
            builder = new RevTreeBuilder(db, original);
            Iterator<Node> nodes = nodeIndex.nodes();
            while (nodes.hasNext()) {
                Node node = nodes.next();
                if (node.getObjectId().isNull()) {
                    builder.remove(node.getName());
                } else {
                    builder.put(node);
                }
            }
        } finally {
            nodeIndex.close();
        }
        LOGGER.debug("Index traversed in {}", sw.stop());
        sw.reset().start();

        RevTree namedTree = builder.build();
        saveExtraFeatureTypes();
        LOGGER.debug("RevTreeBuilder.build() in {}", sw.stop());
        return namedTree;
    }

    private void saveExtraFeatureTypes() {
        Collection<RevFeatureType> types = revFeatureTypes.values();
        List<RevFeatureType> nonDefaults = Lists.newLinkedList();
        for (RevFeatureType t : types) {
            if (!t.getId().equals(defaultMetadataId)) {
                nonDefaults.add(t);
            }
        }
        if (!nonDefaults.isEmpty()) {
            db.putAll(nonDefaults.iterator());
        }
    }

    public Node putFeature(final ObjectId id, final String name,
            @Nullable final BoundingBox bounds, final FeatureType type) {
        Envelope bbox;
        if (bounds == null) {
            bbox = null;
        } else if (bounds instanceof Envelope) {
            bbox = (Envelope) bounds;
        } else {
            bbox = new Envelope(bounds.getMinimum(0), bounds.getMaximum(0), bounds.getMinimum(1),
                    bounds.getMaximum(1));
        }
        RevFeatureType revFeatureType = revFeatureTypes.get(type.getName());
        if (null == revFeatureType) {
            revFeatureType = RevFeatureType.build(type);
            revFeatureTypes.put(type.getName(), revFeatureType);
        }
        ObjectId metadataId = revFeatureType.getId().equals(defaultMetadataId) ? ObjectId.NULL
                : revFeatureType.getId();
        Node node = Node.create(name, id, metadataId, TYPE.FEATURE, bbox);
        put(node);
        return node;
    }

    /**
     * Marks the node named after {@code fid} to be deleted by adding a Node with
     * {@link ObjectId#NULL NULL} ObjectId
     */
    public void removeFeature(String fid) {
        Node node = Node.create(fid, ObjectId.NULL, ObjectId.NULL, TYPE.FEATURE, null);
        put(node);
    }

}
