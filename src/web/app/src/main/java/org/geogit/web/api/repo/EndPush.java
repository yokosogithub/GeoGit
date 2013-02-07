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
            Form options = getRequest().getResourceRef().getQueryAsForm();

            // make a combined ip address to handle requests from multiple machines in the same
            // external network.
            // e.g.: ext.ern.al.IP.int.ern.al.IP
            String ipAddress = info.getAddress() + "." + options.getFirstValue("internalIp", "");

            String refspec = options.getFirstValue("refspec", null);
            ObjectId oid = ObjectId.valueOf(options.getFirstValue("objectId",
                    ObjectId.NULL.toString()));

            GeoGIT ggit = (GeoGIT) getApplication().getContext().getAttributes().get("geogit");
            PushManager pushManager = PushManager.get();
            pushManager.connectionSucceeded(ggit, ipAddress, refspec, oid);
            w.write("Push succeeded for address: " + ipAddress);
            w.flush();
        }
    }
}
