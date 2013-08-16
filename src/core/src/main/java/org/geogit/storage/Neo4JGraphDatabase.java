/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.inject.Inject;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe.LoopBundle;

/**
 * Provides an implementation of a GeoGit Graph Database using Neo4J.
 */
public class Neo4JGraphDatabase extends AbstractGraphDatabase {

    protected Neo4jGraph graphDB = null;

    protected String dbPath;

    protected static Map<String, ServiceContainer> databaseServices = new ConcurrentHashMap<String, ServiceContainer>();

    private final Platform platform;

    /**
     * Container class for the database service to keep track of reference counts.
     */
    protected class ServiceContainer {
        private Neo4jGraph dbService;

        private int refCount;

        public ServiceContainer(Neo4jGraph dbService) {
            this.dbService = dbService;
            this.refCount = 0;
        }

        public void removeRef() {
            this.refCount--;
        }

        public void addRef() {
            this.refCount++;
        }

        public int getRefCount() {
            return this.refCount;
        }

        public Neo4jGraph getService() {
            return this.dbService;
        }
    }

    private enum CommitRelationshipTypes implements RelationshipType {
        TOROOT, PARENT, MAPPED_TO
    }

    static {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running example before it's completed)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                for (Entry<String, ServiceContainer> entry : databaseServices.entrySet()) {
                    File graphPath = new File(entry.getKey());
                    if (graphPath.exists()) {
                        entry.getValue().getService().shutdown();
                    }
                }
                databaseServices.clear();
            }
        });
    }

    /**
     * Constructs a new {@code Neo4JGraphDatabase} using the given platform.
     * 
     * @param platform the platform to use.
     */
    @Inject
    public Neo4JGraphDatabase(final Platform platform) {
        this.platform = platform;
    }

    /**
     * Opens the Neo4J graph database.
     */
    @Override
    public void open() {
        if (isOpen()) {
            return;
        }
        URL envHome = new ResolveGeogitDir(platform).call();
        if (envHome == null) {
            throw new IllegalStateException("Not inside a geogit directory");
        }
        if (!"file".equals(envHome.getProtocol())) {
            throw new UnsupportedOperationException(
                    "This Graph Database works only against file system repositories. "
                            + "Repository location: " + envHome.toExternalForm());
        }
        File repoDir;
        try {
            repoDir = new File(envHome.toURI());
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
        File graph = new File(repoDir, "graph");
        if (!graph.exists() && !graph.mkdir()) {
            throw new IllegalStateException("Cannot create graph directory '"
                    + graph.getAbsolutePath() + "'");
        }

        dbPath = graph.getAbsolutePath() + "/graphDB.db";

        if (databaseServices.containsKey(dbPath)) {
            ServiceContainer serviceContainer = databaseServices.get(dbPath);
            serviceContainer.addRef();
            graphDB = serviceContainer.getService();
        } else {
            graphDB = getGraphDatabase();
            ServiceContainer newContainer = new ServiceContainer(graphDB);
            newContainer.addRef();
            databaseServices.put(dbPath, newContainer);
        }

    }

    /**
     * Constructs the graph database service.
     * 
     * @return the new {@link GraphDatabaseService}
     */
    protected Neo4jGraph getGraphDatabase() {
    	return new Neo4jGraph(dbPath);
//        return new GraphDatabaseFactory().newEmbeddedDatabase(dbPath);
    }

    /**
     * Destroy the graph database service. This will only happen when the ref count for the database
     * service is 0.
     */
    protected void destroyGraphDatabase() {
        File graphPath = new File(dbPath);
        if (graphPath.exists()) {
            graphDB.shutdown();
        }
        databaseServices.remove(dbPath);
    }

    /**
     * @return true if the database is open, false otherwise
     */
    @Override
    public boolean isOpen() {
        return graphDB != null;
    }

    /**
     * Closes the database.
     */
    @Override
    public void close() {
        if (isOpen()) {
            ServiceContainer container = databaseServices.get(dbPath);
            container.removeRef();
            if (container.getRefCount() <= 0) {
                destroyGraphDatabase();
            }
            graphDB = null;
        }

    }

    /**
     * Determines if the given commit exists in the graph database.
     * 
     * @param commitId the commit id to search for
     * @return true if the commit exists, false otherwise
     */
    @Override
    public boolean exists(ObjectId commitId) {
		try {
			com.tinkerpop.blueprints.Index<Vertex> idIndex = graphDB.getIndex(
					"identifiers", Vertex.class);
			CloseableIterable<Vertex> results = null;
			try {
				results = idIndex.get("id", commitId.toString());
				if (results.iterator().hasNext()) {
					results.iterator().next();
					if (results.iterator().hasNext()) {
						throw new NoSuchElementException(); // strictly
															// following Neo4J's
															// getSingle
															// semantics; is
															// this necessary?
					} else {
						return true;
					}
				} else {
					return false;
				}
			} finally {
				if (results != null)
					results.close();
			}
		} finally {
			graphDB.rollback();
		}
    }

    /**
     * Retrieves all of the parents for the given commit.
     * 
     * @param commitid the commit whose parents should be returned
     * @return a list of the parents of the provided commit
     * @throws IllegalArgumentException
     */
    @Override
    public ImmutableList<ObjectId> getParents(ObjectId commitId) throws IllegalArgumentException {
		try {
			com.tinkerpop.blueprints.Index<Vertex> idIndex = graphDB.getIndex(
					"identifiers", Vertex.class);
			Vertex node = null;
			CloseableIterable<Vertex> results = null;
			try {
				results = idIndex.get("id", commitId.toString());
				if (results.iterator().hasNext()) {
					node = results.iterator().next();
				}
			} finally {
				results.close();
			}

			Builder<ObjectId> listBuilder = new ImmutableList.Builder<ObjectId>();

			if (node != null) {
				for (Edge edge : node.getEdges(
						com.tinkerpop.blueprints.Direction.OUT,
						CommitRelationshipTypes.PARENT.name())) {
					Vertex parentNode = edge
							.getVertex(com.tinkerpop.blueprints.Direction.IN);
					listBuilder.add(ObjectId.valueOf(parentNode
							.<String> getProperty("id")));
				}
			}
			return listBuilder.build();
		} finally {
			graphDB.rollback();
		}
    }

    /**
     * Retrieves all of the children for the given commit.
     * 
     * @param commitid the commit whose children should be returned
     * @return a list of the children of the provided commit
     * @throws IllegalArgumentException
     */
    @Override
    public ImmutableList<ObjectId> getChildren(ObjectId commitId) throws IllegalArgumentException {
		try {
			com.tinkerpop.blueprints.Index<Vertex> idIndex = graphDB.getIndex(
					"identifiers", Vertex.class);
			CloseableIterable<Vertex> results = null;
			Vertex node = null;
			try {
				results = idIndex.get("id", commitId.toString());
				if (results.iterator().hasNext()) {
					node = results.iterator().next();
				}
			} finally {
				if (results != null)
					results.close();
			}

			Builder<ObjectId> listBuilder = new ImmutableList.Builder<ObjectId>();

			if (node != null) {
				for (Edge child : node.getEdges(
						com.tinkerpop.blueprints.Direction.IN,
						CommitRelationshipTypes.PARENT.name())) {
					Vertex childNode = child
							.getVertex(com.tinkerpop.blueprints.Direction.OUT);
					listBuilder.add(ObjectId.valueOf(childNode
							.<String> getProperty("id")));
				}
			}
			return listBuilder.build();
		} finally {
			graphDB.rollback();
		}
    }

    /**
     * Adds a commit to the database with the given parents. If a commit with the same id already
     * exists, it will not be inserted.
     * 
     * @param commitId the commit id to insert
     * @param parentIds the commit ids of the commit's parents
     * @return true if the commit id was inserted, false otherwise
     */
    @Override
    public boolean put(ObjectId commitId, ImmutableList<ObjectId> parentIds) {
        try {
            // See if it already exists
            Vertex commitNode = getOrAddNode(commitId);

            if (parentIds.isEmpty()) {
                if (!commitNode
                		.getEdges(com.tinkerpop.blueprints.Direction.OUT, CommitRelationshipTypes.TOROOT.name())
                        .iterator().hasNext()) {
                    // Attach this node to the root node
                	graphDB.getVertex(0);
                    commitNode.addEdge(CommitRelationshipTypes.TOROOT.name(), graphDB.getVertex(0));
                }
            }

            if (!commitNode.getEdges(com.tinkerpop.blueprints.Direction.OUT, CommitRelationshipTypes.PARENT.name())
                    .iterator().hasNext()) {
                // Don't make relationships if they have been created already
                for (ObjectId parent : parentIds) {
                    Vertex parentNode = getOrAddNode(parent);
                    commitNode.addEdge(CommitRelationshipTypes.PARENT.name(), parentNode);
                }
            }
            graphDB.commit();
        } catch (Exception e) {
        	graphDB.rollback();
            throw Throwables.propagate(e);
        }
        return true;
    }

    /**
     * Maps a commit to another original commit. This is used in sparse repositories.
     * 
     * @param mapped the id of the mapped commit
     * @param original the commit to map to
     */
    @Override
    public void map(final ObjectId mapped, final ObjectId original) {
        Vertex commitNode = null;
        try {
            // See if it already exists
            commitNode = getOrAddNode(mapped);

            if (commitNode.getEdges(com.tinkerpop.blueprints.Direction.OUT, CommitRelationshipTypes.MAPPED_TO.name())
                    .iterator().hasNext()) {
                // Remove old mapping
            	Edge toRemove = 
                commitNode.getEdges(com.tinkerpop.blueprints.Direction.OUT, CommitRelationshipTypes.MAPPED_TO.name())
                        .iterator().next();
            	graphDB.removeEdge(toRemove);
            }

            // Don't make relationships if they have been created already
            Vertex originalNode = getOrAddNode(original);
            commitNode.addEdge(CommitRelationshipTypes.MAPPED_TO.name(), originalNode);
            graphDB.commit();
        } catch (Exception e) {
        	graphDB.rollback();
            throw Throwables.propagate(e);
        }
    }

    /**
     * Gets the id of the commit that this commit is mapped to.
     * 
     * @param commitId the commit to find the mapping of
     * @return the mapped commit id
     */
    public ObjectId getMapping(final ObjectId commitId) {
    	com.tinkerpop.blueprints.Index<Vertex> idIndex = graphDB.getIndex("identifiers", Vertex.class);
    	Vertex node = null;
    	CloseableIterable<Vertex> results = null;
    	try {
    		results = idIndex.get("id", commitId.toString());
    		node = results.iterator().next();
    	} finally {
    		if (results != null) results.close();
    	}
    	
    	ObjectId mapped = ObjectId.NULL;
    	Vertex mappedNode = getMappedNode(node);
    	if (mappedNode != null) {
    		mapped = ObjectId.valueOf(mappedNode.<String>getProperty("id"));
    	}
    	return mapped;
    }

    private Vertex getMappedNode(final Vertex commitNode) {
        if (commitNode != null) {
        	Iterable<Edge> mappings = commitNode.getEdges(com.tinkerpop.blueprints.Direction.OUT, CommitRelationshipTypes.MAPPED_TO.name());
        	if (mappings.iterator().hasNext()) {
        		return mappings.iterator().next().getVertex(com.tinkerpop.blueprints.Direction.IN);
        	}
        }
        return null;
    }

    /**
     * Gets a node or adds it if it doesn't exist. Note, this must be called within a
     * {@link Transaction}.
     * 
     * @param commitId
     * @return
     */
    private Vertex getOrAddNode(ObjectId commitId) {
        final String commitIdStr = commitId.toString();
        com.tinkerpop.blueprints.Index<Vertex> index = graphDB.getIndex("identifiers", Vertex.class);
        Vertex v;
        if (index.count("id", commitId.toString()) == 0) {
        	v = graphDB.addVertex(null);
        	v.setProperty("id", commitIdStr);
        } else {
        	CloseableIterable<Vertex> results = null;
        	try {
        		results = index.get("id", commitIdStr);
        		v = results.iterator().next();
        	} finally {
        		if (results != null) results.close();
        	}
        }
        
        return v;
    }

    /**
     * Gets the number of ancestors of the commit until it reaches one with no parents, for example
     * the root or an orphaned commit.
     * 
     * @param commitId the commit id to start from
     * @return the depth of the commit
     */
    @Override
    public int getDepth(final ObjectId commitId) {
    	com.tinkerpop.blueprints.Index<Vertex> idIndex = graphDB.getIndex("identifiers", Vertex.class);
    	Vertex commitNode = null;
    	CloseableIterable<Vertex> results = null;
    	try {
    		results = idIndex.get("id", commitId.toString());
    		commitNode = results.iterator().next();
    	} finally {
    		if (results != null) results.close();
    	}
    	PipeFunction<LoopBundle<Vertex>, Boolean> expandCriterion =
    		new PipeFunction<LoopBundle<Vertex>, Boolean>() {
	    		@Override
	    		public Boolean compute(LoopBundle<Vertex> argument) {
	    			Iterable<Edge> edges = argument.getObject().getEdges(com.tinkerpop.blueprints.Direction.OUT, CommitRelationshipTypes.TOROOT.name());
	    			return edges.iterator().hasNext();
	    		}
	    	};
	    PipeFunction<LoopBundle<Vertex>, Boolean> emitCriterion =
	        new PipeFunction<LoopBundle<Vertex>, Boolean>() {
				@Override
				public Boolean compute(LoopBundle<Vertex> argument) {
					Iterable<Edge> edges = argument.getObject().getEdges(com.tinkerpop.blueprints.Direction.OUT, CommitRelationshipTypes.TOROOT.name());
					return !edges.iterator().hasNext();
				}
	        };
    	GremlinPipeline<Vertex, List> pipe = 
    		new GremlinPipeline<Vertex, Vertex>()
    		    .start(commitNode)
	    	    .as("start")
	    	    .out(CommitRelationshipTypes.PARENT.name())
	    	    .loop("start", expandCriterion, emitCriterion)
	    	    .path();

        int min = Integer.MAX_VALUE;
        for (List<?> path : pipe) {
            int length = path.size();
            if (length < min) {
                min = length;
            }
        }

        return min;
    }

    /**
     * Determines if there are any sparse commits between the start commit and the end commit, not
     * including the end commit.
     * 
     * @param start the start commit
     * @param end the end commit
     * @return true if there are any sparse commits between start and end
     */
    public boolean isSparsePath(final ObjectId start, final ObjectId end) {
    	com.tinkerpop.blueprints.Index<Vertex> idIndex = graphDB.getIndex("identifiers", Vertex.class);
//        Index<Node> idIndex = graphDB.index().forNodes("identifiers");
    	Vertex startNode = null;
    	Vertex endNode = null;
    	CloseableIterable<Vertex> startResults = null;
    	CloseableIterable<Vertex> endResults = null;
    	try {
    		startResults = idIndex.get("id", start.toString());
    		startNode = startResults.iterator().next();
    		endResults = idIndex.get("id", end.toString());
    		endNode = endResults.iterator().next();
    	} finally {
    		if (startResults != null) startResults.close();
    		if (endResults != null) endResults.close();
    	}
    	
    	
		PipeFunction<LoopBundle<Vertex>, Boolean> whileFunction = new PipeFunction<LoopBundle<Vertex>, Boolean>() {
			@Override
			public Boolean compute(LoopBundle<Vertex> argument) {
				return !argument.getObject().getProperty("id").equals(end.toString());
			}
		};
		
		PipeFunction<LoopBundle<Vertex>, Boolean> emitFunction = new PipeFunction<LoopBundle<Vertex>, Boolean>() {
			@Override
			public Boolean compute(LoopBundle<Vertex> argument) {
				return argument.getObject().getProperty("id").equals(end.toString());
			}
		};
		
		GremlinPipeline<Vertex, List> pipe = new GremlinPipeline<Vertex, Vertex>()
		    .start(startNode)
    	    .as("start")
    	    .out(CommitRelationshipTypes.PARENT.name())
    	    .loop("start", whileFunction, emitFunction)
    	    .path();
		    
		for (List path : pipe) {
			for (Vertex vertex : (List<Vertex>)path) {
				if (!vertex.equals(endNode) && vertex.getPropertyKeys().contains(SPARSE_FLAG)) {
					return true;
				}
			}
		}
		
        return false;
    }

    /**
     * Set a property on the provided commit node.
     * 
     * @param commitId the id of the commit
     */
    public void setProperty(ObjectId commitId, String propertyName, String propertyValue) {
        com.tinkerpop.blueprints.Index<Vertex> idIndex = graphDB.getIndex("identifiers", Vertex.class);
        CloseableIterable<Vertex> results = null;
        try {
        	results = idIndex.get("id", commitId.toString());
            Vertex commitNode = results.iterator().next();
            commitNode.setProperty(propertyName, propertyValue);
            graphDB.commit();
        } catch (Exception e) {
        	graphDB.rollback();
        } finally {
        	if (results != null) results.close();
        }
    }

    /**
     * Finds the lowest common ancestor of two commits.
     * 
     * @param leftId the commit id of the left commit
     * @param rightId the commit id of the right commit
     * @return An {@link Optional} of the lowest common ancestor of the two commits, or
     *         {@link Optional#absent()} if a common ancestor could not be found.
     */
    @Override
    public Optional<ObjectId> findLowestCommonAncestor(ObjectId leftId, ObjectId rightId) {
    	com.tinkerpop.blueprints.Index<Vertex> idIndex = graphDB.getIndex("identifiers", Vertex.class);

        Set<Vertex> leftSet = new HashSet<Vertex>();
        Set<Vertex> rightSet = new HashSet<Vertex>();

        Queue<Vertex> leftQueue = new LinkedList<Vertex>();
        Queue<Vertex> rightQueue = new LinkedList<Vertex>();
        
        Vertex leftNode;
        Vertex rightNode;
        CloseableIterable<Vertex> leftResults = null;
        CloseableIterable<Vertex> rightResults = null;
        try {
            leftResults = idIndex.get("id", leftId.toString());
            leftNode = leftResults.iterator().next();
            if (!leftNode.getEdges(com.tinkerpop.blueprints.Direction.OUT).iterator().hasNext()) {
            	return Optional.absent();
            }
            leftQueue.add(leftNode);
            rightResults = idIndex.get("id", rightId.toString());
            rightNode = rightResults.iterator().next();
            if (!rightNode.getEdges(com.tinkerpop.blueprints.Direction.OUT).iterator().hasNext()) {
            	return Optional.absent();
            }
            rightQueue.add(rightNode);
        } finally {
        	if (leftResults != null) leftResults.close();
        	if (rightResults != null) rightResults.close();
        }
            
        List<Vertex> potentialCommonAncestors = new LinkedList<Vertex>();
        while (!leftQueue.isEmpty() || !rightQueue.isEmpty()) {
            if (!leftQueue.isEmpty()) {
                Vertex commit = leftQueue.poll();
                if (processCommit(commit, leftQueue, leftSet, rightQueue, rightSet)) {
                    potentialCommonAncestors.add(commit);
                }
            }
            if (!rightQueue.isEmpty()) {
                Vertex commit = rightQueue.poll();
                if (processCommit(commit, rightQueue, rightSet, leftQueue, leftSet)) {
                    potentialCommonAncestors.add(commit);
                }
            }
        }
        verifyAncestors(potentialCommonAncestors, leftSet, rightSet);

        Optional<ObjectId> ancestor = Optional.absent();
        if (potentialCommonAncestors.size() > 0) {
            ancestor = Optional.of(ObjectId.valueOf((String) potentialCommonAncestors.get(0)
                    .getProperty("id")));
        }
        return ancestor;
    }

//    /**
//     * Helper method for {@link #findLowestCommonAncestor(ObjectId, ObjectId)}.
//     */
//    private boolean processCommit(Node commit, Queue<Node> myQueue, Set<Node> mySet,
//            Queue<Node> theirQueue, Set<Node> theirSet) {
//        if (!mySet.contains(commit)) {
//            mySet.add(commit);
//            if (theirSet.contains(commit)) {
//                // found a common ancestor
//                stopAncestryPath(commit, theirQueue, theirSet);
//
//                return true;
//            }
//            for (Relationship parent : commit.getRelationships(Direction.OUTGOING,
//                    CommitRelationshipTypes.PARENT)) {
//                if (parent.getEndNode().hasRelationship(Direction.OUTGOING)) {
//                    myQueue.add(parent.getEndNode());
//                }
//            }
//        }
//        return false;
//    }
    
    private boolean processCommit(Vertex commit, Queue<Vertex> myQueue, Set<Vertex> mySet, Queue<Vertex> theirQueue, Set<Vertex> theirSet) {
    	if (!mySet.contains(commit)) {
    		mySet.add(commit);
    		if (theirSet.contains(commit)) {
    			stopAncestryPath(commit, theirQueue, theirSet);
    			return true;
    		}
    		for (Edge parentEdge : commit.getEdges(com.tinkerpop.blueprints.Direction.OUT, CommitRelationshipTypes.PARENT.name())) {
    			Vertex parent = parentEdge.getVertex(com.tinkerpop.blueprints.Direction.IN);
    			if (parent.getEdges(com.tinkerpop.blueprints.Direction.OUT).iterator().hasNext()) {
    				myQueue.add(parent);
    			}
    		}
    	}
    	return false;
    	
    }

    /**
     * Helper method for {@link #findLowestCommonAncestor(ObjectId, ObjectId)}.
     */
    private void stopAncestryPath(Node commit, Queue<Node> theirQueue, Set<Node> theirSet) {
        Queue<Node> ancestorQueue = new LinkedList<Node>();
        ancestorQueue.add(commit);
        List<Node> processed = new LinkedList<Node>();
        while (!ancestorQueue.isEmpty()) {
            Node ancestor = ancestorQueue.poll();
            for (Relationship parent : ancestor.getRelationships(CommitRelationshipTypes.PARENT)) {
                Node parentNode = parent.getEndNode();
                if (parentNode.getId() != ancestor.getId()) {
                    if (theirSet.contains(parentNode)) {
                        if (!processed.contains(parentNode)) {
                            ancestorQueue.add(parentNode);
                            processed.add(parentNode);
                        }
                    } else if (theirQueue.contains(parentNode)) {
                        theirQueue.remove(parentNode);
                    }
                }
            }
        }
    }
    
    private void stopAncestryPath(Vertex commit, Queue<Vertex> theirQueue, Set<Vertex> theirSet) {
    	Queue<Vertex> ancestorQueue = new LinkedList<Vertex>();
    	ancestorQueue.add(commit);
    	List<Vertex> processed = new LinkedList<Vertex>();
    	while (!ancestorQueue.isEmpty()) {
    		Vertex ancestor = ancestorQueue.poll();
    		for (Edge parent : ancestor.getEdges(com.tinkerpop.blueprints.Direction.BOTH, CommitRelationshipTypes.PARENT.name())) {
    			Vertex parentNode = parent.getVertex(com.tinkerpop.blueprints.Direction.IN);
    			if (parentNode.getId() != ancestor.getId()) {
    				if (theirSet.contains(parentNode)) {
    					ancestorQueue.add(parentNode);
    					processed.add(parentNode);
    				}
    			} else if (theirQueue.contains(parentNode)) {
    				theirQueue.remove(parentNode);
    			}
    		}
    	}
    }

    private void verifyAncestors(List<Vertex> potentialCommonAncestors, Set<Vertex> leftSet, Set<Vertex> rightSet) {
    	Queue<Vertex> ancestorQueue = new LinkedList<Vertex>();
    	List<Vertex> falseAncestors = new LinkedList<Vertex>();
    	List<Vertex> processed = new LinkedList<Vertex>();
    	
    	for (Vertex v : potentialCommonAncestors) {
    		if (falseAncestors.contains(v)) {
    			continue;
    		}
    		ancestorQueue.add(v);
    		while (!ancestorQueue.isEmpty()) {
    			Vertex ancestor = ancestorQueue.poll();
    			for (Edge parent : ancestor.getEdges(com.tinkerpop.blueprints.Direction.OUT, CommitRelationshipTypes.PARENT.name())) { 
    				Vertex parentNode = parent.getVertex(com.tinkerpop.blueprints.Direction.IN);
    				if (parentNode.getId() != ancestor.getId()) {
    					if (leftSet.contains(parentNode) || rightSet.contains(parentNode)) {
    						if (!processed.contains(parentNode)) {
    							ancestorQueue.add(parentNode);
    							processed.add(parentNode);
    						}
    						if (potentialCommonAncestors.contains(parentNode)) {
    							falseAncestors.add(parentNode);
    						}
    					}
    				}
    			} 
    		}
    	}
    	potentialCommonAncestors.removeAll(falseAncestors);
    }
}
