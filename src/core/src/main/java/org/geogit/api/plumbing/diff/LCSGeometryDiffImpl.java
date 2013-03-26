package org.geogit.api.plumbing.diff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.geogit.api.plumbing.diff.diff_match_patch.Diff;
import org.geogit.api.plumbing.diff.diff_match_patch.LinesToCharsResult;
import org.geogit.api.plumbing.diff.diff_match_patch.Operation;
import org.geogit.api.plumbing.diff.diff_match_patch.Patch;
import org.geogit.storage.FieldType;
import org.geogit.storage.text.TextValueSerializer;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * An class that computes differences between geometries using a Longest-Common-Subsequence
 * algorithm on string representations of them
 * 
 */
public class LCSGeometryDiffImpl {

    public static final String SUBGEOM_SEPARATOR = "/";

    public static final String INNER_RING_SEPARATOR = "@";

    private LinkedList<Patch> patches;

    private diff_match_patch diffMatchPatch;

    private int totalInsertions;

    private int totalDeletions;

    private int replacings;

    private String diffText;

    public LCSGeometryDiffImpl(Optional<Geometry> oldGeom, Optional<Geometry> newGeom) {
        String oldText = oldGeom.isPresent() ? oldGeom.get().toText() : "";
        String newText = newGeom.isPresent() ? newGeom.get().toText() : "";
        diffMatchPatch = new diff_match_patch();
        LinkedList<Diff> diffs = diffMatchPatch.diff_main(oldText, newText);
        patches = diffMatchPatch.patch_make(diffs);

        // to calculate number of edits in the geometry, we do a diffing based on a string
        // representation of the coordinates of the geometry, instead of the WKT.
        // This is more more practical for counting added/removed/edited points and generating a
        // human-readable and easy-to-parse string representation of the diff.
        // NOTE! This is limited to geometries with less than 65535 different points, and might
        // yield wrong results for geometries over that limit.
        // This is a temporary hack, until a better solution is developed.

        oldText = geomToStringOfCoordinates(oldGeom);
        newText = geomToStringOfCoordinates(newGeom);
        LinesToCharsResult chars = coordsToChars(oldText, newText);
        diffs = diffMatchPatch.diff_main(chars.chars1, chars.chars2);
        charsToCoords(diffs, chars.lineArray);

        processDiffs(diffs);

    }

    private LCSGeometryDiffImpl(LinkedList<Patch> patches) {
        diffMatchPatch = new diff_match_patch();
        this.patches = patches;
    }

    public LCSGeometryDiffImpl(String s) {
        String[] tokens = s.split("\t");
        Preconditions.checkArgument(tokens.length == 2);
        String[] countings = tokens[0].split("/");
        Preconditions.checkArgument(countings.length == 3);
        totalDeletions = Integer.parseInt(countings[0]);
        totalInsertions = Integer.parseInt(countings[1]);
        replacings = Integer.parseInt(countings[2]);
        diffMatchPatch = new diff_match_patch();
        String unescaped = tokens[1].replace("\\n", "\n");
        patches = (LinkedList<Patch>) diffMatchPatch.patch_fromText(unescaped);
    }

    private void processDiffs(List<Diff> diffs) {
        totalInsertions = 0;
        totalDeletions = 0;
        replacings = 0;
        int insertions = 0;
        int deletions = 0;
        StringBuilder sb = new StringBuilder();
        for (Diff diff : diffs) {
            String text = diff.text;
            int nCoords = 0;
            String[] tokens = diff.text.split(" ");
            for (String token : tokens) {
                if (token.contains(",")) {
                    nCoords++;
                }
            }
            switch (diff.operation) {
            case INSERT:
                text = text.replace(" " + SUBGEOM_SEPARATOR, ")" + SUBGEOM_SEPARATOR + "(");
                sb.append("(");
                sb.append(text);
                sb.append(") ");
                insertions += nCoords;
                break;
            case DELETE:
                sb.append("[");
                sb.append(text);
                sb.append("] ");
                deletions += nCoords;
                break;
            case EQUAL:
                sb.append(text.trim());
                sb.append(" ");
                replacings += Math.min(deletions, insertions);
                totalDeletions += Math.max(deletions - insertions, 0);
                totalInsertions += Math.max(insertions - deletions, 0);
                insertions = 0;
                deletions = 0;
                break;
            }
        }
        replacings += Math.min(deletions, insertions);
        totalDeletions += Math.max(deletions - insertions, 0);
        totalInsertions += Math.max(insertions - deletions, 0);

        diffText = sb.toString();
        // some final dirty minor corrections
        diffText = diffText.replace("(" + SUBGEOM_SEPARATOR, SUBGEOM_SEPARATOR + "(");
        diffText = diffText.replace("(" + INNER_RING_SEPARATOR, INNER_RING_SEPARATOR + "(");
        diffText = diffText.replace("[" + SUBGEOM_SEPARATOR, SUBGEOM_SEPARATOR + "[");
        diffText = diffText.replace("[" + INNER_RING_SEPARATOR, INNER_RING_SEPARATOR + "[");
        diffText = diffText.replace(" )", ")");
        diffText = diffText.replace(" ]", "]");
    }

