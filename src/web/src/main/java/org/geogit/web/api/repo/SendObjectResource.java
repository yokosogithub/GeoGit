package org.geogit.web.api.repo;

import java.io.IOException;
import java.io.InputStream;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class SendObjectResource extends ServerResource {

    @Post
    public Representation acceptObject(Representation entity) throws IOException {
        Representation result = null;

        InputStream input = entity.getStream();
        byte objectIdBytes[] = new byte[20];
        input.read(objectIdBytes, 0, 20);
        ObjectId objectId = new ObjectId(objectIdBytes);

        final GeoGIT ggit = (GeoGIT) getApplication().getContext().getAttributes().get("geogit");
        if (ggit.getRepository().getObjectDatabase().exists(objectId)) {
            result = new StringRepresentation("Object already existed: " + objectId.toString());

        } else {
            ggit.getRepository().getObjectDatabase().put(objectId, input);
            result = new StringRepresentation("Object added: " + objectId.toString());
        }

        return result;
    }
}
