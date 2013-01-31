package org.geogit.web.api.repo;

import java.io.IOException;
import java.io.Writer;

import org.geogit.api.GeoGIT;
import org.geogit.web.api.commands.PushManager;
import org.restlet.data.ClientInfo;
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
            GeoGIT ggit = (GeoGIT) getApplication().getContext().getAttributes().get("geogit");
            PushManager pushManager = PushManager.get();
            pushManager.connectionSucceeded(ggit, info.getAddress());
            w.write("Push succeeded for address: " + info.getAddress());
            w.flush();
        }
    }
}
