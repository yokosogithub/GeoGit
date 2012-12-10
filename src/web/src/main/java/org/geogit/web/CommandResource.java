package org.geogit.web;

import org.geogit.web.api.CommandSpecException;
import java.io.IOException;
import java.io.Writer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.geogit.api.GeoGIT;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.ResponseWriter;
import org.geogit.web.api.WebAPICommand;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.representation.WriterRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

/**
 *
 */
public class CommandResource extends ServerResource {

    @Post("json|xml")
    public Representation runCommand(Variant variant) {
        Representation rep = null;
        WebAPICommand command = null;
        try {
            command = CommandBuilder.build(getRequest());
            assert command != null;
        } catch (CommandSpecException ex) {
            rep = formatException(ex, variant);
        }
        try {
            if (command != null) {
                GeoGIT geogit = (GeoGIT) getApplication().getContext().getAttributes().get("geogit");
                RestletContext ctx = new RestletContext(geogit);
                command.run(ctx);
                rep = ctx.getRepresentation(variant, getJSONPCallback());
            }
        } catch (CommandSpecException ex) {
            rep = formatException(ex, variant);
        } catch (Exception ex) {
            rep = formatUnexpectedException(ex, variant);
        }
        return rep;
    }


    private Representation formatException(CommandSpecException ex, Variant variant) {
        Logger logger = getLogger();
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "CommandSpecException", ex);
        }
        return new JettisonRepresentation(variant.getMediaType(),
                CommandResponse.error(ex.getMessage()), getJSONPCallback());
    }

    private Representation formatUnexpectedException(Exception ex, Variant variant) {
        Logger logger = getLogger();
        UUID uuid = UUID.randomUUID();
        logger.log(Level.SEVERE, "Unexpected exception : " + uuid, ex);
        return new JettisonRepresentation(variant.getMediaType(),
                CommandResponse.error("Unexpected exception : " + uuid), getJSONPCallback());
    }

    private String getJSONPCallback() {
        Form form = getRequest().getResourceRef().getQueryAsForm();
        return form.getFirstValue("callback", null);
    }

    static class RestletContext implements CommandContext {

        CommandResponse responseContent;
        final GeoGIT geogit;

        RestletContext(GeoGIT geogit) {
            this.geogit = geogit;
        }

        @Override
        public GeoGIT getGeoGIT() {
            return geogit;
        }

        Representation getRepresentation(Variant variant, String callback) {
            return new JettisonRepresentation(variant.getMediaType(), responseContent, callback);
        }

        @Override
        public void setResponseContent(CommandResponse responseContent) {
            this.responseContent = responseContent;
        }
    }

    static class JettisonRepresentation extends WriterRepresentation {

        final CommandResponse impl;
        String callback;

        public JettisonRepresentation(MediaType mediaType, CommandResponse impl, String callback) {
            super(mediaType);
            this.impl = impl;
            this.callback = callback;
        }

        private XMLStreamWriter createWriter(Writer writer) {
            final MediaType mediaType = getMediaType();
            XMLStreamWriter xml;
            if (mediaType.getSubType().equalsIgnoreCase("xml")) {
                try {
                    xml = XMLOutputFactory.newFactory().createXMLStreamWriter(writer);
                } catch (XMLStreamException ex) {
                    throw new RuntimeException(ex);
                }
                callback = null; // this doesn't make sense
            } else if (mediaType == MediaType.APPLICATION_JSON) {
                xml = new MappedXMLStreamWriter(new MappedNamespaceConvention(), writer);
            } else {
                throw new RuntimeException("mediatype not handled " + mediaType);
            }
            return xml;
        }

        @Override
        public void write(Writer writer) throws IOException {
            XMLStreamWriter stax = null;
            if (callback != null) {
                writer.write(callback);
                writer.write('(');
            }
            try {
                stax = createWriter(writer);
                impl.write(new ResponseWriter(stax));
                stax.flush();
                stax.close();
            } catch (Exception ex) {
                throw new IOException(ex);
            }
            if (callback != null) {
                writer.write(");");
            }
        }
    }
}
