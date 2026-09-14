package com.mayureshpatel.pfdataservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ResourceNotFoundException Unit Tests")
class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("should create exception with simple message")
    void shouldCreateWithSimpleMessage() {
        // arrange
        String message = "User not found";

        // act
        ResourceNotFoundException exception = new ResourceNotFoundException(message);

        // assert & verify
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("should create exception with formatted message")
    void shouldCreateWithFormattedMessage() {
        // arrange
        String resourceName = "Account";
        String fieldName = "id";
        Long fieldValue = 123L;
        String expectedMessage = "Account not found with id: '123'";

        // act
        ResourceNotFoundException exception = new ResourceNotFoundException(resourceName, fieldName, fieldValue);

        // assert & verify
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateWithMessageAndCause() {
        // arrange
        String message = "Operation failed";
        Throwable cause = new RuntimeException("Database error");

        // act
        ResourceNotFoundException exception = new ResourceNotFoundException(message, cause);

        // assert & verify
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
