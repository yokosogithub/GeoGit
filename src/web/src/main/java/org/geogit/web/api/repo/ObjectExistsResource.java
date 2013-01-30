package org.geogit.web.api.repo;

import java.io.IOException;
import java.io.Writer;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
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
            ClientInfo info = getRequest().getClientInfo();
            if (ggit.getRepository().blobExists(oid)
                    || PushManager.get().alreadyPushed(info.getAddress(), oid)) {
                w.write("1");
            } else {
                w.write("0");
            }
            w.flush();
        }
    }
}
