/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.Ref;
import org.geogit.api.RevPerson;
import org.geogit.api.RevTag;
import org.geogit.api.plumbing.HashObject;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.storage.ObjectDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Creates a new tag
 * 
 */
public class TagCreateOp extends AbstractGeoGitOp<RevTag> {

    private final ObjectDatabase objectDb;

    private String name;

    private ObjectId commitId;

    private String message;

    private Platform platform;

    /**
     * Constructs a new {@code TagCreateOp} with the given parameters.
     * 
     * @param platform the current platform
     */
    @Inject
    public TagCreateOp(final ObjectDatabase objectDb, final Platform platform) {
        this.objectDb = objectDb;
        this.platform = platform;
    }

    /**
     * Executes the tag creation operation.
     * 
     * @return the created tag
     * 
     */
    public RevTag call() throws RuntimeException {
        checkState(name != null, "tag name was not provided");
        final String tagRefPath = Ref.TAGS_PREFIX + name;
        checkArgument(!command(RefParse.class).setName(tagRefPath).call().isPresent(),
                "A tag named '" + name + "' already exists.");
        if (commitId == null) {
            final Optional<Ref> currHead = command(RefParse.class).setName(Ref.HEAD).call();
            Preconditions.checkState(currHead.isPresent(),
                    "Repository has no HEAD, can't create tag");
            commitId = currHead.get().getObjectId();
        }
        RevPerson tagger = resolveTagger();
        message = message == null ? "" : message;
        RevTag tag = new RevTag(ObjectId.NULL, name, commitId, message, tagger);
        ObjectId id = command(HashObject.class).setObject(tag).call();
        tag = new RevTag(id, name, commitId, message, tagger);
        objectDb.put(tag);
        Optional<Ref> branchRef = command(UpdateRef.class).setName(tagRefPath)
                .setNewValue(tag.getId()).call();
        checkState(branchRef.isPresent());

        return tag;
    }

    public TagCreateOp setMessage(String message) {
        this.message = message;
        return this;
    }

    public TagCreateOp setCommitId(ObjectId commitId) {
        this.commitId = commitId;
        return this;
    }

    public TagCreateOp setName(String name) {
        this.name = name;
        return this;
    }

    private RevPerson resolveTagger() {
        final String nameKey = "user.name";
        final String emailKey = "user.email";

        Optional<String> name = command(ConfigGet.class).setName(nameKey).call();
        Optional<String> email = command(ConfigGet.class).setName(emailKey).call();

        checkState(
                name.isPresent(),
                "%s not found in config. Use geogit config [--global] %s <your name> to configure it.",
                nameKey, nameKey);

        checkState(
                email.isPresent(),
                "%s not found in config. Use geogit config [--global] %s <your email> to configure it.",
                emailKey, emailKey);

        String taggerName = name.get();
        String taggerEmail = email.get();

        long taggerTimeStamp = platform.currentTimeMillis();
        int taggerTimeZoneOffset = platform.timeZoneOffset(taggerTimeStamp);
        return new RevPerson(taggerName, taggerEmail, taggerTimeStamp, taggerTimeZoneOffset);
    }

}
