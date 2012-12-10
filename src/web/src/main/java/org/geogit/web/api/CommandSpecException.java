package org.geogit.web.api;

/**
 * A user-input (or lack thereof) driven exception. Purposefully does not have
 * a constructor to allow a Throwable cause to be specified.
 */
public class CommandSpecException extends RuntimeException {

    public CommandSpecException(String message) {
        super(message);
    }

}
