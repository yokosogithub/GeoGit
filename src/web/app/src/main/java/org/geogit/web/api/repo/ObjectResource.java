/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.repo;

import java.io.IOException;
import java.io.OutputStream;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.datastream.DataStreamSerializationFactory;
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

    private static class ObjectRepresentation extends OutputRepresentation {
        private final ObjectId oid;

        private static final ObjectSerializingFactory serialFac = new DataStreamSerializationFactory();

        private final GeoGIT ggit;

        public ObjectRepresentation(ObjectId oid, GeoGIT ggit) {
            super(MediaType.APPLICATION_OCTET_STREAM);
            this.oid = oid;
            this.ggit = ggit;
        }

        @Override
        public void write(OutputStream out) throws IOException {
            RevObject rawObject = ggit.getRepository().getObjectDatabase().get(oid);
            serialFac.createObjectWriter(rawObject.getType()).write(rawObject, out);
        }
    }
}
