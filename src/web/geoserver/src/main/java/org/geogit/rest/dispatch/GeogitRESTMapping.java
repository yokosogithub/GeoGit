/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.rest.dispatch;

import java.util.Map;

/**
 * This is the extension point for REST modules to register themselves with Geoserver. The mapping
 * should have path specifications compatible with the REST Router class for keys, and Restlets for
 * values.
 * 
 * @author David Winslow <dwinslow@openplans.org>
 */
public class GeogitRESTMapping {
    private Map<String, Object> routes;

    public void setRoutes(Map<String, Object> m) {
        // TODO: Check this and throw an error for bad data
        routes = m;
    }

    public Map<String, Object> getRoutes() {
        return routes;
    }
}
