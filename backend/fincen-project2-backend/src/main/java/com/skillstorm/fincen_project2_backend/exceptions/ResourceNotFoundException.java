package com.skillstorm.fincen_project2_backend.exceptions;

/**
 * Exception thrown when a requested resource cannot be found.
 * 
 * <p>
 * This exception is used throughout the application to indicate that a
 * requested
 * entity does not exist in the system.
 * 
 * <p>
 * The exception is handled by {@link GlobalExceptionHandler} and returns an
 * HTTP 404 (Not Found) status code to the client.
 */
public class ResourceNotFoundException extends RuntimeException {
    /**
     * Constructs a new ResourceNotFoundException with the specified detail message.
     * 
     * @param message The detail message explaining which resource was not found
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}