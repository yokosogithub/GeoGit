package org.geogit.api.porcelain;

/**
 * Exception thrown by the push process.
 * 
 */
@SuppressWarnings("serial")
public class PushException extends RuntimeException {
    /**
     * Possible status codes for Push exceptions.
     */
    public enum StatusCode {
        NOTHING_TO_PUSH, REMOTE_HAS_CHANGES
    }

    public StatusCode statusCode;

    /**
     * Constructs a new {@code PushException} with the given status code.
     * 
     * @param statusCode the status code for this exception
     */
    public PushException(StatusCode statusCode) {
        this(null, statusCode);
    }

    /**
     * Construct a new exception with the given cause and status code.
     * 
     * @param e the cause of this exception
     * @param statusCode the status code for this exception
     */
    public PushException(Exception e, StatusCode statusCode) {
        super(e);
        this.statusCode = statusCode;
    }
}
