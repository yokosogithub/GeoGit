package org.geogit.web.api.repo;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.geogit.api.GeoGIT;
import org.restlet.data.MediaType;
import org.restlet.representation.WriterRepresentation;
import org.restlet.resource.ServerResource;

public class ManifestResource extends ServerResource {
    {
        getVariants().add(new ManifestRepresentation());
    }
    
    private class ManifestRepresentation extends WriterRepresentation {
        public ManifestRepresentation() {
            super(MediaType.TEXT_PLAIN);
        }

        @Override
        public void write(Writer w) throws IOException {
            GeoGIT ggit = (GeoGIT) getApplication().getContext().getAttributes().get("geogit");
            Map<String, String> refs = ggit.getRepository().getRefDatabase().getAll();
            for (Map.Entry<String, String> entry : refs.entrySet()) {
                w.write(entry.getKey());
                w.write(" ");
                w.write(entry.getValue());
                w.write("\n");
            }
            w.flush();
        }
    }
}
