/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.internal.history;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 *
 */
public class Primitive {

    private long id;

    private long changesetId;

    private boolean visible;

    private long timestamp;

    private int version;

    private String userName;

    private long userId;

    private Map<String, String> tags;

    public Primitive() {
        tags = Maps.newHashMap();
    }

    public long getId() {
        return id;
    }

    public long getChangesetId() {
        return changesetId;
    }

    public boolean isVisible() {
        return visible;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getVersion() {
        return version;
    }

    public String getUserName() {
        return userName;
    }

    public long getUserId() {
        return userId;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    void setId(long id) {
        this.id = id;
    }

    void setChangesetId(long changesetId) {
        this.changesetId = changesetId;
    }

    void setVisible(boolean visible) {
        this.visible = visible;
    }

    void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    void setVersion(int version) {
        this.version = version;
    }

    void setUserName(String userName) {
        this.userName = userName;
    }

    void setUserId(long userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append("[id:").append(getId())
                .append(",changeset:").append(changesetId).append(",user:").append(userName)
                .append(",uid:").append(userId).append(",ts:").append(timestamp)
                .append(",version:").append(version).append(",visible:").append(visible).toString();
    }

}
