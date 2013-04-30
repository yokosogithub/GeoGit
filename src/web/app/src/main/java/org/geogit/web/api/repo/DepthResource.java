/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.repo;

import java.io.IOException;
import java.io.Writer;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.WriterRepresentation;
import org.restlet.resource.ServerResource;

import com.google.common.base.Optional;

public class DepthResource extends ServerResource {
    {
        getVariants().add(new DepthRepresentation());
    }

    private class DepthRepresentation extends WriterRepresentation {
        public DepthRepresentation() {
            super(MediaType.TEXT_PLAIN);
        }

        @Override
        public void write(Writer w) throws IOException {
            Form options = getRequest().getResourceRef().getQueryAsForm();

            Optional<String> commit = Optional
                    .fromNullable(options.getFirstValue("commitId", null));

            GeoGIT ggit = (GeoGIT) getApplication().getContext().getAttributes().get("geogit");

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
