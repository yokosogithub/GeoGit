package org.geogit.geotools.plumbing;

/**
 * @author mfawcett
 * 
 *         Exception thrown by ConfigDatabase that contains the error status code.
 */
@SuppressWarnings("serial")
public class GeoToolsOpException extends RuntimeException {
    public enum StatusCode {
        ALL_AND_TABLE_DEFINED, DATASTORE_NOT_DEFINED, TABLE_NOT_DEFINED, NO_FEATURES_FOUND, TABLE_NOT_FOUND, UNABLE_TO_GET_NAMES, UNABLE_TO_GET_FEATURES, UNABLE_TO_INSERT
    }

    public StatusCode statusCode;

    public GeoToolsOpException(StatusCode statusCode) {
        this(null, statusCode);
    }

    public GeoToolsOpException(Exception e, StatusCode statusCode) {
        super(e);
        this.statusCode = statusCode;
    }
}
