package org.geogit.web.api.repo;

import java.io.IOException;
import java.io.Writer;

import org.geogit.web.api.commands.PushManager;
import org.restlet.data.ClientInfo;
import org.restlet.data.MediaType;
import org.restlet.representation.WriterRepresentation;
import org.restlet.resource.ServerResource;

public class BeginPush extends ServerResource {
    {
        getVariants().add(new BeginPushRepresentation());
    }

    private class BeginPushRepresentation extends WriterRepresentation {
        public BeginPushRepresentation() {
            super(MediaType.TEXT_PLAIN);
        }

        @Override
        public void write(Writer w) throws IOException {
            ClientInfo info = getRequest().getClientInfo();
            PushManager pushManager = PushManager.get();
            pushManager.connectionBegin(info.getAddress());
            w.write("Push began for address: " + info.getAddress());
            w.flush();
        }
    }
}
