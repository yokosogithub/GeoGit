package org.geogit.web.api.repo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.RepositoryFilter;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.remote.BinaryPackedChanges;
import org.geogit.remote.FilteredDiffIterator;
import org.geogit.repository.Repository;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FilteredChangesResource extends ServerResource {
    @Override
    protected Representation post(Representation entity) throws ResourceException {
        try {
            final Reader body = entity.getReader();
            final JsonParser parser = new JsonParser();
            final JsonElement messageJson = parser.parse(body);

            final List<ObjectId> tracked = new ArrayList<ObjectId>();

            RepositoryFilter filter = new RepositoryFilter();

            ObjectId commitId = ObjectId.NULL;

            if (messageJson.isJsonObject()) {
                final JsonObject message = messageJson.getAsJsonObject();
                final JsonArray trackedArray;
                if (message.has("tracked") && message.get("tracked").isJsonArray()) {
                    trackedArray = message.get("tracked").getAsJsonArray();
                } else {
                    trackedArray = new JsonArray();
                }
                if (message.has("commitId") && message.get("commitId").isJsonPrimitive()) {
                    commitId = ObjectId.valueOf(message.get("commitId").getAsJsonPrimitive()
                            .getAsString());
                } else {
                    commitId = ObjectId.NULL;
                }
                for (final JsonElement e : trackedArray) {
                    if (e.isJsonPrimitive()) {
                        tracked.add(ObjectId.valueOf(e.getAsJsonPrimitive().getAsString()));
                    }
                }

                if (message.has("filter") && message.get("filter").isJsonArray()) {
                    JsonArray filterArray = message.get("filter").getAsJsonArray();
                    for (final JsonElement e : filterArray) {
                        if (e.isJsonObject()) {
                            JsonObject filterObject = e.getAsJsonObject();
                            String featureType = null;
                            String filterType = null;
                            String filterText = null;
                            if (filterObject.has("featuretype")
                                    && filterObject.get("featuretype").isJsonPrimitive()) {
                                featureType = filterObject.get("featuretype").getAsJsonPrimitive()
                                        .getAsString();
                            }
                            if (filterObject.has("type")
                                    && filterObject.get("type").isJsonPrimitive()) {
                                filterType = filterObject.get("type").getAsJsonPrimitive()
                                        .getAsString();
                            }
                            if (filterObject.has("filter")
                                    && filterObject.get("filter").isJsonPrimitive()) {
                                filterText = filterObject.get("filter").getAsJsonPrimitive()
                                        .getAsString();
                            }
                            if (featureType != null && filterType != null && filterText != null) {
                                filter.addFilter(featureType, filterType, filterText);
                            }
                        }
                    }

                }
            }

            final GeoGIT ggit = (GeoGIT) getApplication().getContext().getAttributes()
                    .get("geogit");
            final Repository repository = ggit.getRepository();

            RevCommit commit = repository.getCommit(commitId);

            ObjectId parent = ObjectId.NULL;
            if (commit.getParentIds().size() > 0) {
                parent = commit.getParentIds().get(0);
            }

            Iterator<DiffEntry> changes = ggit.command(DiffOp.class).setNewVersion(commit.getId())
                    .setOldVersion(parent).setReportTrees(true).call();
            FilteredDiffIterator filteredChanges = new FilteredDiffIterator(changes, repository,
                    filter) {
                @Override
                protected boolean trackingObject(ObjectId objectId) {
                    return tracked.contains(objectId);
                }
            };

            return new FilteredDiffIteratorRepresentation(new BinaryPackedChanges(repository),
                    filteredChanges);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final MediaType PACKED_OBJECTS = new MediaType("application/x-geogit-packed");

    private class FilteredDiffIteratorRepresentation extends OutputRepresentation {

        private final BinaryPackedChanges packer;

        private final FilteredDiffIterator changes;

        public FilteredDiffIteratorRepresentation(BinaryPackedChanges packer,
                FilteredDiffIterator changes) {
            super(PACKED_OBJECTS);
            this.changes = changes;
            this.packer = packer;
        }

        @Override
        public void write(OutputStream out) throws IOException {
            packer.write(out, changes);
            // signal the end of changes
            out.write(2);
            if (changes.wasFiltered()) {
                out.write(1);
            } else {
                out.write(0);
            }
        }
    }
}
