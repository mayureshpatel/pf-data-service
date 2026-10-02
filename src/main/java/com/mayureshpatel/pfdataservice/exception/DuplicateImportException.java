package com.mayureshpatel.pfdataservice.exception;

/**
 * Thrown when a CSV file's hash matches a previously imported file for the same account -- the
 * whole batch is rejected outright rather than silently re-imported or partially skipped.
 */
public class DuplicateImportException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** @param message a description of the duplicate that was detected */
    public DuplicateImportException(String message) {
        super(message);
    }
}
