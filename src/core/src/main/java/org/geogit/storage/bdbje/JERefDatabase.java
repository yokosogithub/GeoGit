/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.bdbje;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import org.geogit.api.MutableTree;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.RefDatabase;
import org.geogit.storage.RevSHA1Tree;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.inject.Inject;
import com.sleepycat.je.Environment;

/**
 * Database of repository {@link Ref references}
 * <p>
 * It uses the {@link ObjectDatabase} to store the references in a {@link RevTree} under the
 * {@code ".geogit/refs"} key.
 * </p>
 * 
 */
public class JERefDatabase implements RefDatabase {

    private static final String REFS_TREE_KEY = ".geogit/refs";

    private static final ObjectId REFS_TREE_ID = ObjectId.forString(REFS_TREE_KEY);

    private ObjectDatabase refsDb;

    private ObjectSerialisingFactory serialFactory;

    private EnvironmentBuilder envProvider;

    @Inject
    public JERefDatabase(EnvironmentBuilder envProvider, ObjectSerialisingFactory serialFactory) {
        this.envProvider = envProvider;
        this.serialFactory = serialFactory;
    }

    /**
     * 
     * @see org.geogit.storage.RefDatabase#create()
     */
    @Override
    public void create() {
        if (refsDb != null) {
            return;
        }
        Environment environment = envProvider.setRelativePath("refs").get();
        refsDb = new JEObjectDatabase(environment);
        refsDb.create();

        final String headRefName = Ref.HEAD;
        condCreate(headRefName, TYPE.COMMIT);
        final String master = Ref.MASTER;
        condCreate(master, TYPE.COMMIT);
    }

    private void condCreate(final String refName, TYPE type) {
        RevTree refsTree = getRefsTree();

        NodeRef child = refsTree.get(refName);
        if (null == child) {
            put(new Ref(refName, ObjectId.NULL, type));
        }
    }

    private RevTree getRefsTree() {
        RevTree refsTree;
        try {
            if (refsDb.exists(REFS_TREE_ID)) {
                refsTree = refsDb.get(REFS_TREE_ID, serialFactory.createRevTreeReader(refsDb));
                // refsTree = db.get(REFS_TREE_ID, new BxmlRevTreeReader(db));
            } else {
                refsTree = new RevSHA1Tree(refsDb);
                refsDb.put(REFS_TREE_ID, serialFactory.createRevTreeWriter(refsTree));
                // db.put(REFS_TREE_ID, new BxmlRevTreeWriter(refsTree));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return refsTree;
    }

    /**
     * 
     * @see org.geogit.storage.RefDatabase#close()
     */
    @Override
    public void close() {
        if (refsDb != null) {
            refsDb.close();
            refsDb = null;
        }
    }

    /**
     * @param name
     * @return
     * @see org.geogit.storage.RefDatabase#getRef(java.lang.String)
     */
    @Override
    public Ref getRef(final String name) {
        Preconditions.checkNotNull(name, "Ref name can't be null");
        RevTree refsTree = getRefsTree();
        NodeRef child = refsTree.get(name);
        return child == null ? null
                : new Ref(child.getName(), child.getObjectId(), child.getType());
    }

    /**
     * @param prefix
     * @return
     * @see org.geogit.storage.RefDatabase#getRefs(java.lang.String)
     */
    @Override
    public List<Ref> getRefs(final String prefix) {
        Preconditions.checkNotNull(prefix, "Ref prefix can't be null");
        List<Ref> refs = new LinkedList<Ref>();
        RevTree refsTree = getRefsTree();

        Iterator<NodeRef> iterator = refsTree.iterator(new Predicate<NodeRef>() {
            public boolean apply(NodeRef input) {
                return input.getName().startsWith(prefix);
            }
        });

        Iterators.addAll(refs, Iterators.transform(iterator, new Function<NodeRef, Ref>() {

            @Override
            public Ref apply(@Nullable NodeRef input) {
                return input == null ? null : new Ref(input.getName(), input.getObjectId(), input
                        .getType());
            }
        }));
        return refs;
    }

    /**
     * @param oid
     * @return
     * @see org.geogit.storage.RefDatabase#getRefsPontingTo(org.geogit.api.ObjectId)
     */
    @Override
    public List<Ref> getRefsPontingTo(final ObjectId oid) {
        Preconditions.checkNotNull(oid);
        List<Ref> refs = new LinkedList<Ref>();
        RevTree refsTree = getRefsTree();
        throw new UnsupportedOperationException(
                "waiting for tree walking implementation to reliable implement this method");
        // return refs;
    }

    /**
     * @param ref
     * @return
     * @see org.geogit.storage.RefDatabase#put(org.geogit.api.Ref)
     */
    @Override
    public boolean put(final Ref ref) {
        Preconditions.checkNotNull(ref);
        Preconditions.checkNotNull(ref.getName());
        Preconditions.checkNotNull(ref.getObjectId());

        RevTree refsTree = getRefsTree();
        NodeRef oldTarget = refsTree.get(ref.getName());
        if (oldTarget != null && oldTarget.getObjectId().equals(ref.getObjectId())) {
            return false;
        }
        refsTree = refsTree.mutable();
        ((MutableTree) refsTree).put(new NodeRef(ref.getName(), ref.getObjectId(), ref.getType()));
        try {
            refsDb.put(REFS_TREE_ID, serialFactory.createRevTreeWriter(refsTree));
            // db.put(REFS_TREE_ID, new BxmlRevTreeWriter(refsTree));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}
