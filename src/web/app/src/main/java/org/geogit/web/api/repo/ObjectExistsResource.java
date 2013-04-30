/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.repo;

import java.io.IOException;
import java.io.Writer;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.web.api.commands.PushManager;
import org.restlet.data.ClientInfo;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.WriterRepresentation;
import org.restlet.resource.ServerResource;

public class ObjectExistsResource extends ServerResource {
    {
        getVariants().add(new ObjectExistsRepresentation());
    }

    private class ObjectExistsRepresentation extends WriterRepresentation {
        public ObjectExistsRepresentation() {
            super(MediaType.TEXT_PLAIN);
        }

        @Override
        public void write(Writer w) throws IOException {
            Form options = getRequest().getResourceRef().getQueryAsForm();

            ObjectId oid = ObjectId.valueOf(options.getFirstValue("oid", ObjectId.NULL.toString()));

            GeoGIT ggit = (GeoGIT) getApplication().getContext().getAttributes().get("geogit");
            PushManager pushManager = PushManager.get();
            ClientInfo info = getRequest().getClientInfo();
            // make a combined ip address to handle requests from multiple machines in the same
            // external network.
            // e.g.: ext.ern.al.IP.int.ern.al.IP
            String ipAddress = info.getAddress() + "." + options.getFirstValue("internalIp", "");
            if (ggit.getRepository().blobExists(oid) || pushManager.alreadyPushed(ipAddress, oid)) {
                w.write("1");
            } else {
                w.write("0");
            }
            w.flush();
        }
    }
}
