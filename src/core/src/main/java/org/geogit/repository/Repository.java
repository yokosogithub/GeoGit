/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.io.Closeable;
import java.net.URL;
import java.util.Map;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.CommandLocator;
import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.GraphDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.RefDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * A repository is a collection of commits, each of which is an archive of what the project's
 * working tree looked like at a past date, whether on your machine or someone else's.
 * <p>
 * It also defines HEAD (see below), which identifies the branch or commit the current working tree
 * stemmed from. Lastly, it contains a set of branches and tags, to identify certain commits by
 * name.
 * </p>
 * 
 * @see WorkingTree
 */
public class Repository implements CommandLocator {

    private static Logger LOGGER = LoggerFactory.getLogger(Repository.class);

    @Inject
    private StagingArea index;

    @Inject
    private WorkingTree workingTree;

    @Inject
    private Injector injector;

    @Inject
    private ConfigDatabase configDatabase;

    @Inject
    private RefDatabase refDatabase;

    @Inject
    private ObjectDatabase objectDatabase;

    @Inject
    private GraphDatabase graphDatabase;

    private URL repositoryLocation;

    public static final String DEPTH_CONFIG_KEY = "core.depth";

    Repository() {
        //
    }

    public void configure() throws RepositoryConnectionException {
        refDatabase.configure();
        objectDatabase.configure();
        graphDatabase.configure();
        index.getDatabase().configure();
    }

    public void open() throws RepositoryConnectionException {
        refDatabase.checkConfig();
        objectDatabase.checkConfig();
        graphDatabase.checkConfig();
        index.getDatabase().checkConfig();
        refDatabase.create();
        objectDatabase.open();
        graphDatabase.open();
        index.getDatabase().open();
        Optional<URL> repoUrl = command(ResolveGeogitDir.class).call();
        Preconditions.checkState(repoUrl.isPresent(), "Repository URL can't be located");
        this.repositoryLocation = repoUrl.get();
    }

    /**
     * Closes the repository.
     */
    public synchronized void close() {
        close(refDatabase);
        close(objectDatabase);
        close(graphDatabase);
        close(index.getDatabase());
    }

    private void close(Closeable db) {
        try {
            db.close();
        } catch (Exception e) {
            LOGGER.error("Error closing database " + db, e);
        }
    }

    public URL getLocation() {
        return repositoryLocation;
    }

    /**
     * @return the {@link ConfigDatabase} for this repository
     */
    public ConfigDatabase getConfigDatabase() {
        return configDatabase;
    }

    /**
     * @return the {@link RefDatabase} for this repository
     */
    @Override
    public RefDatabase getRefDatabase() {
        return refDatabase;
    }

    /**
     * @return the {@link ObjectDatabase} for this repository
     */
    public ObjectDatabase getObjectDatabase() {
        return objectDatabase;
    }

    /**
     * @return the {@link GraphDatabase} for this repository
     */
    public GraphDatabase getGraphDatabase() {
        return graphDatabase;
    }

    /**
     * @return the {@link StagingArea} for this repository
     */
    @Override
    public StagingArea getIndex() {
        return index;
    }

    /**
     * Finds and returns an instance of a command of the specified class.
     * 
     * @param commandClass the kind of command to locate and instantiate
     * @return a new instance of the requested command class, with its dependencies resolved
     */
    public <T extends AbstractGeoGitOp<?>> T command(Class<T> commandClass) {
        return injector.getInstance(commandClass);
    }

    /**
     * @return the {@link WorkingTree} for this repository
     */
    @Override
    public WorkingTree getWorkingTree() {
        return workingTree;
    }

    /**
     * Test if a blob exists in the object database
     * 
     * @param id the ID of the blob in the object database
     * @return true if the blob exists with the parameter ID, false otherwise
     */
    public boolean blobExists(final ObjectId id) {
        return getObjectDatabase().exists(id);
    }

    /**
     * @param revStr the string to parse
     * @return the parsed {@link Ref}, or {@link Optional#absent()} if it did not parse.
     */
    public Optional<Ref> getRef(final String revStr) {
        Optional<Ref> ref = command(RefParse.class).setName(revStr).call();
        return ref;
    }

    /**
     * @return the {@link Ref} pointed to by HEAD, or {@link Optional#absent()} if it could not be
     *         resolved.
     */
    public Optional<Ref> getHead() {
        return getRef(Ref.HEAD);
    }

