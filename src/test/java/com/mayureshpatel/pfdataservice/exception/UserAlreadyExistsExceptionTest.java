package com.mayureshpatel.pfdataservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("UserAlreadyExistsException Unit Tests")
class UserAlreadyExistsExceptionTest {

    @Nested
    @DisplayName("Constructor: Message only")
    class MessageConstructorTests {

        @Test
        @DisplayName("should create exception with provided message")
        void shouldCreateWithProvidedMessage() {
            // Arrange
            String message = "User with email john@example.com already exists";

            // Act
            UserAlreadyExistsException exception = new UserAlreadyExistsException(message);

            // Assert
            assertEquals(message, exception.getMessage());
        }
    }
}
