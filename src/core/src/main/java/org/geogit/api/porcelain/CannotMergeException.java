package org.geogit.api.porcelain;

public class CannotMergeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CannotMergeException(String msg) {
        super(msg);
    }

}