    private String geomToStringOfCoordinates(Optional<Geometry> opt) {
        if (!opt.isPresent()) {
            return "";
        }
        Function<Coordinate, String> printCoords = new Function<Coordinate, String>() {
            @Override
            @Nullable
            public String apply(@Nullable Coordinate coord) {
                return Double.toString(coord.x) + "," + Double.toString(coord.y);
            }

        };

        StringBuilder sb = new StringBuilder();
        Geometry geom = opt.get();
        sb.append(geom.getGeometryType() + " ");
        int n = geom.getNumGeometries();
        for (int i = 0; i < n; i++) {
            Geometry subgeom = geom.getGeometryN(i);
            if (subgeom instanceof Polygon) {
                Polygon polyg = (Polygon) subgeom;
                Coordinate[] coords = polyg.getExteriorRing().getCoordinates();
                Iterator<String> iter = Iterators
                        .transform(Iterators.forArray(coords), printCoords);
                sb.append(Joiner.on(' ').join(iter));
                for (int j = 0; j < polyg.getNumInteriorRing(); j++) {
                    coords = polyg.getInteriorRingN(j).getCoordinates();
                    iter = Iterators.transform(Iterators.forArray(coords), printCoords);
                    sb.append(" " + INNER_RING_SEPARATOR + " ");
                    sb.append(Joiner.on(' ').join(iter));
                }
                if (i < n - 1) {
                    sb.append(" " + SUBGEOM_SEPARATOR + " ");
                }
            } else {
                Coordinate[] coords = subgeom.getCoordinates();
                Iterator<String> iter = Iterators
                        .transform(Iterators.forArray(coords), printCoords);
                sb.append(Joiner.on(' ').join(iter));
                sb.append(" " + SUBGEOM_SEPARATOR + " ");
            }
        }

        String s = sb.toString().trim();
        return s;

    }

    public LCSGeometryDiffImpl reversed() {
        LinkedList<Patch> reversedPatches = diffMatchPatch.patch_deepCopy(patches);
        for (Patch patch : reversedPatches) {
            LinkedList<Diff> diffs = patch.diffs;
            for (Diff diff : diffs) {
                if (diff.operation == Operation.DELETE) {
                    diff.operation = Operation.INSERT;
                } else if (diff.operation == Operation.INSERT) {
                    diff.operation = Operation.DELETE;
                }
            }
        }
        return new LCSGeometryDiffImpl(reversedPatches);
    }

    public boolean canBeAppliedOn(Optional<Geometry> obj) {
        String wkt = obj.isPresent() ? obj.get().toText() : "";
        Object[] res = diffMatchPatch.patch_apply(patches, wkt);
        boolean[] bool = (boolean[]) res[1];
        for (int i = 0; i < bool.length; i++) {
            if (!bool[i]) {
                return false;
            }
        }
        return true;
    }

    public Optional<Geometry> applyOn(Optional<Geometry> obj) {
        Preconditions.checkState(canBeAppliedOn(obj));
        String wkt = obj.isPresent() ? obj.get().toText() : "";
        String res = (String) diffMatchPatch.patch_apply(patches, wkt)[0];
        if (!res.isEmpty()) {
            return Optional.fromNullable((Geometry) TextValueSerializer.fromString(
                    FieldType.forBinding(Geometry.class), res));
        } else {
            return Optional.absent();
        }
    }

