/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

/**
 * An annotated tag.
 * 
 * @author groldan
 * 
 */
public class RevTag extends AbstractRevObject {

    private String name;

    private ObjectId commit;

    /**
     * Constructs a new {@code RevTag} with the given {@link ObjectId}, name, and commit id.
     * 
     * @param id the {@code ObjectId} to use for this tag
     * @param name the name of the tag
     * @param commitId the {@code ObjectId} of the commit that this tag points to
     */
    public RevTag(final ObjectId id, final String name, final ObjectId commitId) {
        super(id);
    }

    @Override
    public TYPE getType() {
        return TYPE.TAG;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the {@code ObjectId} of the commit that this tag points to
     */
    public ObjectId getCommitId() {
        return commit;
    }
}
