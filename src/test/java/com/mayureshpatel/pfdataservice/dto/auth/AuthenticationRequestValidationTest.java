package com.mayureshpatel.pfdataservice.dto.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies {@link AuthenticationRequest}'s jakarta bean-validation constraints pass on a fully valid request and fail correctly per-field, one {@code @Nested} class per field below. */
@DisplayName("AuthenticationRequest validation tests")
class AuthenticationRequestValidationTest {

    private static final String VALID_PASSWORD = "password123";

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("should pass validation with valid fields")
    void validate_validRequest_noViolations() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .username("testuser")
                .password(VALID_PASSWORD)
                .build();

        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    /** {@code username} must be non-blank and no more than 50 characters. */
    @Nested
    @DisplayName("Field: username")
    class UsernameValidationTests {

        @Test
        @DisplayName("should fail when username is blank")
        void validate_blankUsername_hasViolation() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .username("")
                    .password(VALID_PASSWORD)
                    .build();

            Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> "username".equals(v.getPropertyPath().toString()));
        }

        @Test
        @DisplayName("should fail when username is null")
        void validate_nullUsername_hasViolation() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .username(null)
                    .password(VALID_PASSWORD)
                    .build();

            Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> "username".equals(v.getPropertyPath().toString()));
        }

        @Test
        @DisplayName("should fail when username exceeds 50 characters")
        void validate_usernameTooLong_hasViolation() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .username("a".repeat(51))
                    .password(VALID_PASSWORD)
                    .build();

            Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v ->
                    "username".equals(v.getPropertyPath().toString()) &&
                            v.getMessage().contains("50"));
        }
    }

    /** {@code password} must be non-blank. */
    @Nested
    @DisplayName("Field: password")
    class PasswordValidationTests {

        @Test
        @DisplayName("should fail when password is blank")
        void validate_blankPassword_hasViolation() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .username("testuser")
                    .password("")
                    .build();

            Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> "password".equals(v.getPropertyPath().toString()));
        }

        @Test
        @DisplayName("should fail when password is null")
        void validate_nullPassword_hasViolation() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .username("testuser")
                    .password(null)
                    .build();

            Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> "password".equals(v.getPropertyPath().toString()));
        }
    }
}
