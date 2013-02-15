package org.geogit.api.porcelain;

import java.util.List;

import org.geogit.api.Ref;

import com.google.common.collect.Lists;

public class PullResult {

    private List<Ref> changedRefs = Lists.newArrayList();

    private List<Ref> newRefs = Lists.newArrayList();

    private String remoteName = null;

    public void addChangedRef(Ref changedRef) {
        changedRefs.add(changedRef);
    }

    public List<Ref> getChangedRefs() {
        return changedRefs;
    }

    public void addNewRef(Ref newRef) {
        newRefs.add(newRef);
    }

    public List<Ref> getNewRefs() {
        return newRefs;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }
}
