package com.mayureshpatel.pfdataservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CsvParsingException Unit Tests")
class CsvParsingExceptionTest {

    @Nested
    @DisplayName("Constructor: Message only")
    class MessageConstructorTests {

        @Test
        @DisplayName("should create exception with provided message")
        void shouldCreateWithProvidedMessage() {
            // Arrange
            String message = "Failed to parse CSV at line 5";

            // Act
            CsvParsingException exception = new CsvParsingException(message);

            // Assert
            assertEquals(message, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Constructor: Message and Cause")
    class MessageAndCauseConstructorTests {

        @Test
        @DisplayName("should create exception with provided message and cause")
        void shouldCreateWithProvidedMessageAndCause() {
            // Arrange
            String message = "Invalid numeric format";
            Throwable cause = new NumberFormatException("Invalid double");

            // Act
            CsvParsingException exception = new CsvParsingException(message, cause);

            // Assert
            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
        }
    }
}
