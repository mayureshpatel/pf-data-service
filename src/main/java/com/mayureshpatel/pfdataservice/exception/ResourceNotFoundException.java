package com.mayureshpatel.pfdataservice.exception;

/**
 * This codebase's general-purpose "entity not found" exception -- thrown by nearly every
 * repository/service lookup that resolves to {@code ResourceNotFoundException} via
 * {@code GlobalExceptionHandler} into a 404 response, rather than each resource type having its
 * own dedicated not-found exception. Every one of this exception's 55 real call sites uses the
 * simple {@link #ResourceNotFoundException(String)} constructor -- the other two below are
 * currently unused.
 */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param message a description of what wasn't found */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Builds a standardized "{@code resourceName} not found with {@code fieldName}: '{@code
     * fieldValue}'" message rather than requiring every call site to format its own. Currently
     * unused -- every real call site formats its own message via
     * {@link #ResourceNotFoundException(String)} instead.
     *
     * @param resourceName the kind of resource that wasn't found (e.g. "Account")
     * @param fieldName    the field that was searched on (e.g. "id")
     * @param fieldValue   the value that didn't match anything
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }

    /**
     * Currently unused -- every real call site uses {@link #ResourceNotFoundException(String)}
     * instead, without a wrapped cause.
     *
     * @param message a description of what wasn't found
     * @param cause   the underlying failure
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
