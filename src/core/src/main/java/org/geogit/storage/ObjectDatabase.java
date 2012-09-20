/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.geogit.api.MutableTree;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;

import com.google.common.base.Optional;

public interface ObjectDatabase {

    public abstract void close();

    /**
     * Initializes/opens the databse. It's safe to call this method multiple times, and only the
     * first call shall take effect.
     */
    public abstract void create();

    public abstract boolean exists(final ObjectId id);

    /**
     * @param id
     * @return
     * @throws IOException
     * @throws IllegalArgumentException if an object with such id does not exist
     */
    public abstract InputStream getRaw(final ObjectId id);

    public List<ObjectId> lookUp(final String partialId);

    /**
     * @param <T>
     * @param id
     * @param reader
     * @return
     * @throws IOException
     * @throws IllegalArgumentException if an object with such id does not exist
     */
    public abstract <T> T get(final ObjectId id, final ObjectReader<T> reader);

    /**
     * 
     */
    public abstract <T> ObjectId put(final ObjectWriter<T> writer);

    /**
     * @param id
     * @param writer
     * @return {@code true} if the object was inserted and it didn't exist previously, {@code false}
     *         if the object was inserted and it replaced an already existing object with the same
     *         key.
     * @throws Exception
     */
    public abstract boolean put(final ObjectId id, final ObjectWriter<?> writer);

    /**
     * @param root
     * @param tree
     * @param pathToTree
     * @return the id of the saved state of the modified root
     */
    public ObjectId writeBack(MutableTree root, final RevTree tree, final String pathToTree);

    public abstract ObjectInserter newObjectInserter();

    /**
     * If a child tree of {@code parent} addressed by the given {@code childPath} exists, returns
     * it's mutable copy, otherwise just returns a new mutable tree without any modification to root
     * or any intermadiate tree between root and the requested tree path.
     * 
     * @throws IllegalArgumentException if an reference exists for {@code childPath} but is not of
     *         type {@code TREE}
     * @todo: this should be taken off the objectdatabase interface and make it a command
     */
    public MutableTree getOrCreateSubTree(final RevTree rootTree, final String treePath);

    /**
     * Creates and return a new, empty tree, that stores to this {@link ObjectDatabase}
     */
    public MutableTree newTree();

    public Optional<NodeRef> getTreeChild(RevTree root, String path);

    // public RevTree getTree(final ObjectId treeId);
    //
    // public RevTree getTree(final ObjectId treeId, int assignedDepth);

    public boolean delete(ObjectId objectId);

    public abstract ObjectSerialisingFactory getSerialFactory();

}