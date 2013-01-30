package org.geogit.cli.porcelain;

import static org.fusesource.jansi.Ansi.Color.BLUE;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.geogit.api.plumbing.diff.DiffEntry.ChangeType.ADDED;
import static org.geogit.api.plumbing.diff.DiffEntry.ChangeType.MODIFIED;

import java.io.IOException;
import java.util.Iterator;
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
import org.geogit.cli.AnsiDecorator;
import org.geogit.storage.text.AttributeValueSerializer;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.base.Optional;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;

interface DiffPrinter {

    /**
     * @param geogit
     * @param console
     * @param entry
     * @throws IOException
     */
    void print(GeoGIT geogit, ConsoleReader console, DiffEntry entry) throws IOException;

}

class SummaryDiffPrinter implements DiffPrinter {

    @Override
    public void print(GeoGIT geogit, ConsoleReader console, DiffEntry entry) throws IOException {

        Ansi ansi = AnsiDecorator.newAnsi(console.getTerminal().isAnsiSupported());

        final NodeRef newObject = entry.getNewObject();
        final NodeRef oldObject = entry.getOldObject();

        String oldMode = oldObject == null ? shortOid(ObjectId.NULL) : shortOid(oldObject
                .getMetadataId());
        String newMode = newObject == null ? shortOid(ObjectId.NULL) : shortOid(newObject
                .getMetadataId());

        String oldId = oldObject == null ? shortOid(ObjectId.NULL) : shortOid(oldObject.objectId());
        String newId = newObject == null ? shortOid(ObjectId.NULL) : shortOid(newObject.objectId());

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

class FullDiffPrinter implements DiffPrinter {

    SummaryDiffPrinter summaryPrinter = new SummaryDiffPrinter();

    private boolean noGeom;

    private boolean noHeader;

    public FullDiffPrinter(boolean noGeom, boolean noHeader) {
        this.noGeom = noGeom;
        this.noHeader = noHeader;
    }

    @Override
    public void print(GeoGIT geogit, ConsoleReader console, DiffEntry diffEntry) throws IOException {

        if (!noHeader) {
            summaryPrinter.print(geogit, console, diffEntry);
        }

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
                        && !noGeom) {
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
