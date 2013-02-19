package org.geogit.api.porcelain;

import org.geogit.api.Ref;

public class PullResult {

    private Ref oldRef = null;

    private Ref newRef = null;

    private String remoteName = null;

    private FetchResult fetchResult = null;

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }

    public FetchResult getFetchResult() {
        return fetchResult;
    }

    public void setFetchResult(FetchResult fetchResult) {
        this.fetchResult = fetchResult;
    }

    public Ref getOldRef() {
        return oldRef;
    }

    public void setOldRef(Ref oldRef) {
        this.oldRef = oldRef;
    }

    public Ref getNewRef() {
        return newRef;
    }

    public void setNewRef(Ref newRef) {
        this.newRef = newRef;
    }
}
