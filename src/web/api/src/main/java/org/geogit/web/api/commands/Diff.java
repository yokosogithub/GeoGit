package org.geogit.web.api.commands;

import java.util.Iterator;
import java.util.List;

import org.geogit.api.FeatureBuilder;
import org.geogit.api.GeoGIT;
import org.geogit.api.GeogitSimpleFeature;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffEntry.ChangeType;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;
import org.geogit.web.api.WebAPICommand;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class Diff implements WebAPICommand {
    private String oldRefSpec;

    private String newRefSpec;

    private String pathFilter;

    private String crs;

    private Double xMax;

    private Double xMin;

    private Double yMax;

    private Double yMin;

    public void setOldRefSpec(String oldRefSpec) {
        this.oldRefSpec = oldRefSpec;
    }

    public void setNewRefSpec(String newRefSpec) {
        this.newRefSpec = newRefSpec;
    }

    public void setPathFilter(String pathFilter) {
        this.pathFilter = pathFilter;
    }

    public void setCRS(String crs) {
        this.crs = crs;
    }

    public void setXMax(Double xMax) {
        this.xMax = xMax;
    }

    public void setXMin(Double xMin) {
        this.xMin = xMin;
    }

    public void setYMax(Double yMax) {
        this.yMax = yMax;
    }

    public void setYMin(Double yMin) {
        this.yMin = yMin;
    }

    @Override
    public void run(CommandContext context) {
        if (oldRefSpec == null || oldRefSpec.trim().isEmpty()) {
            throw new CommandSpecException("No old ref spec");
        }

        BoundingBox bbox = null;

        if (xMax != null && xMin != null && yMax != null && yMin != null && crs != null) {
            CoordinateReferenceSystem rs;
            try {
                rs = CRS.decode(crs);
            } catch (Exception e) {
                throw new CommandSpecException("Invalid Coordinate Reference System: "
                        + e.getMessage());
            }
            bbox = new ReferencedEnvelope(xMax.intValue(), yMax.intValue(), xMin.intValue(),
                    yMin.intValue(), rs);
        }

        final GeoGIT geogit = context.getGeoGIT();

        final Iterator<DiffEntry> diff = geogit.command(DiffOp.class).setOldVersion(oldRefSpec)
                .setNewVersion(newRefSpec).setFilter(pathFilter).call();

        final List<GeogitSimpleFeature> features = Lists.newLinkedList();
        final List<ChangeType> changes = Lists.newLinkedList();
        while (diff.hasNext()) {
            DiffEntry diffEntry = diff.next();
            Optional<RevObject> feature = Optional.absent();
            Optional<RevObject> type = Optional.absent();
            if (diffEntry.changeType() == ChangeType.ADDED
                    || diffEntry.changeType() == ChangeType.MODIFIED) {
                feature = geogit.command(RevObjectParse.class).setObjectId(diffEntry.newObjectId())
                        .call();
                type = geogit.command(RevObjectParse.class)
                        .setObjectId(diffEntry.getNewObject().getMetadataId()).call();
            } else if (diffEntry.changeType() == ChangeType.REMOVED) {
                feature = geogit.command(RevObjectParse.class).setObjectId(diffEntry.oldObjectId())
                        .call();
                type = geogit.command(RevObjectParse.class)
                        .setObjectId(diffEntry.getOldObject().getMetadataId()).call();
            }
            if (feature.isPresent() && feature.get() instanceof RevFeature && type.isPresent()
                    && type.get() instanceof RevFeatureType) {
                RevFeature revFeature = (RevFeature) feature.get();
                FeatureBuilder builder = new FeatureBuilder((RevFeatureType) type.get());
                GeogitSimpleFeature simpleFeature = (GeogitSimpleFeature) builder.build(revFeature
                        .getId().toString(), revFeature);
                if (bbox != null) {
                    BoundingBox featureBBox = simpleFeature.getBounds();
                    if (!bbox.getCoordinateReferenceSystem().equals(
                            featureBBox.getCoordinateReferenceSystem())) {
                        try {
                            featureBBox = featureBBox.toBounds(bbox.getCoordinateReferenceSystem());
                        } catch (TransformException e) {
                            throw new CommandSpecException(
                                    "Problem transforming feature bounding box from "
                                            + featureBBox.getCoordinateReferenceSystem().toString()
                                            + " to "
                                            + bbox.getCoordinateReferenceSystem().toString() + ": "
                                            + e.getMessage());
                        }
                    }
                    if (!featureBBox.intersects(bbox)) {
                        continue;
                    }
                }
                features.add(simpleFeature);
                changes.add(diffEntry.changeType());
            }
        }

        context.setResponseContent(new CommandResponse() {
            @Override
            public void write(ResponseWriter out) throws Exception {
                out.start();
                out.writeDiffResponse(features.iterator(), changes.iterator());
                out.finish();
            }
        });
    }
}
