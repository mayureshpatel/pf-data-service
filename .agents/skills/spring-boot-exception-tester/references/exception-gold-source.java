package com.mayureshpatel.pfdataservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gold Standard examples for Exception unit testing.
 * Demonstrates testing for simple, formatted, and nested exceptions using @Nested organization.
 */
@DisplayName("Exception Gold Standard Tests")
class ExceptionGoldStandardTest {

    @Nested
    @DisplayName("Constructor: Message only")
    class MessageConstructorTests {

        @Test
        @DisplayName("should create exception with simple message")
        void shouldCreateWithSimpleMessage() {
            // Arrange
            String message = "Resource not found";

            // Act
            ResourceNotFoundException exception = new ResourceNotFoundException(message);

            // Assert
            assertEquals(message, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Constructor: Formatted Resource Message")
    class FormattedConstructorTests {

        @Test
        @DisplayName("should create exception with formatted message")
        void shouldCreateWithFormattedMessage() {
            // Arrange
            String resourceName = "User";
            String fieldName = "email";
            String fieldValue = "test@example.com";
            String expectedMessage = "User not found with email: 'test@example.com'";

            // Act
            ResourceNotFoundException exception = new ResourceNotFoundException(resourceName, fieldName, fieldValue);

            // Assert
            assertEquals(expectedMessage, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Constructor: Message and Cause")
    class CauseConstructorTests {

        @Test
        @DisplayName("should create exception with message and cause")
        void shouldCreateWithMessageAndCause() {
            // Arrange
            String message = "Unexpected error";
            Throwable cause = new IllegalStateException("Internal failure");

            // Act
            ResourceNotFoundException exception = new ResourceNotFoundException(message, cause);

            // Assert
            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
        }
    }
}
