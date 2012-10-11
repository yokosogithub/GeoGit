/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.regex.Pattern;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * Resolves the reference given by a ref spec to the {@link ObjectId} it finally points to,
 * dereferencing symbolic refs as necessary.
 */
public class RevParse extends AbstractGeoGitOp<Optional<ObjectId>> {

    private String refSpec;

    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-f]+$");

    private StagingDatabase indexDb;

    @Inject
    public RevParse(StagingDatabase indexDb) {
        this.indexDb = indexDb;
    }

    public RevParse setRefSpec(final String refSpec) {
        this.refSpec = refSpec;
        return this;
    }

    /**
     * Parses a geogit revision string and return an object id.
     * <p>
     * Combinations of these operators are supported:
     * <ul>
     * <li><b>HEAD</b>, <b>MERGE_HEAD</b>, <b>FETCH_HEAD</b></li>
     * <li><b>SHA-1</b>: a complete or abbreviated SHA-1</li>
     * <li><b>refs/...</b>: a complete reference name</li>
     * <li><b>short-name</b>: a short reference name under {@code refs/heads}, {@code refs/tags}, or
     * {@code refs/remotes} namespace</li>
     * <li><b>tag-NN-gABBREV</b>: output from describe, parsed by treating {@code ABBREV} as an
     * abbreviated SHA-1.</li>
     * <li><i>id</i><b>^</b>: first parent of commit <i>id</i>, this is the same as {@code id^1}</li>
     * <li><i>id</i><b>^0</b>: ensure <i>id</i> is a commit</li>
     * <li><i>id</i><b>^n</b>: n-th parent of commit <i>id</i></li>
     * <li><i>id</i><b>~n</b>: n-th historical ancestor of <i>id</i>, by first parent. {@code id~3}
     * is equivalent to {@code id^1^1^1} or {@code id^^^}.</li>
     * <li><i>id</i><b>:path</b>: Lookup path under tree named by <i>id</i></li>
     * <li><i>id</i><b>^{commit}</b>: ensure <i>id</i> is a commit</li>
     * <li><i>id</i><b>^{tree}</b>: ensure <i>id</i> is a tree</li>
     * <li><i>id</i><b>^{tag}</b>: ensure <i>id</i> is a tag</li>
     * <li><i>id</i><b>^{blob}</b>: ensure <i>id</i> is a blob</li>
     * </ul>
     * 
     * <p>
     * The following operators are specified by Git conventions, but are not supported by this
     * method:
     * <ul>
     * <li><b>ref@{n}</b>: n-th version of ref as given by its reflog</li>
     * <li><b>ref@{time}</b>: value of ref at the designated time</li>
     * </ul>
     * 
     * @param revstr A geogit object references expression
     * @throws IllegalArgumentException if the ref spec doesn't resolve to any object in the
     *         respository
     * @return the resolved object id, may be {@link Optional#absent() absent}
     */
    @Override
    public Optional<ObjectId> call() {
        checkState(refSpec != null);
        ObjectId resolvedTo = null;
        // TODO: handle other kinds of ref specs

        // is it a ref?
        Optional<Ref> ref = command(RefParse.class).setName(refSpec).call();
        if (ref.isPresent()) {
            resolvedTo = ref.get().getObjectId();
        } else {
            // does it look like an object id hash?
            boolean hexPatternMatches = HEX_PATTERN.matcher(refSpec).matches();
            if (hexPatternMatches) {
                try {
                    if (ObjectId.valueOf(refSpec).isNull()) {
                        return Optional.of(ObjectId.NULL);
                    }
                } catch (IllegalArgumentException ignore) {
                    // its a partial id
                }
                List<ObjectId> hashMatches = indexDb.lookUp(refSpec);
                if (hashMatches.size() > 1) {
                    throw new IllegalArgumentException(String.format(
                            "Ref spec (%s) matches more than one object id: %s", refSpec,
                            hashMatches.toString()));
                }
                if (hashMatches.size() == 1) {
                    resolvedTo = hashMatches.get(0);
                }
            }
        }
        return Optional.fromNullable(resolvedTo);
    }
}
