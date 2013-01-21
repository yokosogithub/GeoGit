/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static org.fusesource.jansi.Ansi.Color.BLUE;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.geogit.api.plumbing.diff.DiffEntry.ChangeType.ADDED;
import static org.geogit.api.plumbing.diff.DiffEntry.ChangeType.MODIFIED;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jline.console.ConsoleReader;

import org.fusesource.jansi.Ansi;
import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.plumbing.DiffFeature;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.diff.AttributeDiff;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffEntry.ChangeType;
import org.geogit.api.plumbing.diff.FeatureDiff;
import org.geogit.api.plumbing.diff.GeometryAttributeDiff;
import org.geogit.api.plumbing.diff.LCSGeometryDiffImpl;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.AnsiDecorator;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.storage.text.AttributeValueSerializer;
import org.opengis.feature.type.PropertyDescriptor;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Shows changes between commits, commits and working tree, etc.
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit diff [-- <path>...]}: compare working tree and index
 * <li> {@code geogit diff <commit> [-- <path>...]}: compare the working tree with the given commit
 * <li> {@code geogit diff --cached [-- <path>...]}: compare the index with the HEAD commit
 * <li> {@code geogit diff --cached <commit> [-- <path>...]}: compare the index with the given commit
 * <li> {@code geogit diff <commit1> <commit2> [-- <path>...]}: compare {@code commit1} with
 * {@code commit2}, where {@code commit1} is the eldest or left side of the diff.
 * </ul>
 * 
 * @see DiffOp
 */
@Parameters(commandNames = "diff", commandDescription = "Show changes between commits, commit and working tree, etc")
public class Diff extends AbstractCommand implements CLICommand {

    @Parameter(description = "[<commit> [<commit>]] [-- <path>...]", arity = 2)
    private List<String> refSpec = Lists.newArrayList();

    @Parameter(names = "--", hidden = true, variableArity = true)
    private List<String> paths = Lists.newArrayList();

    @Parameter(names = "--cached", description = "compares the specified tree (commit, branch, etc) and the staging area")
    private boolean cached;

    @Parameter(names = "--summary", description = "List only summary of changes")
    private boolean summary;

    @Parameter(names = "--nogeom", description = "Do not show detailed coordinate changes in geometries")
    private boolean nogeom;

