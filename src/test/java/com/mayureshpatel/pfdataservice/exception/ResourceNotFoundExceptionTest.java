package com.mayureshpatel.pfdataservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ResourceNotFoundException Unit Tests")
class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("should create exception with simple message")
    void shouldCreateWithSimpleMessage() {
        // Arrange
        String message = "User not found";

        // Act
        ResourceNotFoundException exception = new ResourceNotFoundException(message);

        // Assert
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("should create exception with formatted message")
    void shouldCreateWithFormattedMessage() {
        // Arrange
        String resourceName = "Account";
        String fieldName = "id";
        Long fieldValue = 123L;
        String expectedMessage = "Account not found with id: '123'";

        // Act
        ResourceNotFoundException exception = new ResourceNotFoundException(resourceName, fieldName, fieldValue);

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateWithMessageAndCause() {
        // Arrange
        String message = "Operation failed";
        Throwable cause = new RuntimeException("Database error");

        // Act
        ResourceNotFoundException exception = new ResourceNotFoundException(message, cause);

        // Assert
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
