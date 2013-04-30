/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.repo;

import java.io.IOException;
import java.io.Writer;

import org.geogit.web.api.commands.PushManager;
import org.restlet.data.ClientInfo;
import org.restlet.data.Form;
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
            Form options = getRequest().getResourceRef().getQueryAsForm();

            // make a combined ip address to handle requests from multiple machines in the same
            // external network.
            // e.g.: ext.ern.al.IP.int.ern.al.IP
            String ipAddress = info.getAddress() + "." + options.getFirstValue("internalIp", "");
            PushManager pushManager = PushManager.get();
            pushManager.connectionBegin(ipAddress);
            w.write("Push began for address: " + ipAddress);
            w.flush();
        }
    }
}
