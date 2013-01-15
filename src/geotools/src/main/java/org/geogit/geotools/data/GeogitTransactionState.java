/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.data;

import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentState;

/**
 *
 */
public class GeogitTransactionState extends ContentState {

    /**
     * @param entry
     */
    public GeogitTransactionState(ContentEntry entry) {
        super(entry);
    }

}