    /**
     * Executes the diff command with the specified options.
     * 
     * @param cli
     * @throws Exception
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        if (refSpec.size() > 2) {
            cli.getConsole().println("Commit list is too long :" + refSpec);
            return;
        }

        if (nogeom && summary) {
            cli.getConsole().println("Only one printing mode allowed");
            return;
        }

        GeoGIT geogit = cli.getGeogit();

        DiffOp diff = geogit.command(DiffOp.class);

        String oldVersion = resolveOldVersion();
        String newVersion = resolveNewVersion();

        diff.setOldVersion(oldVersion).setNewVersion(newVersion).setCompareIndex(cached);

        Iterator<DiffEntry> entries;
        if (paths.isEmpty()) {
            entries = diff.setProgressListener(cli.getProgressListener()).call();
        } else {
            entries = Iterators.emptyIterator();
            for (String path : paths) {
                Iterator<DiffEntry> moreEntries = diff.setFilter(path)
                        .setProgressListener(cli.getProgressListener()).call();
                entries = Iterators.concat(entries, moreEntries);
            }
        }

        if (!entries.hasNext()) {
            cli.getConsole().println("No differences found");
            return;
        }

        DiffPrinter printer;
        if (summary) {
            printer = new SummaryPrinter();
        } else {
            printer = new FullPrinter();
        }

        List<ObjectId> newFeatureTypes = Lists.newArrayList();
        DiffEntry entry;
        while (entries.hasNext()) {
            entry = entries.next();
            printer.print(geogit, cli.getConsole(), entry, newFeatureTypes);
        }
    }

    private String resolveOldVersion() {
        return refSpec.size() > 0 ? refSpec.get(0) : null;
    }

    private String resolveNewVersion() {
        return refSpec.size() > 1 ? refSpec.get(1) : null;
    }

    private interface DiffPrinter {

        /**
         * @param geogit
         * @param console
         * @param entry
         * @throws IOException
         */
        void print(GeoGIT geogit, ConsoleReader console, DiffEntry entry,
                List<ObjectId> featureTypes) throws IOException;

    }

    private class SummaryPrinter implements DiffPrinter {

        @Override
        public void print(GeoGIT geogit, ConsoleReader console, DiffEntry entry,
                List<ObjectId> featureTypes) throws IOException {

            Ansi ansi = AnsiDecorator.newAnsi(console.getTerminal().isAnsiSupported());

            final NodeRef newObject = entry.getNewObject();
            final NodeRef oldObject = entry.getOldObject();

            String oldMode = oldObject == null ? shortOid(ObjectId.NULL) : shortOid(oldObject
                    .getMetadataId());
            String newMode = newObject == null ? shortOid(ObjectId.NULL) : shortOid(newObject
                    .getMetadataId());

            String oldId = oldObject == null ? shortOid(ObjectId.NULL) : shortOid(oldObject
                    .objectId());
            String newId = newObject == null ? shortOid(ObjectId.NULL) : shortOid(newObject
                    .objectId());

            ansi.a(oldMode).a(" ");
            ansi.a(newMode).a(" ");

            ansi.a(oldId).a(" ");
            ansi.a(newId).a(" ");

            ansi.fg(entry.changeType() == ADDED ? GREEN : (entry.changeType() == MODIFIED ? YELLOW
                    : RED));
            char type = entry.changeType().toString().charAt(0);
            ansi.a("  ").a(type).reset();
            ansi.a("  ").a(formatPath(entry));

            console.println(ansi.toString());

        }

    }

    private class FullPrinter implements DiffPrinter {

        SummaryPrinter summaryPrinter = new SummaryPrinter();

        @Override
        public void print(GeoGIT geogit, ConsoleReader console, DiffEntry diffEntry,
                List<ObjectId> featureTypes) throws IOException {

            summaryPrinter.print(geogit, console, diffEntry, featureTypes);

            if (diffEntry.changeType() == ChangeType.MODIFIED) {
                FeatureDiff diff = geogit.command(DiffFeature.class)
                        .setNewVersion(Suppliers.ofInstance(diffEntry.getNewObject()))
                        .setOldVersion(Suppliers.ofInstance(diffEntry.getOldObject())).call();

                Map<PropertyDescriptor, AttributeDiff> diffs = diff.getDiffs();

                Ansi ansi = AnsiDecorator.newAnsi(console.getTerminal().isAnsiSupported());
                Set<Entry<PropertyDescriptor, AttributeDiff>> entries = diffs.entrySet();
                Iterator<Entry<PropertyDescriptor, AttributeDiff>> iter = entries.iterator();
                while (iter.hasNext()) {
                    Entry<PropertyDescriptor, AttributeDiff> entry = iter.next();
                    PropertyDescriptor pd = entry.getKey();
                    AttributeDiff ad = entry.getValue();
                    if (ad instanceof GeometryAttributeDiff
                            && ad.getType() == org.geogit.api.plumbing.diff.AttributeDiff.TYPE.MODIFIED
                            && !nogeom) {
                        GeometryAttributeDiff gd = (GeometryAttributeDiff) ad;
                        ansi.fg(YELLOW);
                        ansi.a(pd.getName()).a(": ");
                        ansi.reset();
                        String text = gd.getDiff().getDiffCoordsString();
                        for (int i = 0; i < text.length(); i++) {
                            if (text.charAt(i) == '(') {
                                ansi.fg(GREEN);
                                ansi.a(text.charAt(i));
                            } else if (text.charAt(i) == '[') {
                                ansi.fg(RED);
                                ansi.a(text.charAt(i));
                            } else if (text.charAt(i) == ']' || text.charAt(i) == ')') {
                                ansi.a(text.charAt(i));
                                ansi.reset();
                            } else if (text.charAt(i) == LCSGeometryDiffImpl.INNER_RING_SEPARATOR
                                    .charAt(0)
                                    || text.charAt(i) == LCSGeometryDiffImpl.SUBGEOM_SEPARATOR
                                            .charAt(0)) {
                                ansi.fg(BLUE);
                                ansi.a(text.charAt(i));
                                ansi.reset();
                            } else {
                                ansi.a(text.charAt(i));
                            }
                        }
                        ansi.reset();
                        ansi.newline();
                    } else {
                        ansi.fg(ad.getType() == org.geogit.api.plumbing.diff.AttributeDiff.TYPE.ADDED ? GREEN
                                : (ad.getType() == org.geogit.api.plumbing.diff.AttributeDiff.TYPE.REMOVED ? RED
                                        : YELLOW));
                        ansi.a(pd.getName()).a(": ").a(ad.toString());
                        ansi.reset();
                        ansi.newline();
                    }
                }
                console.println(ansi.toString());
            } else if (diffEntry.changeType() == ChangeType.ADDED) {
                NodeRef noderef = diffEntry.getNewObject();
                RevFeatureType featureType = geogit.command(RevObjectParse.class)
                        .setObjectId(noderef.getMetadataId()).call(RevFeatureType.class).get();
                Optional<RevObject> obj = geogit.command(RevObjectParse.class)
                        .setObjectId(noderef.objectId()).call();
                RevFeature feature = (RevFeature) obj.get();
                ImmutableList<Optional<Object>> values = feature.getValues();
                int i = 0;
                for (Optional<Object> opt : values) {
                    if (opt.isPresent()) {
                        Object value = opt.get();
                        console.println(featureType.sortedDescriptors().get(i).getName() + "\t"
                                + AttributeValueSerializer.asText(value));
                    } else {
                        console.println("NULL");
                    }
                    i++;
                }
                console.println();
            }

        }
    }

    private static String shortOid(ObjectId oid) {
        return new StringBuilder(oid.toString().substring(0, 6)).append("...").toString();
    }

    private static String formatPath(DiffEntry entry) {
        String path;
        NodeRef oldObject = entry.getOldObject();
        NodeRef newObject = entry.getNewObject();
        if (oldObject == null) {
            path = newObject.path();
        } else if (newObject == null) {
            path = oldObject.path();
        } else {
            if (oldObject.path().equals(newObject.path())) {
                path = oldObject.path();
            } else {
                path = oldObject.path() + " -> " + newObject.path();
            }
        }
        return path;
    }
}
