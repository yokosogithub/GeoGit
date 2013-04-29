/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.CatObject;
import org.geogit.api.plumbing.FindCommonAncestor;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.api.plumbing.merge.ConflictsReadOp;
import org.geogit.api.porcelain.FeatureNodeRefFromRefspec;
import org.geogit.api.porcelain.MergeOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;

/**
 * Show existing conflicts
 * 
 * @see MergeOp
 */

// Currently it just print conflict descriptions, so they can be used by another tool instead.

@Parameters(commandNames = "conflicts", commandDescription = "Shows existing conflicts")
public class Conflicts extends AbstractCommand implements CLICommand {

    @Parameter(description = "<path> [<path>...]")
    private List<String> paths = Lists.newArrayList();

    @Parameter(names = { "--diff" }, description = "Show diffs instead of full element descriptions")
    private boolean previewDiff;

    private GeoGIT geogit;

    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());

        geogit = cli.getGeogit();
        List<Conflict> conflicts = geogit.command(ConflictsReadOp.class).call();

        if (conflicts.isEmpty()) {
            cli.getConsole().println("No elements need merging.");
            return;
        }
        for (Conflict conflict : conflicts) {
            if (paths.isEmpty() || paths.contains(conflict.getPath())) {
                if (previewDiff) {
                    printConflictDiff(conflict, cli.getConsole(), geogit);
                } else {
                    printConflict(conflict, cli.getConsole(), geogit);
                }
            }
        }
    }

    private void printConflictDiff(Conflict conflict, ConsoleReader console, GeoGIT geogit)
            throws IOException {
        FullDiffPrinter diffPrinter = new FullDiffPrinter(false, true);
        console.println("---" + conflict.getPath() + "---");
        ObjectId mergeHeadId = geogit.command(RefParse.class).setName(Ref.MERGE_HEAD).call().get()
                .getObjectId();
        Optional<RevCommit> mergeHead = geogit.command(RevObjectParse.class)
                .setObjectId(mergeHeadId).call(RevCommit.class);
        ObjectId origHeadId = geogit.command(RefParse.class).setName(Ref.ORIG_HEAD).call().get()
                .getObjectId();
        Optional<RevCommit> origHead = geogit.command(RevObjectParse.class).setObjectId(origHeadId)
                .call(RevCommit.class);
        Optional<RevCommit> commonAncestor = geogit.command(FindCommonAncestor.class)
                .setLeft(mergeHead.get()).setRight(origHead.get()).call();
        String ancestorPath = commonAncestor.get().getId().toString() + ":" + conflict.getPath();
        Optional<NodeRef> ancestorNodeRef = geogit.command(FeatureNodeRefFromRefspec.class)
                .setRefspec(ancestorPath).call();
        String path = Ref.ORIG_HEAD + ":" + conflict.getPath();
        Optional<NodeRef> oursNodeRef = geogit.command(FeatureNodeRefFromRefspec.class)
                .setRefspec(path).call();
        DiffEntry diffEntry = new DiffEntry(ancestorNodeRef.orNull(), oursNodeRef.orNull());
        console.println("Ours");
        diffPrinter.print(geogit, console, diffEntry);
        path = Ref.MERGE_HEAD + ":" + conflict.getPath();
        Optional<NodeRef> theirsNodeRef = geogit.command(FeatureNodeRefFromRefspec.class)
                .setRefspec(path).call();
        diffEntry = new DiffEntry(ancestorNodeRef.orNull(), theirsNodeRef.orNull());
        console.println("Theirs");
        diffPrinter.print(geogit, console, diffEntry);

    }

    private void printConflict(Conflict conflict, ConsoleReader console, GeoGIT geogit)
            throws IOException {

        console.println(conflict.getPath());
        console.println();
        printObject("Ancestor", conflict.getAncestor(), console, geogit);
        console.println();
        printObject("Ours", conflict.getOurs(), console, geogit);
        console.println();
        printObject("Theirs", conflict.getTheirs(), console, geogit);
        console.println();

    }

    private void printObject(String name, ObjectId id, ConsoleReader console, GeoGIT geogit)
            throws IOException {

        console.println(name + "\t" + id.toString());
        if (!id.isNull()) {
            Optional<RevObject> obj = geogit.command(RevObjectParse.class).setObjectId(id).call();
            CharSequence s = geogit.command(CatObject.class)
                    .setObject(Suppliers.ofInstance(obj.get())).call();
            console.println(s);
        }

    }

    private RevFeatureType getFeatureTypeFromRefSpec(String ref) {

        String featureTypeRef = NodeRef.parentPath(ref);

        String treeRef = featureTypeRef.split(":")[0];
        String path = featureTypeRef.split(":")[1];
        ObjectId revTreeId = geogit.command(ResolveTreeish.class).setTreeish(treeRef).call().get();
        RevTree revTree = geogit.command(RevObjectParse.class).setObjectId(revTreeId)
                .call(RevTree.class).get();

        Optional<NodeRef> nodeRef = geogit.command(FindTreeChild.class).setParent(revTree)
                .setChildPath(path).call();
        Preconditions.checkArgument(nodeRef.isPresent(), "Invalid reference: %s", ref);

        RevFeatureType revFeatureType = geogit.command(RevObjectParse.class)
                .setObjectId(nodeRef.get().getMetadataId()).call(RevFeatureType.class).get();
        return revFeatureType;

    }

    private Optional<RevFeature> getFeatureFromRefSpec(String ref) {

        Optional<RevObject> revObject = geogit.command(RevObjectParse.class).setRefSpec(ref)
                .call(RevObject.class);

        if (!revObject.isPresent()) { // let's try to see if it is a feature in the working tree
            NodeRef.checkValidPath(ref);
            Optional<NodeRef> elementRef = geogit.command(FindTreeChild.class)
                    .setParent(geogit.getRepository().getWorkingTree().getTree()).setChildPath(ref)
                    .call();
            Preconditions.checkArgument(elementRef.isPresent(), "Invalid reference: %s", ref);
            ObjectId id = elementRef.get().getNode().getObjectId();
            revObject = geogit.command(RevObjectParse.class).setObjectId(id).call(RevObject.class);
        }

        if (revObject.isPresent()) {
            Preconditions.checkArgument(TYPE.FEATURE.equals(revObject.get().getType()),
                    "%s does not resolve to a feature", ref);
            return Optional.of(RevFeature.class.cast(revObject.get()));
        } else {
            return Optional.absent();
        }
    }

    private NodeRef nodeRefFromRefSpecPath(String ref) {

        Optional<RevFeature> feature = getFeatureFromRefSpec(ref);

        if (feature.isPresent()) {
            RevFeatureType featureType = getFeatureTypeFromRefSpec(ref);
            RevFeature feat = feature.get();
            return new NodeRef(Node.create(NodeRef.nodeFromPath(ref), feat.getId(),
                    featureType.getId(), TYPE.FEATURE), NodeRef.parentPath(ref),
                    featureType.getId());

        } else {
            // return new NodeRef(Node.create("", ObjectId.NULL, ObjectId.NULL, TYPE.FEATURE), "",
            // ObjectId.NULL);
            return null;
        }

    }
}
