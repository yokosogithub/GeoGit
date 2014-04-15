/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.rest.repository;

import static org.geogit.rest.repository.RESTUtils.getGeogit;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.DiffOp;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Returns a list of all feature ids affected by a specified commit.
 */
public class AffectedFeaturesResource extends Resource {

    @Override
    public void init(Context context, Request request, Response response) {
        super.init(context, request, response);
        List<Variant> variants = getVariants();
        variants.add(new AffectedFeaturesRepresentation(request));
    }

    private static class AffectedFeaturesRepresentation extends OutputRepresentation {

        private Request request;

        public AffectedFeaturesRepresentation(Request request) {
            super(MediaType.TEXT_PLAIN);
            this.request = request;
        }

        @Override
        public void write(OutputStream out) throws IOException {
            PrintWriter w = new PrintWriter(out);
            Form options = request.getResourceRef().getQueryAsForm();

            Optional<String> commit = Optional
                    .fromNullable(options.getFirstValue("commitId", null));

            Preconditions.checkState(commit.isPresent(), "No commit specified.");

            GeoGIT ggit = getGeogit(request).get();

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
