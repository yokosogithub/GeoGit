package org.geogit.api.porcelain;

/**
 * @author jgarrett
 * 
 *         Exception thrown by remote commands.
 */
@SuppressWarnings("serial")
public class RemoteException extends RuntimeException {
    public enum StatusCode {
        REMOTE_NOT_FOUND, MISSING_NAME, MISSING_URL, REMOTE_ALREADY_EXISTS
    }

    public StatusCode statusCode;

    public RemoteException(StatusCode statusCode) {
        this(null, statusCode);
    }

    public RemoteException(Exception e, StatusCode statusCode) {
        super(e);
        this.statusCode = statusCode;
    }
}
