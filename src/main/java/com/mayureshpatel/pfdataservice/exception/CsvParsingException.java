package com.mayureshpatel.pfdataservice.exception;

/**
 * Thrown when a bank statement CSV file can't be parsed -- either a genuinely malformed file, or
 * a mismatch between the file's actual layout and the {@code BankName} format it was parsed as.
 */
public class CsvParsingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** @param message a description of what went wrong parsing the file */
    public CsvParsingException(String message) {
        super(message);
    }

    /**
     * @param message a description of what went wrong parsing the file
     * @param cause   the underlying failure
     */
    public CsvParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
