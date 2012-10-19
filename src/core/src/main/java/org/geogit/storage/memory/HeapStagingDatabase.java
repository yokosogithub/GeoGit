/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.memory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.annotation.Nullable;

import org.geogit.api.CommandLocator;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.ning.compress.lzf.LZFInputStream;

public class HeapStagingDatabase extends HeapObjectDatabse implements StagingDatabase {

    private ObjectDatabase repositoryDb;

    private CommandLocator commandLocator;

    /**
     * @param referenceDatabase the repository reference database, used to get the head re
     * @param repoDb
     * @param stagingDb
     */
    @Inject
    public HeapStagingDatabase(final ObjectDatabase repositoryDb,
            final CommandLocator commandLocator) {
        this.repositoryDb = repositoryDb;
        this.commandLocator = commandLocator;
    }

    // /////////////////////////////////////////
    /**
     * 
     * @see org.geogit.storage.StagingDatabase#open()
     */
    @Override
    public void open() {
        super.open();
        // {
        // Optional<Ref> stageHead = commandLocator.command(RefParse.class)
        // .setName(Ref.STAGE_HEAD).call();
        // if (!stageHead.isPresent()) {
        // // resolve to current head
        // ObjectId headTreeId = commandLocator.command(ResolveTreeish.class)
        // .setTreeish(Ref.HEAD).call();
        // commandLocator.command(UpdateRef.class).setName(Ref.STAGE_HEAD)
        // .setNewValue(headTreeId).call();
        // }
        // }
        // {
        // Optional<Ref> workTreeHead = commandLocator.command(RefParse.class)
        // .setName(Ref.WORK_HEAD).call();
        // if (!workTreeHead.isPresent()) {
        // // resolve to current head
        // ObjectId stageTreeId = commandLocator.command(ResolveTreeish.class)
        // .setTreeish(Ref.STAGE_HEAD).call();
        // commandLocator.command(UpdateRef.class).setName(Ref.WORK_HEAD)
        // .setNewValue(stageTreeId).call();
        // }
        // }
    }

    /**
     * @see org.geogit.storage.StagingDatabase#close()
     */
    @Override
    public void close() {
        super.close();
    }

    // //////////////////////////////////////////////////////////////////////////////////////////

    private synchronized List<NodeRef> filter(SortedMap<String, NodeRef> map, String pathFilter) {
        List<NodeRef> matches = Lists.newLinkedList();

        for (Map.Entry<String, NodeRef> e : map.entrySet()) {
            String path = e.getKey();
            if (path.startsWith(pathFilter)) {
                matches.add(e.getValue());
            }
        }
        return matches;
    }

    /**
     * @return number of entries removed
     */
    private int remove(SortedMap<String, NodeRef> map, @Nullable String pathFilter) {
        int size = 0;
        if (pathFilter == null || pathFilter.isEmpty()) {
            size = map.size();
            map.clear();
        } else {
            for (String path : map.tailMap(pathFilter).keySet()) {
                if (path.startsWith(pathFilter)) {
                    size++;
                    map.remove(path);
                } else {
                    break;
                }
            }
        }
        return size;
    }

    // /////////////////////////////////////////////////////////////////////

    @Override
    public boolean exists(ObjectId id) {
        boolean exists = super.exists(id);
        if (!exists) {
            exists = repositoryDb.exists(id);
        }
        return exists;
    }

    @Override
    public final InputStream getRaw(final ObjectId id) throws IllegalArgumentException {
        InputStream in = getRawInternal(id);
        if (in != null) {
            try {
                return new LZFInputStream(in);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
        return repositoryDb.getRaw(id);
    }

    @Override
    protected InputStream getRawInternal(ObjectId id) throws IllegalArgumentException {
        if (super.exists(id)) {
            return super.getRawInternal(id);
        }
        return null;
    }

    @Override
    public List<ObjectId> lookUp(String partialId) {
        Set<ObjectId> lookUp = new HashSet<ObjectId>(super.lookUp(partialId));
        lookUp.addAll(repositoryDb.lookUp(partialId));
        return new ArrayList<ObjectId>(lookUp);
    }

    @Override
    public <T> T get(ObjectId id, ObjectReader<T> reader) {
        if (super.exists(id)) {
            return super.get(id, reader);
        }
        return repositoryDb.get(id, reader);
    }

    @Override
    public ObjectInserter newObjectInserter() {
        return super.newObjectInserter();
    }

    @Override
    public boolean delete(ObjectId objectId) {
        return super.delete(objectId);
    }

}
