/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.rest.repository;

import org.restlet.resource.Resource;

/**
 *
 */
public class EndPush extends Resource {
    
    @Override
    public boolean allowGet() {
        return false;
    }

}
