package org.geogit.web.api.commands;

import java.util.HashMap;
import java.util.Map;

import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.diff.AttributeDiff;
import org.geogit.api.plumbing.diff.FeatureDiff;
import org.geogit.api.plumbing.diff.GenericAttributeDiffImpl;
import org.geogit.api.plumbing.diff.GeometryAttributeDiff;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;
import org.geogit.web.api.WebAPICommand;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Geometry;

public class FeatureDiffWeb implements WebAPICommand {

    private String path;

    private String newCommitId;

    private String oldCommitId;

    public void setPath(String path) {
        this.path = path;
    }

    public void setNewCommitId(String newCommitId) {
        this.newCommitId = newCommitId;
    }

    public void setOldCommitId(String oldCommitId) {
        this.oldCommitId = oldCommitId;
    }

    private Optional<NodeRef> parseID(ObjectId id, GeoGIT geogit) {
        Optional<RevObject> object = geogit.command(RevObjectParse.class).setObjectId(id).call();
        RevCommit commit = null;
        if (object.isPresent() && object.get() instanceof RevCommit) {
            commit = (RevCommit) object.get();
        } else {
            throw new CommandSpecException("Couldn't resolve id: " + id.toString() + " to a commit");
        }

        object = geogit.command(RevObjectParse.class).setObjectId(commit.getTreeId()).call();

        if (object.isPresent()) {
            RevTree tree = (RevTree) object.get();
            return geogit.command(FindTreeChild.class).setParent(tree).setChildPath(path).call();
        } else {
            throw new CommandSpecException("Couldn't resolve commit's treeId");
        }
    }

    @Override
    public void run(CommandContext context) {
        if (path == null || path.trim().isEmpty()) {
            throw new CommandSpecException("No path for feature name specifed");
        } else if (newCommitId.equals(ObjectId.NULL.toString()) || newCommitId.trim().isEmpty()) {
            throw new CommandSpecException("No newCommitId specified");
        }

        ObjectId newId = ObjectId.valueOf(newCommitId);
        ObjectId oldId = ObjectId.valueOf(oldCommitId);

        final GeoGIT geogit = context.getGeoGIT();

        RevFeature newFeature = null;
        RevFeatureType newFeatureType = null;

        RevFeature oldFeature = null;
        RevFeatureType oldFeatureType = null;

        final Map<PropertyDescriptor, AttributeDiff> diffs;

        Optional<NodeRef> ref = parseID(newId, geogit);

        Optional<RevObject> object;

        boolean removed = false;

        if (ref.isPresent()) {
            object = geogit.command(RevObjectParse.class).setObjectId(ref.get().getMetadataId())
                    .call();
            if (object.isPresent() && object.get() instanceof RevFeatureType) {
                newFeatureType = (RevFeatureType) object.get();
            } else {
                throw new CommandSpecException("Couldn't resolve newCommit's featureType");
            }
            object = geogit.command(RevObjectParse.class).setObjectId(ref.get().objectId()).call();
            if (object.isPresent() && object.get() instanceof RevFeature) {
                newFeature = (RevFeature) object.get();
            } else {
                throw new CommandSpecException("Couldn't resolve newCommit's feature");
            }
        } else {
            removed = true;
        }

        if (!oldId.equals(ObjectId.NULL)) {
            ref = parseID(oldId, geogit);

            if (ref.isPresent()) {
                object = geogit.command(RevObjectParse.class)
                        .setObjectId(ref.get().getMetadataId()).call();
                if (object.isPresent() && object.get() instanceof RevFeatureType) {
                    oldFeatureType = (RevFeatureType) object.get();
                } else {
                    throw new CommandSpecException("Couldn't resolve oldCommit's featureType");
                }
                object = geogit.command(RevObjectParse.class).setObjectId(ref.get().objectId())
                        .call();
                if (object.isPresent() && object.get() instanceof RevFeature) {
                    oldFeature = (RevFeature) object.get();
                } else {
                    throw new CommandSpecException("Couldn't resolve oldCommit's feature");
                }
            } else {
                throw new CommandSpecException(
                        "Couldn't resolve path to a node in the oldCommit's tree");
            }

            if (!removed) {
                FeatureDiff diff = new FeatureDiff(path, newFeature, oldFeature, newFeatureType,
                        oldFeatureType);
                diffs = diff.getDiffs();
            } else {
                Map<PropertyDescriptor, AttributeDiff> tempDiffs = new HashMap<PropertyDescriptor, AttributeDiff>();
                ImmutableList<PropertyDescriptor> attributes = oldFeatureType.sortedDescriptors();
                ImmutableList<Optional<Object>> values = oldFeature.getValues();
                for (int index = 0; index < attributes.size(); index++) {
                    Optional<Object> value = values.get(index);
                    if (Geometry.class.isAssignableFrom(attributes.get(index).getType()
                            .getBinding())) {
                        tempDiffs.put(
                                attributes.get(index),
                                new GeometryAttributeDiff(Optional.fromNullable((Geometry) value
                                        .orNull()), Optional.fromNullable((Geometry) null)));
                    } else {
                        tempDiffs.put(attributes.get(index), new GenericAttributeDiffImpl(value,
                                null));
                    }
                }
                diffs = tempDiffs;
            }

        } else {
            Map<PropertyDescriptor, AttributeDiff> tempDiffs = new HashMap<PropertyDescriptor, AttributeDiff>();
            ImmutableList<PropertyDescriptor> attributes = newFeatureType.sortedDescriptors();
            ImmutableList<Optional<Object>> values = newFeature.getValues();
            for (int index = 0; index < attributes.size(); index++) {
                Optional<Object> value = values.get(index);
                if (Geometry.class.isAssignableFrom(attributes.get(index).getType().getBinding())) {
                    tempDiffs.put(attributes.get(index),
                            new GeometryAttributeDiff(Optional.fromNullable((Geometry) null),
                                    Optional.fromNullable((Geometry) value.orNull())));
                } else {
                    tempDiffs.put(attributes.get(index), new GenericAttributeDiffImpl(null, value));
                }
            }
            diffs = tempDiffs;
        }

        context.setResponseContent(new CommandResponse() {

            @Override
            public void write(ResponseWriter out) throws Exception {
                out.start();
                out.writeFeatureDiffResponse(diffs);
                out.finish();
            }
        });
    }

}
