package com.mayureshpatel.pfdataservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DuplicateImportException Unit Tests")
class DuplicateImportExceptionTest {

    @Nested
    @DisplayName("Constructor: Message only")
    class MessageConstructorTests {

        @Test
        @DisplayName("should create exception with provided message")
        void shouldCreateWithProvidedMessage() {
            // Arrange
            String message = "File already imported: hash123";

            // Act
            DuplicateImportException exception = new DuplicateImportException(message);

            // Assert
            assertEquals(message, exception.getMessage());
        }
    }
}
