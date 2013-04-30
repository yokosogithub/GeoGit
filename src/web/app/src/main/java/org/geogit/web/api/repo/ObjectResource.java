/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.repo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;
import org.restlet.resource.Finder;
import org.restlet.resource.ServerResource;

public class ObjectResource extends Finder {
    @Override
    public ServerResource find(Request request, Response response) {
        if (request.getAttributes().containsKey("id")) {
            final GeoGIT ggit = (GeoGIT) getApplication().getContext().getAttributes()
                    .get("geogit");
            final String id = (String) request.getAttributes().get("id");
            final ObjectId oid = ObjectId.valueOf(id);
            if (ggit.getRepository().blobExists(oid)) {
                return new ServerResource() {
                    {
                        getVariants().add(new ObjectRepresentation(oid, ggit));
                    }
                };
            }
        }
        return super.find(request, response);
    }

    private class ObjectRepresentation extends OutputRepresentation {
        private final ObjectId oid;

        private final GeoGIT ggit;

        public ObjectRepresentation(ObjectId oid, GeoGIT ggit) {
            super(MediaType.APPLICATION_OCTET_STREAM);
            this.oid = oid;
            this.ggit = ggit;
        }

        @Override
        public void write(OutputStream out) throws IOException {
            InputStream rawObject = ggit.getRepository().getRawObject(oid);
            try {
                byte[] buff = new byte[8192];
                int len = 0;
                while ((len = rawObject.read(buff)) >= 0) {
                    out.write(buff, 0, len);
                }
            } finally {
                rawObject.close();
            }
        }
    }
}
