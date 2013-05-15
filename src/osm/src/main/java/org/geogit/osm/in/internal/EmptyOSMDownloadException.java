/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.in.internal;

/**
 * An exception to indicate that no valid data was downloaded from an OSM server. It reports the
 * number of feature the data contained, and the number of feature among them that were not complete
 * or had error (like, for instance, ways without nodes)
 * 
 */
public class EmptyOSMDownloadException extends RuntimeException {

    private long unpprocessedCount;

    private long count;

    public EmptyOSMDownloadException(long count, long unprocessedCount) {
        this.count = count;
        this.unpprocessedCount = unprocessedCount;
    }

    /**
     * The number of features downloaded that could not be processeed and imported
     * 
     * @return
     */
    public long getUnpprocessedCount() {
        return unpprocessedCount;
    }

    /**
     * The total number of downloaded features
     * 
     * @return
     */
    public long getCount() {
        return count;
    }

    private static final long serialVersionUID = 1L;

}
