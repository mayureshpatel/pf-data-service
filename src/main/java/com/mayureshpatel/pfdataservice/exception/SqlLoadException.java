package com.mayureshpatel.pfdataservice.exception;

public class SqlLoadException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SqlLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