    /**
     * Returns a human-readable representation of the difference
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toString(totalDeletions) + " point(s) deleted, ");
        sb.append(Integer.toString(totalInsertions) + " new point(s) added, ");
        sb.append(Integer.toString(replacings) + " point(s) moved");

        return sb.toString();
    }

    /**
     * Returns a serialized text version of the difference
     */
    public String asText() {
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toString(totalDeletions));
        sb.append("/");
        sb.append(Integer.toString(totalInsertions));
        sb.append("/");
        sb.append(Integer.toString(replacings));
        sb.append("\t");
        sb.append(diffMatchPatch.patch_toText(patches).replace("\n", "\\n"));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LCSGeometryDiffImpl)) {
            return false;
        }
        LCSGeometryDiffImpl d = (LCSGeometryDiffImpl) o;
        for (int i = 0; i < d.patches.size(); i++) {
            Patch patchA = patches.get(i);
            Patch patchB = d.patches.get(i);
            if (!patchA.equals(patchB)) {
                return false;
            }
        }
        return true;
    }

    // ========================================================================================
    // These 2 methods are meant to be used to split a text containing a representation of
    // coordinate in a geometry into chunks representing points that can be then hashed
    // as characters.
    // They are just an adapted version of the diff_lineToChars method in diff_match_patch, using a
    // different split character and some extra behaviour

    protected LinesToCharsResult coordsToChars(String text1, String text2) {
        List<String> lineArray = new ArrayList<String>();
        Map<String, Integer> lineHash = new HashMap<String, Integer>();
        lineArray.add("");

        String chars1 = splitAndHash(text1, lineArray, lineHash, ' ');
        String chars2 = splitAndHash(text2, lineArray, lineHash, ' ');
        return new LinesToCharsResult(chars1, chars2, lineArray);
    }

    private String splitAndHash(String text, List<String> lineArray, Map<String, Integer> lineHash,
            char splitChar) {
        StringBuilder chars = new StringBuilder();
        Iterable<String> tokens = Splitter.on(" ").split(text);
        for (String token : tokens) {
            if (lineHash.containsKey(token)) {
                chars.append(String.valueOf((char) (int) lineHash.get(token)));
            } else {
                lineArray.add(token);
                lineHash.put(token, lineArray.size() - 1);
                chars.append(String.valueOf((char) (lineArray.size() - 1)));
            }
        }
        return chars.toString();
    }

    protected void charsToCoords(LinkedList<Diff> diffs, List<String> lineArray) {
        StringBuilder text;
        for (Diff diff : diffs) {
            text = new StringBuilder();
            for (int y = 0; y < diff.text.length(); y++) {
                String coordText = lineArray.get(diff.text.charAt(y));
                text.append(coordText);
                if (coordText.length() > 2) {
                    text.append(' ');
                }
            }
            diff.text = text.toString();
        }
    }

    public String getDiffCoordsString() {
        return diffText;
    }

    // ========================================================================================
    //
    // protected static class GeomToStringResult {
    // protected String chars1;
    //
    // protected String chars2;
    //
    // protected List<Object> lineArray;
    //
    // protected GeomToStringResult(String chars1, String chars2, List<Object> lineArray) {
    // this.chars1 = chars1;
    // this.chars2 = chars2;
    // this.lineArray = lineArray;
    // }
    // }
    //
    // public enum PARTICLES {
    // POINT, LINESTRING, POLYGON, MULTIPOINT, MULTILINESTRING, MULTIPOLYGON, PART, HOLE,
    // }
    //
    // protected GeomToStringResult geomsToString(Optional<Geometry> geom1, Optional<Geometry>
    // geom2) {
    // List<Object> lineArray = Lists.newArrayList();
    // Map<Object, Integer> lineHash = Maps.newHashMap();
    // lineArray.add("");
    //
    // Object[] array1 = geom1.isPresent() ? geomToArray(geom1.get()) : new Object[0];
    // Object[] array2 = geom1.isPresent() ? geomToArray(geom2.get()) : new Object[0];
    // String chars1 = arrayToString(array1, lineArray, lineHash);
    // String chars2 = arrayToString(array2, lineArray, lineHash);
    // return new GeomToStringResult(chars1, chars2, lineArray);
    // }
    //
    // private Object[] geomToArray(Geometry geometry) {
    // List<Object> list = Lists.newArrayList();
    // if (geometry instanceof LineString) {
    // list.add(PARTICLES.LINESTRING);
    // for (Coordinate coord : geometry.getCoordinates()) {
    // list.add(coord);
    // }
    // }
    // return list.toArray(new Object[0]);
    // }
    //
    // private String arrayToString(Object[] array, List<Object> lineArray,
    // Map<Object, Integer> lineHash) {
    // StringBuilder chars = new StringBuilder();
    // for (Object obj : array) {
    // if (lineHash.containsKey(obj)) {
    // chars.append(String.valueOf((char) (int) lineHash.get(obj)));
    // } else {
    // lineArray.add(obj);
    // lineHash.put(obj, lineArray.size() - 1);
    // chars.append(String.valueOf((char) (lineArray.size() - 1)));
    // }
    // }
    // return chars.toString();
    // }
    //
    // protected void stringToArray(LinkedList<Diff> diffs, List<Object> lineArray) {
    //
    // }

}
