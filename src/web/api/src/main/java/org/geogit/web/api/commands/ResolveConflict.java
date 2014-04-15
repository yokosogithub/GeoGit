/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.commands;

import static com.google.common.base.Preconditions.checkState;

import org.geogit.api.CommandLocator;
import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeBuilder;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.WriteBack;
import org.geogit.api.porcelain.AddOp;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * The interface for the Add operation in GeoGit.
 * 
 * Web interface for {@link AddOp}
 */

public class ResolveConflict extends AbstractWebAPICommand {

    private String path;

    private ObjectId objectId;

    /**
     * Mutator for the path variable
     * 
     * @param path - the path to the feature you want to add
     */
    public void setPath(String path) {
        this.path = path;
    }

    public void setFeatureObjectId(String objectId) {
        this.objectId = ObjectId.valueOf(objectId);
    }

    /**
     * Runs the command and builds the appropriate response
     * 
     * @param context - the context to use for this command
     * 
     * @throws CommandSpecException
     */
    @Override
    public void run(CommandContext context) {
        if (this.getTransactionId() == null) {
            throw new CommandSpecException(
                    "No transaction was specified, add requires a transaction to preserve the stability of the repository.");
        }
        final CommandLocator geogit = this.getCommandLocator(context);

        RevTree revTree = geogit.getWorkingTree().getTree();

        Optional<NodeRef> nodeRef = geogit.command(FindTreeChild.class).setParent(revTree)
                .setChildPath(NodeRef.parentPath(path)).setIndex(true).call();
        Preconditions.checkArgument(nodeRef.isPresent(), "Invalid reference: %s",
                NodeRef.parentPath(path));

        RevFeatureType revFeatureType = geogit.command(RevObjectParse.class)
                .setObjectId(nodeRef.get().getMetadataId()).call(RevFeatureType.class).get();

        RevFeature revFeature = geogit.command(RevObjectParse.class).setObjectId(objectId)
                .call(RevFeature.class).get();

        CoordinateReferenceSystem crs = revFeatureType.type().getCoordinateReferenceSystem();
        Envelope bounds = ReferencedEnvelope.create(crs);

        Optional<Object> o;
        for (int i = 0; i < revFeature.getValues().size(); i++) {
            o = revFeature.getValues().get(i);
            if (o.isPresent() && o.get() instanceof Geometry) {
                Geometry g = (Geometry) o.get();
                if (bounds.isNull()) {
                    bounds.init(JTS.bounds(g, crs));
                } else {
                    bounds.expandToInclude(JTS.bounds(g, crs));
                }
            }
        }

        NodeRef node = new NodeRef(Node.create(NodeRef.nodeFromPath(path), objectId, ObjectId.NULL,
                TYPE.FEATURE, bounds), NodeRef.parentPath(path), ObjectId.NULL);

        Optional<NodeRef> parentNode = geogit.command(FindTreeChild.class)
                .setParent(geogit.getWorkingTree().getTree()).setChildPath(node.getParentPath())
                .setIndex(true).call();
        RevTreeBuilder treeBuilder = null;
        ObjectId metadataId = ObjectId.NULL;
        if (parentNode.isPresent()) {
            metadataId = parentNode.get().getMetadataId();
            Optional<RevTree> parsed = geogit.command(RevObjectParse.class)
                    .setObjectId(parentNode.get().getNode().getObjectId()).call(RevTree.class);
            checkState(parsed.isPresent(), "Parent tree couldn't be found in the repository.");
            treeBuilder = new RevTreeBuilder(geogit.getIndex().getDatabase(), parsed.get());
            treeBuilder.remove(node.getNode().getName());
        } else {
            treeBuilder = new RevTreeBuilder(geogit.getIndex().getDatabase());
        }
        treeBuilder.put(node.getNode());
        ObjectId newTreeId = geogit
                .command(WriteBack.class)
                .setAncestor(
                        geogit.getWorkingTree().getTree().builder(geogit.getIndex().getDatabase()))
                .setChildPath(node.getParentPath()).setToIndex(true).setTree(treeBuilder.build())
                .setMetadataId(metadataId).call();
        geogit.getWorkingTree().updateWorkHead(newTreeId);

        AddOp command = geogit.command(AddOp.class);

        command.addPattern(path);

        command.call();

        context.setResponseContent(new CommandResponse() {
            @Override
            public void write(ResponseWriter out) throws Exception {
                out.start();
                out.writeElement("Add", "Success");
                out.finish();
            }
        });
    }
}
