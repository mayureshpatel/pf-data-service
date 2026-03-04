package com.mayureshpatel.pfdataservice.dto.user;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RegistrationRequest validation tests")
class RegistrationRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private RegistrationRequest.RegistrationRequestBuilder validRequest() {
        return RegistrationRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("P@ssword1");
    }

    @Test
    @DisplayName("should pass validation with all valid fields")
    void validate_validRequest_noViolations() {
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(validRequest().build());

        assertThat(violations).isEmpty();
    }

    @Nested
    @DisplayName("username validation")
    class UsernameValidation {

        @Test
        @DisplayName("should fail when username is blank")
        void validate_blankUsername_hasViolation() {
            Set<ConstraintViolation<RegistrationRequest>> violations =
                    validator.validate(validRequest().username("").build());

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        }

        @Test
        @DisplayName("should fail when username is null")
        void validate_nullUsername_hasViolation() {
            Set<ConstraintViolation<RegistrationRequest>> violations =
                    validator.validate(validRequest().username(null).build());

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        }

        @Test
        @DisplayName("should fail when username is shorter than 3 characters")
        void validate_usernameTooShort_hasViolation() {
            Set<ConstraintViolation<RegistrationRequest>> violations =
                    validator.validate(validRequest().username("ab").build());

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        }

        @Test
        @DisplayName("should fail when username exceeds 50 characters")
        void validate_usernameTooLong_hasViolation() {
            Set<ConstraintViolation<RegistrationRequest>> violations =
                    validator.validate(validRequest().username("a".repeat(51)).build());

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        }

        @Test
        @DisplayName("should fail when username contains special characters")
        void validate_usernameWithSpecialChars_hasViolation() {
            Set<ConstraintViolation<RegistrationRequest>> violations =
                    validator.validate(validRequest().username("user@name").build());

            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("username") &&
                            v.getMessage().contains("letters, numbers, and underscores"));
        }

        @Test
        @DisplayName("should accept username with underscores and digits")
        void validate_usernameWithUnderscoresAndDigits_passes() {
            Set<ConstraintViolation<RegistrationRequest>> violations =
                    validator.validate(validRequest().username("user_123").build());

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("email validation")
    class EmailValidation {

        @Test
        @DisplayName("should fail when email is blank")
        void validate_blankEmail_hasViolation() {
            Set<ConstraintViolation<RegistrationRequest>> violations =
                    validator.validate(validRequest().email("").build());

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("should fail when email is invalid format")
        void validate_invalidEmail_hasViolation() {
            Set<ConstraintViolation<RegistrationRequest>> violations =
                    validator.validate(validRequest().email("not-an-email").build());

            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("email") &&
                            v.getMessage().contains("valid"));
        }
    }

    @Nested
    @DisplayName("password validation")
    class PasswordValidation {

        @Test
        @DisplayName("should fail when password is blank")
        void validate_blankPassword_hasViolation() {
            Set<ConstraintViolation<RegistrationRequest>> violations =
                    validator.validate(validRequest().password("").build());

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("should fail when password is shorter than 8 characters")
        void validate_passwordTooShort_hasViolation() {
            Set<ConstraintViolation<RegistrationRequest>> violations =
                    validator.validate(validRequest().password("P@ss1").build());

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("should fail when password is missing uppercase letter")
        void validate_passwordNoUppercase_hasViolation() {
            Set<ConstraintViolation<RegistrationRequest>> violations =
                    validator.validate(validRequest().password("p@ssword1").build());

            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("password") &&
                            v.getMessage().contains("uppercase"));
        }

        @Test
        @DisplayName("should fail when password is missing lowercase letter")
        void validate_passwordNoLowercase_hasViolation() {
            Set<ConstraintViolation<RegistrationRequest>> violations =
                    validator.validate(validRequest().password("P@SSWORD1").build());

            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("password") &&
                            v.getMessage().contains("lowercase"));
        }

        @Test
        @DisplayName("should fail when password is missing digit")
        void validate_passwordNoDigit_hasViolation() {
            Set<ConstraintViolation<RegistrationRequest>> violations =
                    validator.validate(validRequest().password("P@ssword").build());

            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("password") &&
                            v.getMessage().contains("digit"));
        }

        @Test
        @DisplayName("should fail when password is missing special character")
        void validate_passwordNoSpecialChar_hasViolation() {
            Set<ConstraintViolation<RegistrationRequest>> violations =
                    validator.validate(validRequest().password("Password1").build());

            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("password") &&
                            v.getMessage().contains("special character"));
        }
    }
}
