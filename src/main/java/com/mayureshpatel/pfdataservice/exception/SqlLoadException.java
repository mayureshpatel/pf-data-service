package com.mayureshpatel.pfdataservice.exception;

/**
 * Thrown by {@code SqlLoader} when a {@code .sql} classpath resource can't be read. {@code SqlLoader}
 * itself currently has no callers in this codebase -- every repository instead keeps its SQL as
 * inline Java text-block constants (see any {@code *Queries} class) rather than loading it from
 * external files at runtime.
 */
public class SqlLoadException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * @param message a description of what failed to load
     * @param cause   the underlying I/O failure
     */
    public SqlLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
