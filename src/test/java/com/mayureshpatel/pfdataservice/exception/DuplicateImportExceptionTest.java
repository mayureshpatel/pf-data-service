package com.mayureshpatel.pfdataservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies {@code DuplicateImportException}'s single message-only constructor. */
@DisplayName("DuplicateImportException Unit Tests")
class DuplicateImportExceptionTest {

    /** The constructor sets {@code getMessage()} to the given value. */
    @Nested
    @DisplayName("Constructor: Message only")
    class MessageConstructorTests {

        @Test
        @DisplayName("should create exception with provided message")
        void shouldCreateWithProvidedMessage() {
            // arrange
            String message = "File already imported: hash123";

            // act
            DuplicateImportException exception = new DuplicateImportException(message);

            // assert & verify
            assertEquals(message, exception.getMessage());
        }
    }
}
