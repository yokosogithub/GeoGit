package org.geogit.web.api.repo;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.DiffOp;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.WriterRepresentation;
import org.restlet.resource.ServerResource;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class AffectedFeaturesResource extends ServerResource {
    {
        getVariants().add(new AffectedFeaturesRepresentation());
    }

    private class AffectedFeaturesRepresentation extends WriterRepresentation {
        public AffectedFeaturesRepresentation() {
            super(MediaType.TEXT_PLAIN);
        }

        @Override
        public void write(Writer w) throws IOException {
            Form options = getRequest().getResourceRef().getQueryAsForm();

            Optional<String> commit = Optional
                    .fromNullable(options.getFirstValue("commitId", null));

            Preconditions.checkState(commit.isPresent(), "No commit specified.");

            GeoGIT ggit = (GeoGIT) getApplication().getContext().getAttributes().get("geogit");

            ObjectId commitId = ObjectId.valueOf(commit.get());

            RevCommit revCommit = ggit.getRepository().getCommit(commitId);

            if (revCommit.getParentIds() != null && revCommit.getParentIds().size() > 0) {
                ObjectId parentId = revCommit.getParentIds().get(0);
                final Iterator<DiffEntry> diff = ggit.command(DiffOp.class).setOldVersion(parentId)
                        .setNewVersion(commitId).call();

                while (diff.hasNext()) {
                    DiffEntry diffEntry = diff.next();
                    if (diffEntry.getOldObject() != null) {
                        w.write(diffEntry.getOldObject().getNode().getObjectId().toString() + "\n");
                    }
                }
                w.flush();
            }
        }
    }
}
