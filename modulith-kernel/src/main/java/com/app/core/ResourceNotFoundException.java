package com.app.core;

/**
 * Exception thrown when a requested resource is not found
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resource, fieldName, fieldValue));
    }
}
