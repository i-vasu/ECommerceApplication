package com.app.exception;

/**
 * Exception thrown for API-related errors
 */
public class APIException extends RuntimeException {

    public APIException(String message) {
        super(message);
    }

    public APIException(String message, Throwable cause) {
        super(message, cause);
    }
}
