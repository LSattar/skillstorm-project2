package exceptions;

/**
 * Exception thrown when a resource operation conflicts with existing data or business rules.
 * 
 * <p>This exception is used to indicate that an operation cannot be completed due to
 * a conflict, such as:
 * <ul>
 *   <li>Attempting to create a resource with a duplicate unique identifier</li>
 *   <li>Attempting to delete a resource that is referenced by other entities</li>
 *   <li>Other business rule violations that prevent the operation</li>
 * </ul>
 * 
 * <p>The exception is handled by {@link GlobalExceptionHandler} and returns an
 * HTTP 409 (Conflict) status code to the client.
 */
public class ResourceConflictException extends RuntimeException {
    /**
     * Constructs a new ResourceConflictException with the specified detail message.
     * 
     * @param message The detail message explaining the conflict
     */
    public ResourceConflictException(String message) {
        super(message);
    }
}