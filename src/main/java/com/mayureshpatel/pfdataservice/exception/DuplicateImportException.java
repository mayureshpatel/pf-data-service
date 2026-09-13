package com.mayureshpatel.pfdataservice.exception;

public class DuplicateImportException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DuplicateImportException(String message) {
        super(message);
    }
}
