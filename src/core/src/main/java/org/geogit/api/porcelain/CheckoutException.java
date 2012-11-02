package org.geogit.api.porcelain;

/**
 * Exception thrown by the {@link CheckoutOp checkout} op.
 * <p>
 * 
 * @TODO: define and codify the possible causes for a checkout to fail
 */
@SuppressWarnings("serial")
public class CheckoutException extends RuntimeException {

    public CheckoutException(String msg) {
        this(msg, null);
    }

    public CheckoutException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