    /**
     * Determines if a commit with the given {@link ObjectId} exists in the object database.
     * 
     * @param id the id to look for
     * @return true if the object was found, false otherwise
     */
    public boolean commitExists(final ObjectId id) {
        try {
            RevObject revObject = getObjectDatabase().get(id);
            return revObject instanceof RevCommit;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Gets the {@link RevCommit} with the given {@link ObjectId} from the object database.
     * 
     * @param commitId the {@code ObjectId} for the commit
     * @return the {@code RevCommit}
     */
    public RevCommit getCommit(final ObjectId commitId) {
        RevCommit commit = getObjectDatabase().getCommit(commitId);

        return commit;
    }

    /**
     * Test if a tree exists in the object database
     * 
     * @param id the ID of the tree in the object database
     * @return true if the tree exists with the parameter ID, false otherwise
     */
    public boolean treeExists(final ObjectId id) {
        try {
            getObjectDatabase().getTree(id);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    /**
     * @return the {@link ObjectId} of the root tree
     */
    public ObjectId getRootTreeId() {
        // find the root tree
        ObjectId commitId = command(RevParse.class).setRefSpec(Ref.HEAD).call().get();
        if (commitId.isNull()) {
            return commitId;
        }
        RevCommit commit = command(RevObjectParse.class).setRefSpec(commitId.toString())
                .call(RevCommit.class).get();
        ObjectId treeId = commit.getTreeId();
        return treeId;
    }

    /**
     * @return an {@link ObjectInserter} to insert objects into the object database
     */
    public ObjectInserter newObjectInserter() {
        return getObjectDatabase().newObjectInserter();
    }

    /**
     * @param contentId the {@link ObjectId} of the feature to get
     * @return the {@link RevFeature} that was found in the object database
     */
    public RevFeature getFeature(final ObjectId contentId) {

        RevFeature revFeature = getObjectDatabase().getFeature(contentId);

        return revFeature;
    }

    /**
     * @return the existing {@link RevTree} pointed to by HEAD, or a new {@code RevTree} if it did
     *         not exist
     */
    public RevTree getOrCreateHeadTree() {
        Optional<ObjectId> headTreeId = command(ResolveTreeish.class).setTreeish(Ref.HEAD).call();
        if (!headTreeId.isPresent() || headTreeId.get().isNull()) {
            return RevTree.EMPTY;
        }
        return getTree(headTreeId.get());
    }

    /**
     * @param treeId the tree to retrieve
     * @return the {@link RevTree} referred to by the given {@link ObjectId}
     */
    public RevTree getTree(ObjectId treeId) {
        return command(RevObjectParse.class).setObjectId(treeId).call(RevTree.class).get();
    }

    /**
     * @param path the path to search for
     * @return an {@link Optional} of the {@link Node} for the child, or {@link Optional#absent()}
     *         if it wasn't found
     */
    public Optional<Node> getRootTreeChild(String path) {
        Optional<NodeRef> nodeRef = command(FindTreeChild.class).setChildPath(path).call();
        if (nodeRef.isPresent()) {
            return Optional.of(nodeRef.get().getNode());
        } else {
            return Optional.absent();
        }
    }

    /**
     * Search the given tree for the child path.
     * 
     * @param tree the tree to search
     * @param childPath the path to search for
     * @return an {@link Optional} of the {@link Node} for the child path, or
     *         {@link Optional#absent()} if it wasn't found
     */
    public Optional<Node> getTreeChild(RevTree tree, String childPath) {
        Optional<NodeRef> nodeRef = command(FindTreeChild.class).setParent(tree)
                .setChildPath(childPath).call();
        if (nodeRef.isPresent()) {
            return Optional.of(nodeRef.get().getNode());
        } else {
            return Optional.absent();
        }
    }

    /**
     * Gets the depth of the repository, or {@link Optional#absent} if this is not a shallow clone.
     * 
     * @return the depth
     */
    public Optional<Integer> getDepth() {
        int repoDepth = 0;
        Optional<Map<String, String>> depthResult = command(ConfigOp.class)
                .setAction(ConfigAction.CONFIG_GET).setName(DEPTH_CONFIG_KEY).call();
        if (depthResult.isPresent()) {
            String depthString = depthResult.get().get(DEPTH_CONFIG_KEY);
            if (depthString != null) {
                repoDepth = Integer.parseInt(depthString);
            }
        }

        if (repoDepth == 0) {
            return Optional.absent();
        }
        return Optional.of(repoDepth);
    }

    /**
     * @return true if this is a sparse (mapped) clone.
     */
    public boolean isSparse() {
        Optional<Map<String, String>> sparseResult = command(ConfigOp.class)
                .setAction(ConfigAction.CONFIG_GET).setName("sparse.filter").call();
        return sparseResult.isPresent();
    }

    @Override
    public Platform getPlatform() {
        return injector.getInstance(Platform.class);
    }
}
