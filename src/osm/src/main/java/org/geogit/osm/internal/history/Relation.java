/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.internal.history;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 *
 */
public class Relation extends Primitive {

    public static class Member {
        private String type;

        private long ref;

        private String role;

        public Member(String type, long ref, @Nullable String role) {
            this.type = type;
            this.ref = ref;
            this.role = role;
        }

        public String getType() {
            return type;
        }

        public long getRef() {
            return ref;
        }

        public Optional<String> getRole() {
            return Optional.fromNullable(role);
        }

        @Override
        public String toString() {
            return new StringBuilder("[type:").append(type).append(",ref:").append(ref)
                    .append(",role:").append(role).append(']').toString();
        }
    }

    private List<Member> members;

    public Relation() {
        super();
        members = Lists.newLinkedList();
    }

    public ImmutableList<Member> getMembers() {
        return ImmutableList.copyOf(members);
    }

    /**
     * @param member
     */
    void addMember(Member member) {
        members.add(member);
    }

    @Override
    public String toString() {
        return new StringBuilder(super.toString()).append(",members:").append(members).append(']')
                .toString();
    }

}
