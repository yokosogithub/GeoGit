package org.geogit.api.porcelain;

import org.geogit.api.plumbing.diff.Patch;

/**
 * This exception indicate that a given patch is outdated and does not correspond to the current
 * state of the working tree, so it cannot be applied.
 * 
 */
public class CannotApplyPatchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * A patch with the conflicting changes of the patch that caused the exception
     * 
     */
    private Patch patch;

    public CannotApplyPatchException(Patch patch) {
        this.patch = patch;
    }

    public Patch getPatch() {
        return patch;
    }

}
