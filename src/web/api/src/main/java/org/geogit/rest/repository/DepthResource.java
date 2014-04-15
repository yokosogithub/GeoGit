/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.rest.repository;

import static org.geogit.rest.repository.RESTUtils.getGeogit;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
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
 *
 */
public class DepthResource extends Resource {

    @Override
    public void init(Context context, Request request, Response response) {
        super.init(context, request, response);
        List<Variant> variants = getVariants();
        variants.add(new DepthRepresentation(request));
    }

    private static class DepthRepresentation extends OutputRepresentation {

        private Request request;

        public DepthRepresentation(Request request) {
            super(MediaType.TEXT_PLAIN);
            this.request = request;
        }

        @Override
        public void write(OutputStream out) throws IOException {
            PrintWriter w = new PrintWriter(out);
            Form options = request.getResourceRef().getQueryAsForm();

            Optional<String> commit = Optional
                    .fromNullable(options.getFirstValue("commitId", null));

            Optional<GeoGIT> geogit = getGeogit(request);
            Preconditions.checkState(geogit.isPresent());
            GeoGIT ggit = geogit.get();

            Optional<Integer> depth = Optional.absent();

            if (commit.isPresent()) {
                depth = Optional.of(ggit.getRepository().getGraphDatabase()
                        .getDepth(ObjectId.valueOf(commit.get())));
            } else {
                depth = ggit.getRepository().getDepth();
            }

            if (depth.isPresent()) {
                w.write(depth.get().toString());
            }
            w.flush();

        }

    }
}
