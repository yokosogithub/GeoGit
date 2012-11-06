package org.geogit.api.porcelain;

/**
 * Exception thrown by ConfigDatabase that contains the error status code.
 * 
 * @author mfawcett
 */
@SuppressWarnings("serial")
public class ConfigException extends RuntimeException {
    /**
     * Possible status codes for Config exceptions.
     */
    public enum StatusCode {
        INVALID_LOCATION, CANNOT_WRITE, SECTION_OR_NAME_NOT_PROVIDED, SECTION_OR_KEY_INVALID, OPTION_DOES_N0T_EXIST, MULTIPLE_OPTIONS_MATCH, INVALID_REGEXP, USERHOME_NOT_SET, TOO_MANY_ACTIONS, MISSING_SECTION, TOO_MANY_ARGS
    }

    public StatusCode statusCode;

    /**
     * Constructs a new {@code ConfigException} with the given status code.
     * 
     * @param statusCode the status code for this exception
     */
    public ConfigException(StatusCode statusCode) {
        this(null, statusCode);
    }

    /**
     * Construct a new exception with the given cause and status code.
     * 
     * @param e the cause of this exception
     * @param statusCode the status code for this exception
     */
    public ConfigException(Exception e, StatusCode statusCode) {
        super(e);
        this.statusCode = statusCode;
    }
}
