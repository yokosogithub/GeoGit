package org.geogit.api.porcelain;

import org.geogit.api.RevCommit;

import com.google.common.base.Optional;

public class ValueAndCommit {

    public Optional<?> value;

    public RevCommit commit;

    public ValueAndCommit(Optional<?> value, RevCommit commit) {
        this.value = value;
        this.commit = commit;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(value.orNull()).append('/').append(commit.getId())
                .toString();
    }
}
