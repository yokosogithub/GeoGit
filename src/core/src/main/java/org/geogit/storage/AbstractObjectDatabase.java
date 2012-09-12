/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

import org.geogit.api.MutableTree;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.repository.DepthSearch;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closeables;
import com.google.inject.Inject;
import com.ning.compress.lzf.LZFInputStream;
import com.ning.compress.lzf.LZFOutputStream;

public abstract class AbstractObjectDatabase implements ObjectDatabase {

    @Inject
    protected ObjectSerialisingFactory serialFactory;

    public AbstractObjectDatabase() {
        // TODO: use an external cache
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#lookUp(java.lang.String)
     */
    @Override
    public List<ObjectId> lookUp(final String partialId) {
        Preconditions.checkNotNull(partialId);

        byte[] raw = ObjectId.toRaw(partialId);

        return lookUpInternal(raw);
    }

    protected abstract List<ObjectId> lookUpInternal(byte[] raw);

    /**
     * @see org.geogit.storage.ObjectDatabase#get(org.geogit.api.ObjectId,
     *      org.geogit.storage.ObjectReader)
     */
    @Override
    public <T> T get(final ObjectId id, final ObjectReader<T> reader) {
        Preconditions.checkNotNull(id, "id");
        Preconditions.checkNotNull(reader, "reader");

        T object;
        InputStream raw = getRaw(id);
        try {
            object = reader.read(id, raw);
        } finally {
            Closeables.closeQuietly(raw);
        }
        return object;
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#getRaw(org.geogit.api.ObjectId)
     */
    @Override
    public final InputStream getRaw(final ObjectId id) throws IllegalArgumentException {
        InputStream in = getRawInternal(id);
        try {
            return new LZFInputStream(in);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    protected abstract InputStream getRawInternal(ObjectId id) throws IllegalArgumentException;

    /**
     * @see org.geogit.storage.ObjectDatabase#put(org.geogit.storage.ObjectWriter)
     */
    @Override
    public final <T> ObjectId put(final ObjectWriter<T> writer) {
        MessageDigest sha1;
        ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        DigestOutputStream keyGenOut;
        try {
            sha1 = MessageDigest.getInstance("SHA1");

            keyGenOut = new DigestOutputStream(rawOut, sha1);
            // GZIPOutputStream cOut = new GZIPOutputStream(keyGenOut);
            LZFOutputStream cOut = new LZFOutputStream(keyGenOut);

            try {
                writer.write(cOut);
            } finally {
                // cOut.finish();
                cOut.flush();
                cOut.close();
                keyGenOut.flush();
                keyGenOut.close();
                rawOut.flush();
                rawOut.close();
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        final byte[] rawData = rawOut.toByteArray();
        final byte[] rawKey = keyGenOut.getMessageDigest().digest();
        final ObjectId id = new ObjectId(rawKey);
        putInternal(id, rawData, false);
        return id;
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#put(org.geogit.api.ObjectId,
     *      org.geogit.storage.ObjectWriter)
     */
    @Override
    public final boolean put(final ObjectId id, final ObjectWriter<?> writer) {
        ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        // GZIPOutputStream cOut = new GZIPOutputStream(rawOut);
        LZFOutputStream cOut = new LZFOutputStream(rawOut);
        try {
            // writer.write(cOut);
            writer.write(cOut);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            // cOut.finish();
            try {
                cOut.flush();
                cOut.close();
                rawOut.flush();
                rawOut.close();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        final byte[] rawData = rawOut.toByteArray();
        return putInternal(id, rawData, true);
    }

    /**
     * @param id
     * @param rawData
     * @param override if {@code true} an a record with the given id already exists, it shall be
     *        overriden. If {@code false} and a record with the given id already exists, it shall
     *        not be overriden.
     * @return
     */
    protected abstract boolean putInternal(ObjectId id, byte[] rawData, final boolean override);

    /**
     * @see org.geogit.storage.ObjectDatabase#newObjectInserter()
     */
    @Override
    public ObjectInserter newObjectInserter() {
        return new ObjectInserter(this);
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#newTree()
     */
    @Override
    public MutableTree newTree() {
        return new RevSHA1Tree(this).mutable();
    }

    /**
     * If a child tree of {@code parent} addressed by the given {@code childPath} exists, returns
     * it's mutable copy, otherwise just returns a new mutable tree without any modification to
     * root.
     * 
     * @throws IllegalArgumentException if an reference exists for {@code childPath} but is not of
     *         type {@code TREE}
     */
    @Override
    public MutableTree getOrCreateSubTree(final RevTree parent, List<String> childPath) {
        NodeRef treeChildRef = getTreeChild(parent, childPath);
        if (treeChildRef == null) {
            return newTree();
        }
        if (!TYPE.TREE.equals(treeChildRef.getType())) {
            throw new IllegalArgumentException("Object exsits as child of tree " + parent.getId()
                    + " but is not a tree: " + treeChildRef);
        }

        return getTree(treeChildRef.getObjectId()).mutable();
    }

    protected RevTree getTree(final ObjectId treeId) {
        if (treeId.isNull()) {
            return newTree();
        }
        RevTree tree = this.get(treeId, serialFactory.createRevTreeReader(this));

        return tree;
    }

    protected RevTree getTree(final ObjectId treeId, int assignedDepth) {
        if (treeId.isNull()) {
            return newTree();
        }
        RevTree tree = this.get(treeId, serialFactory.createRevTreeReader(this, assignedDepth));
        return tree;
    }

    /**
     * @param root
     * @param tree
     * @param pathToTree
     * @return the id of the saved state of the modified root
     */
    @Override
    public ObjectId writeBack(MutableTree root, final RevTree tree, final List<String> pathToTree) {

        final ObjectId treeId = put(serialFactory.createRevTreeWriter(tree));
        final String treeName = pathToTree.get(pathToTree.size() - 1);

        if (pathToTree.size() == 1) {
            root.put(new NodeRef(treeName, treeId, TYPE.TREE));
            ObjectId newRootId = put(serialFactory.createRevTreeWriter(root));
            return newRootId;
        }
        final List<String> parentPath = pathToTree.subList(0, pathToTree.size() - 1);
        NodeRef parentRef = getTreeChild(root, parentPath);
        MutableTree parent;
        if (parentRef == null) {
            parent = newTree();
        } else {
            ObjectId parentId = parentRef.getObjectId();
            parent = getTree(parentId).mutable();
        }
        parent.put(new NodeRef(treeName, treeId, TYPE.TREE));
        return writeBack(root, parent, parentPath);
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#getTreeChild(org.geogit.api.RevTree,
     *      java.lang.String[])
     */
    @Override
    public NodeRef getTreeChild(RevTree root, String... path) {
        return getTreeChild(root, Arrays.asList(path));
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#getTreeChild(org.geogit.api.RevTree, java.util.List)
     */
    @Override
    public NodeRef getTreeChild(RevTree root, List<String> path) {
        NodeRef treeRef = new DepthSearch(this, serialFactory).find(root,
                ImmutableList.copyOf(path));
        return treeRef;
    }

    public ObjectSerialisingFactory getSerialFactory() {
        return serialFactory;
    }
}
