package org.geogit.storage;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe.LoopBundle;

public abstract class TransactionalBlueprintsGraphDatabase<DB extends IndexableGraph & TransactionalGraph>
        extends BlueprintsGraphDatabase<DB> {
    public TransactionalBlueprintsGraphDatabase(Platform platform) {
        super(platform);
    }

    @Override
    protected void commit() {
        graphDB.commit();
    }

    @Override
    protected void rollback() {
        graphDB.rollback();
    }
}
