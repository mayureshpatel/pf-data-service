package com.mayureshpatel.pfdataservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
            // arrange
            String message = "Resource not found";

            // act
            ResourceNotFoundException exception = new ResourceNotFoundException(message);

            // assert & verify
            assertEquals(message, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Constructor: Formatted Resource Message")
    class FormattedConstructorTests {

        @Test
        @DisplayName("should create exception with formatted message")
        void shouldCreateWithFormattedMessage() {
            // arrange
            String resourceName = "User";
            String fieldName = "email";
            String fieldValue = "test@example.com";
            String expectedMessage = "User not found with email: 'test@example.com'";

            // act
            ResourceNotFoundException exception = new ResourceNotFoundException(resourceName, fieldName, fieldValue);

            // assert & verify
            assertEquals(expectedMessage, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Constructor: Message and Cause")
    class CauseConstructorTests {

        @Test
        @DisplayName("should create exception with message and cause")
        void shouldCreateWithMessageAndCause() {
            // arrange
            String message = "Unexpected error";
            Throwable cause = new IllegalStateException("Internal failure");

            // act
            ResourceNotFoundException exception = new ResourceNotFoundException(message, cause);

            // assert & verify
            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
        }
    }
}
