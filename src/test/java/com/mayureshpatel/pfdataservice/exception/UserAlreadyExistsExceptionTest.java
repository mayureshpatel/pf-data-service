package com.mayureshpatel.pfdataservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies {@code UserAlreadyExistsException}'s single message-only constructor. */
@DisplayName("UserAlreadyExistsException Unit Tests")
class UserAlreadyExistsExceptionTest {

    /** The constructor sets {@code getMessage()} to the given value. */
    @Nested
    @DisplayName("Constructor: Message only")
    class MessageConstructorTests {

        @Test
        @DisplayName("should create exception with provided message")
        void shouldCreateWithProvidedMessage() {
            // arrange
            String message = "User with email john@example.com already exists";

            // act
            UserAlreadyExistsException exception = new UserAlreadyExistsException(message);

            // assert & verify
            assertEquals(message, exception.getMessage());
        }
    }
}
