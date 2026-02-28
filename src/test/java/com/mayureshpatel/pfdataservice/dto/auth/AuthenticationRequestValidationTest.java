package com.mayureshpatel.pfdataservice.dto.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthenticationRequest validation tests")
class AuthenticationRequestValidationTest {

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
                .password("password123")
                .build();

        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("should fail when username is blank")
    void validate_blankUsername_hasViolation() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .username("")
                .password("password123")
                .build();

        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("should fail when username is null")
    void validate_nullUsername_hasViolation() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .username(null)
                .password("password123")
                .build();

        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("should fail when password is blank")
    void validate_blankPassword_hasViolation() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .username("testuser")
                .password("")
                .build();

        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("should fail when password is null")
    void validate_nullPassword_hasViolation() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .username("testuser")
                .password(null)
                .build();

        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("should fail when username exceeds 50 characters")
    void validate_usernameTooLong_hasViolation() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .username("a".repeat(51))
                .password("password123")
                .build();

        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("username") &&
                v.getMessage().contains("50"));
    }
}
