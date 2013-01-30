package org.geogit.api.porcelain;

/**
 * A common class for exceptions thrown from GeoGit operations and classes
 */
public abstract class GeoGitOpException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GeoGitOpException(String msg) {
        super(msg);
    }

}
