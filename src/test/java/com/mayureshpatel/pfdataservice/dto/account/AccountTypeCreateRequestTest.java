package com.mayureshpatel.pfdataservice.dto.account;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AccountTypeCreateRequest Unit Tests")
class AccountTypeCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("should pass when all fields are valid")
    void shouldPassWithValidData() {
        AccountTypeCreateRequest request = AccountTypeCreateRequest.builder()
                .code("SAVINGS")
                .label("Savings Account")
                .icon("piggy-bank")
                .color("#00FF00")
                .isAsset(true)
                .sortOrder(1)
                .isActive(true)
                .build();

        Set<ConstraintViolation<AccountTypeCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Nested
    @DisplayName("Field: code")
    class CodeValidationTests {
        @Test
        @DisplayName("should fail when code is null")
        void shouldFailWhenCodeIsNull() {
            AccountTypeCreateRequest request = AccountTypeCreateRequest.builder()
                    .code(null)
                    .build();
            Set<ConstraintViolation<AccountTypeCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "code", "Account type code cannot be null.");
        }

        @Test
        @DisplayName("should fail when code exceeds 20 characters")
        void shouldFailWhenCodeIsTooLong() {
            AccountTypeCreateRequest request = AccountTypeCreateRequest.builder()
                    .code("A".repeat(21))
                    .build();
            Set<ConstraintViolation<AccountTypeCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "code", "Account type code must be less than 20 characters.");
        }
    }

    @Nested
    @DisplayName("Field: label")
    class LabelValidationTests {
        @Test
        @DisplayName("should fail when label is null")
        void shouldFailWhenLabelIsNull() {
            AccountTypeCreateRequest request = AccountTypeCreateRequest.builder()
                    .label(null)
                    .build();
            Set<ConstraintViolation<AccountTypeCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "label", "Account type label cannot be null.");
        }

        @Test
        @DisplayName("should fail when label exceeds 50 characters")
        void shouldFailWhenLabelIsTooLong() {
            AccountTypeCreateRequest request = AccountTypeCreateRequest.builder()
                    .label("A".repeat(51))
                    .build();
            Set<ConstraintViolation<AccountTypeCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "label", "Account type label must be less than 50 characters.");
        }
    }

    @Nested
    @DisplayName("Field: icon")
    class IconValidationTests {
        @Test
        @DisplayName("should fail when icon exceeds 50 characters")
        void shouldFailWhenIconIsTooLong() {
            AccountTypeCreateRequest request = AccountTypeCreateRequest.builder()
                    .icon("A".repeat(51))
                    .build();
            Set<ConstraintViolation<AccountTypeCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "icon", "Account type icon must be less than 50 characters.");
        }
    }

    @Nested
    @DisplayName("Field: color")
    class ColorValidationTests {
        @Test
        @DisplayName("should fail when color exceeds 20 characters")
        void shouldFailWhenColorIsTooLong() {
            AccountTypeCreateRequest request = AccountTypeCreateRequest.builder()
                    .color("A".repeat(21))
                    .build();
            Set<ConstraintViolation<AccountTypeCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "color", "Account type color must be less than 20 characters.");
        }
    }

    @Nested
    @DisplayName("Field: sortOrder")
    class SortOrderValidationTests {
        @Test
        @DisplayName("should fail when sortOrder is null")
        void shouldFailWhenSortOrderIsNull() {
            AccountTypeCreateRequest request = AccountTypeCreateRequest.builder()
                    .sortOrder(null)
                    .build();
            Set<ConstraintViolation<AccountTypeCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "sortOrder", "Account type sort order cannot be null.");
        }

        @Test
        @DisplayName("should fail when sortOrder is negative")
        void shouldFailWhenSortOrderIsNegative() {
            AccountTypeCreateRequest request = AccountTypeCreateRequest.builder()
                    .sortOrder(-1)
                    .build();
            Set<ConstraintViolation<AccountTypeCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "sortOrder", "Account type sort order must be a positive number or zero.");
        }
    }

    private void assertViolation(Set<ConstraintViolation<AccountTypeCreateRequest>> violations, String property, String message) {
        assertFalse(violations.isEmpty(), "Should have violations for " + property);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals(property) && v.getMessage().equals(message)
        ), "Should have violation for " + property + " with message: " + message);
    }
}
