package org.geogit.api.porcelain;

/**
 * @author mfawcett
 * 
 *         Exception thrown by ConfigDatabase that contains the error status code.
 */
@SuppressWarnings("serial")
public class ConfigException extends RuntimeException {
    public enum StatusCode {
        INVALID_LOCATION, CANNOT_WRITE, SECTION_OR_NAME_NOT_PROVIDED, SECTION_OR_KEY_INVALID, OPTION_DOES_N0T_EXIST, MULTIPLE_OPTIONS_MATCH, INVALID_REGEXP, USERHOME_NOT_SET, TOO_MANY_ACTIONS, MISSING_SECTION, TOO_MANY_ARGS
    }

    public StatusCode statusCode;

    public ConfigException(StatusCode statusCode) {
        this(null, statusCode);
    }

    public ConfigException(Exception e, StatusCode statusCode) {
        super(e);
        this.statusCode = statusCode;
    }
}
