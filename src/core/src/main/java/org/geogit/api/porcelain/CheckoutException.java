package org.geogit.api.porcelain;

/**
 * Exception thrown by the {@link CheckoutOp checkout} op.
 * <p>
 * 
 * @TODO: define and codify the possible causes for a checkout to fail
 */
@SuppressWarnings("serial")
public class CheckoutException extends RuntimeException {

    public enum StatusCode {
        LOCAL_CHANGES_NOT_COMMITTED
    }

    public StatusCode statusCode;

    public CheckoutException(StatusCode statusCode) {
        this(null, statusCode);

    }

    public CheckoutException(Exception e, StatusCode statusCode) {
        super(e);
        this.statusCode = statusCode;
    }
}
