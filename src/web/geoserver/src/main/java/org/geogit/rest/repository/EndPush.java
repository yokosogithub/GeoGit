/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.rest.repository;

import static org.geogit.rest.repository.GeogitResourceUtils.getGeogit;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.web.api.commands.PushManager;
import org.restlet.Context;
import org.restlet.data.ClientInfo;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 *
 */
public class EndPush extends Resource {

    @Override
    public void init(Context context, Request request, Response response) {
        super.init(context, request, response);
        List<Variant> variants = getVariants();
        variants.add(new EndPushRepresentation());
    }

    private class EndPushRepresentation extends WriterRepresentation {
        public EndPushRepresentation() {
            super(MediaType.TEXT_PLAIN);
        }

        @Override
        public void write(Writer w) throws IOException {
            ClientInfo info = getRequest().getClientInfo();
            Request request = getRequest();
            Optional<GeoGIT> ggit = getGeogit(request);
            Preconditions.checkState(ggit.isPresent());
            PushManager pushManager = PushManager.get();
            pushManager.connectionSucceeded(ggit.get(), info.getAddress());
            w.write("Push succeeded for address: " + info.getAddress());
            w.flush();
        }
    }

}
