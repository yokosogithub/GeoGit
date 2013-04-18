package org.geogit.web.api;

import org.geogit.api.GeoGIT;

/**
 *
 */
public interface CommandContext {

    /**
     * @return the {@link GeoGIT} for this context.
     */
    GeoGIT getGeoGIT();

    /**
     * Sets the response for the context.
     * 
     * @param responseContent the command response
     */
    void setResponseContent(CommandResponse responseContent);

}
