/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.repo;

import java.io.IOException;
import java.io.Writer;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.plumbing.RefParse;
import org.geogit.web.api.commands.PushManager;
import org.restlet.data.ClientInfo;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.WriterRepresentation;
import org.restlet.resource.ServerResource;

import com.google.common.base.Optional;

public class EndPush extends ServerResource {
    {
        getVariants().add(new EndPushRepresentation());
    }

    private class EndPushRepresentation extends WriterRepresentation {
        public EndPushRepresentation() {
            super(MediaType.TEXT_PLAIN);
        }

        @Override
        public void write(Writer w) throws IOException {
            ClientInfo info = getRequest().getClientInfo();
            GeoGIT ggit = (GeoGIT) getApplication().getContext().getAttributes().get("geogit");
            Form options = getRequest().getResourceRef().getQueryAsForm();

            // make a combined ip address to handle requests from multiple machines in the same
            // external network.
            // e.g.: ext.ern.al.IP.int.ern.al.IP
            String ipAddress = info.getAddress() + "." + options.getFirstValue("internalIp", "");

            String refspec = options.getFirstValue("refspec", null);
            ObjectId oid = ObjectId.valueOf(options.getFirstValue("objectId",
                    ObjectId.NULL.toString()));
            ObjectId originalRefValue = ObjectId.valueOf(options.getFirstValue("originalRefValue",
                    ObjectId.NULL.toString()));

            Optional<Ref> currentRef = ggit.command(RefParse.class).setName(refspec).call();

            if (currentRef.isPresent() && !currentRef.get().getObjectId().equals(ObjectId.NULL)
                    && !currentRef.get().getObjectId().equals(originalRefValue)) {
                // Abort push
                w.write("Push aborted for address: " + ipAddress
                        + ". The ref was changed during push.");
                w.flush();
            } else {
                PushManager pushManager = PushManager.get();
                pushManager.connectionSucceeded(ggit, ipAddress, refspec, oid);
                w.write("Push succeeded for address: " + ipAddress);
                w.flush();
            }
        }
    }
}
