/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.memory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.annotation.Nullable;

import org.geogit.api.DiffEntry;
import org.geogit.api.MutableTree;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.StagingDatabase;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class HeapStagingDatabase implements ObjectDatabase, StagingDatabase {

    private SortedMap<List<String>, DiffEntry> staged;

    private SortedMap<List<String>, DiffEntry> unstaged;

    // /////////////////////////////////////////
    /**
     * The staging area object database, contains only differences between the index and the
     * repository
     */
    private ObjectDatabase stagingDb;

    private ObjectDatabase repositoryDb;

    /**
     * @param referenceDatabase the repository reference database, used to get the head re
     * @param repoDb
     * @param stagingDb
     */
    @Inject
    public HeapStagingDatabase(final ObjectDatabase repositoryDb) {
        this.repositoryDb = repositoryDb;
    }

    /**
     * 
     * @see org.geogit.storage.StagingDatabase#create()
     */
    @Override
    public void create() {
        if (stagingDb == null) {
            stagingDb = new HeapObjectDatabse();
            stagingDb.create();

            Comparator<List<String>> pathComparator = new Comparator<List<String>>() {
                @Override
                public int compare(List<String> o1, List<String> o2) {
                    return o1.toString().compareTo(o2.toString());
                }
            };
            unstaged = Maps.newTreeMap(pathComparator);
            staged = Maps.newTreeMap(pathComparator);
        }
    }

    /**
     * @see org.geogit.storage.StagingDatabase#close()
     */
    @Override
    public void close() {
        if (stagingDb != null) {
            staged.clear();
            unstaged.clear();
            stagingDb.close();
            staged = null;
            unstaged = null;
            stagingDb = null;
        }
    }

    /**
     * @see org.geogit.storage.StagingDatabase#reset()
     */
    @Override
    public synchronized void reset() {
        unstaged.clear();
        staged.clear();
    }

    /**
     * @see org.geogit.storage.StagingDatabase#clearUnstaged()
     */
    @Override
    public synchronized void clearUnstaged() {
        this.unstaged.clear();
    }

    /**
     * @see org.geogit.storage.StagingDatabase#clearStaged()
     */
    @Override
    public synchronized void clearStaged() {
        this.staged.clear();
    }

    // //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @param diffEntry
     * @see org.geogit.storage.StagingDatabase#putUnstaged(org.geogit.api.DiffEntry)
     */
    @Override
    public synchronized void putUnstaged(final DiffEntry diffEntry) {
        unstaged.put(diffEntry.getPath(), diffEntry);
    }

    /**
     * @param diffEntry
     * @see org.geogit.storage.StagingDatabase#stage(org.geogit.api.DiffEntry)
     */
    @Override
    public synchronized void stage(DiffEntry diffEntry) {
        List<String> path = diffEntry.getPath();
        DiffEntry remove = unstaged.remove(path);
        if (remove != null) {
            staged.put(path, diffEntry);
        }
    }

    /**
     * @param pathFilter
     * @return
     * @see org.geogit.storage.StagingDatabase#countUnstaged(java.util.List)
     */
    @Override
    public synchronized int countUnstaged(final List<String> pathFilter) {
        if (pathFilter == null || pathFilter.size() == 0) {
            return unstaged.size();
        }
        List<DiffEntry> matches = filter(unstaged, pathFilter);
        return matches.size();
    }

    /**
     * @param pathFilter
     * @return
     * @see org.geogit.storage.StagingDatabase#countStaged(java.util.List)
     */
    @Override
    public synchronized int countStaged(final List<String> pathFilter) {
        if (pathFilter == null || pathFilter.size() == 0) {
            return staged.size();
        }
        List<DiffEntry> matches = filter(staged, pathFilter);
        return matches.size();
    }

    /**
     * @see org.geogit.storage.StagingDatabase#getUnstaged(java.util.List)
     */
    @Override
    public synchronized Iterator<DiffEntry> getUnstaged(@Nullable final List<String> pathFilter) {
        if (pathFilter == null || pathFilter.size() == 0) {
            return Lists.newArrayList(unstaged.values()).iterator();
        }
        List<DiffEntry> matches = filter(unstaged, pathFilter);
        return matches.iterator();
    }

    /**
     * @see org.geogit.storage.StagingDatabase#getStaged(java.util.List)
     */
    @Override
    public synchronized Iterator<DiffEntry> getStaged(@Nullable final List<String> pathFilter) {
        if (pathFilter == null || pathFilter.size() == 0) {
            return Lists.newArrayList(staged.values()).iterator();
        }
        List<DiffEntry> matches = filter(staged, pathFilter);
        return matches.iterator();
    }

    private synchronized List<DiffEntry> filter(SortedMap<List<String>, DiffEntry> map,
            List<String> pathFilter) {
        List<DiffEntry> matches = Lists.newLinkedList();

        for (Map.Entry<List<String>, DiffEntry> e : map.entrySet()) {
            List<String> path = e.getKey();
            if (path.size() >= pathFilter.size()
                    && pathFilter.equals(path.subList(0, pathFilter.size()))) {
                matches.add(e.getValue());
            }
        }
        return matches;
    }

    /**
     * @see org.geogit.storage.StagingDatabase#removeStaged(java.util.List)
     */
    @Override
    public synchronized int removeStaged(final List<String> pathFilter) {
        return remove(staged, pathFilter);
    }

    /**
     * @see org.geogit.storage.StagingDatabase#removeUnStaged(java.util.List)
     */
    @Override
    public synchronized int removeUnStaged(final List<String> pathFilter) {
        return remove(unstaged, pathFilter);
    }

    /**
     * @return
     */
    private int remove(SortedMap<List<String>, DiffEntry> map, List<String> pathFilter) {
        int size = 0;
        if (pathFilter == null || pathFilter.isEmpty()) {
            size = map.size();
            map.clear();
        } else {
            for (List<String> key : Lists.newArrayList(map.keySet())) {
                if (key.size() >= pathFilter.size()
                        && pathFilter.equals(key.subList(0, pathFilter.size()))) {
                    size++;
                    map.remove(key);
                }
            }
        }
        return size;
    }

    /**
     * @see org.geogit.storage.StagingDatabase#findStaged(java.lang.String)
     */
    @Override
    public synchronized DiffEntry findStaged(final String... path) {
        return findStaged(Arrays.asList(path));
    }

    /**
     * @see org.geogit.storage.StagingDatabase#findStaged(java.util.List)
     */
    @Override
    public synchronized DiffEntry findStaged(final List<String> path) {
        DiffEntry diffEntry = staged.get(path);
        return diffEntry;
    }

    /**
     * @see org.geogit.storage.StagingDatabase#findUnstaged(java.lang.String)
     */
    @Override
    public synchronized DiffEntry findUnstaged(final String... path) {
        return findUnstaged(Arrays.asList(path));

    }

    /**
     * @see org.geogit.storage.StagingDatabase#findUnstaged(java.util.List)
     */
    @Override
    public synchronized DiffEntry findUnstaged(final List<String> path) {
        DiffEntry diffEntry = unstaged.get(path);
        return diffEntry;
    }

    /**
     * @return
     * @see org.geogit.storage.StagingDatabase#getObjectDatabase()
     */
    @Override
    public ObjectDatabase getObjectDatabase() {
        return this.stagingDb;
    }

    // /////////////////////////////////////////////////////////////////////

    @Override
    public boolean exists(ObjectId id) {
        boolean exists = stagingDb.exists(id);
        if (!exists) {
            exists = repositoryDb.exists(id);
        }
        return exists;
    }

    @Override
    public InputStream getRaw(ObjectId id) {
        if (stagingDb.exists(id)) {
            return stagingDb.getRaw(id);
        }
        return repositoryDb.getRaw(id);
    }

    @Override
    public List<ObjectId> lookUp(String partialId) {
        Set<ObjectId> lookUp = new HashSet<ObjectId>(stagingDb.lookUp(partialId));
        lookUp.addAll(repositoryDb.lookUp(partialId));
        return new ArrayList<ObjectId>(lookUp);
    }

    @Override
    public <T> T get(ObjectId id, ObjectReader<T> reader) {
        if (stagingDb.exists(id)) {
            return stagingDb.get(id, reader);
        }
        return repositoryDb.get(id, reader);
    }

    @Override
    public <T> ObjectId put(ObjectWriter<T> writer) {
        return stagingDb.put(writer);
    }

    @Override
    public boolean put(ObjectId id, ObjectWriter<?> writer) {
        return stagingDb.put(id, writer);
    }

    @Override
    public ObjectId writeBack(MutableTree root, RevTree tree, List<String> pathToTree) {
        return stagingDb.writeBack(root, tree, pathToTree);
    }

    @Override
    public ObjectInserter newObjectInserter() {
        return stagingDb.newObjectInserter();
    }

    @Override
    public MutableTree getOrCreateSubTree(RevTree parent, List<String> childPath) {
        NodeRef treeChild = repositoryDb.getTreeChild(parent, childPath);
        if (null != treeChild) {
            return repositoryDb.getOrCreateSubTree(parent, childPath);
        }
        return stagingDb.getOrCreateSubTree(parent, childPath);
    }

    @Override
    public MutableTree newTree() {
        return stagingDb.newTree();
    }

    @Override
    public NodeRef getTreeChild(RevTree root, String... path) {
        return getTreeChild(root, Arrays.asList(path));
    }

    @Override
    public NodeRef getTreeChild(RevTree root, List<String> path) {
        NodeRef treeChild = stagingDb.getTreeChild(root, path);
        if (null != treeChild) {
            return treeChild;
        }
        return repositoryDb.getTreeChild(root, path);
    }

    @Override
    public boolean delete(ObjectId objectId) {
        return stagingDb.delete(objectId);
    }

    @Override
    public ObjectSerialisingFactory getSerialFactory() {
        return repositoryDb.getSerialFactory();
    }
}
