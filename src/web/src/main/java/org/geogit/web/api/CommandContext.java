package org.geogit.web.api;

import org.geogit.api.GeoGIT;

/**
 *
 */
public interface CommandContext {

    GeoGIT getGeoGIT();

    void setResponseContent(CommandResponse responseContent);

}
