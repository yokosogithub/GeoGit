/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.internal;

/**
 * A class to store the values that define the result of a download operation
 * 
 */
public class OSMDownloadReport {

    private long unpprocessedCount;

    private long count;

    private long latestChangeset;

    private long latestTimestamp;

    private long nodeCount;

    private long wayCount;

    public OSMDownloadReport(long count, long nodeCount, long wayCount, long unprocessedCount,
            long latestChangeset, long latestTimestamp) {
        this.count = count;
        this.nodeCount = nodeCount;
        this.wayCount = wayCount;
        this.unpprocessedCount = unprocessedCount;
        this.latestChangeset = latestChangeset;
        this.latestTimestamp = latestTimestamp;
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

    public long getNodeCount() {
        return nodeCount;
    }

    public long getWayCount() {
        return wayCount;
    }

    /**
     * The id of the most recent changeset downloaded
     * 
     * @return
     */
    public long getLatestChangeset() {
        return latestChangeset;
    }

    /**
     * the timestamp of the most recent changeset downloaded
     * 
     * @return
     */
    public long getLatestTimestamp() {
        return latestTimestamp;
    }

}
