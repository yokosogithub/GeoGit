package org.geogit.web.api.repo;

import java.io.IOException;
import java.io.InputStream;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.restlet.data.ClientInfo;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class SendObjectResource extends ServerResource {

    @Post
    public Representation acceptObject(Representation entity) throws IOException {
        Representation result = null;

        ClientInfo info = entity.createClientInfo();

        InputStream input = entity.getStream();
        byte objectIdBytes[] = new byte[20];
        input.read(objectIdBytes, 0, 20);
        ObjectId objectId = new ObjectId(objectIdBytes);

        final GeoGIT ggit = (GeoGIT) getApplication().getContext().getAttributes().get("geogit");
        PushManager pushManager = (PushManager) getApplication().getContext().getAttributes()
                .get("pushmanager");
        if (ggit.getRepository().getObjectDatabase().exists(objectId)) {
            result = new StringRepresentation("Object already existed: " + objectId.toString());

        } else {
            // put it into the staging database until we have all of the data
            ggit.getRepository().getIndex().getDatabase().put(objectId, input);
            pushManager.addObject(info.getAddress(), objectId);
            result = new StringRepresentation("Object added: " + objectId.toString());
        }

        return result;
    }
}
