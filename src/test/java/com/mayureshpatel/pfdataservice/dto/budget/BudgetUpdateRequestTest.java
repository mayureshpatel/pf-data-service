package com.mayureshpatel.pfdataservice.dto.budget;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BudgetUpdateRequest Unit Tests")
class BudgetUpdateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("should pass when all fields are valid")
    void shouldPassWithValidData() {
        BudgetUpdateRequest request = BudgetUpdateRequest.builder()
                .id(1L)
                .userId(1L)
                .amount(new BigDecimal("1500.00"))
                .build();

        Set<ConstraintViolation<BudgetUpdateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Nested
    @DisplayName("Field: id")
    class IdValidationTests {
        @Test
        @DisplayName("should fail when id is null")
        void shouldFailWhenIdIsNull() {
            BudgetUpdateRequest request = BudgetUpdateRequest.builder()
                    .id(null)
                    .build();
            Set<ConstraintViolation<BudgetUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "id", "Budget ID cannot be null.");
        }

        @Test
        @DisplayName("should fail when id is not positive")
        void shouldFailWhenIdIsNotPositive() {
            BudgetUpdateRequest request = BudgetUpdateRequest.builder()
                    .id(0L)
                    .build();
            Set<ConstraintViolation<BudgetUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "id", "Budget ID must be a positive number.");
        }
    }

    @Nested
    @DisplayName("Field: userId")
    class UserIdValidationTests {
        @Test
        @DisplayName("should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            BudgetUpdateRequest request = BudgetUpdateRequest.builder()
                    .userId(null)
                    .build();
            Set<ConstraintViolation<BudgetUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "userId", "User ID cannot be null.");
        }

        @Test
        @DisplayName("should fail when userId is not positive")
        void shouldFailWhenUserIdIsNotPositive() {
            BudgetUpdateRequest request = BudgetUpdateRequest.builder()
                    .userId(-1L)
                    .build();
            Set<ConstraintViolation<BudgetUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "userId", "User ID must be a positive number.");
        }
    }

    @Nested
    @DisplayName("Field: amount")
    class AmountValidationTests {
        @Test
        @DisplayName("should fail when amount is null")
        void shouldFailWhenAmountIsNull() {
            BudgetUpdateRequest request = BudgetUpdateRequest.builder()
                    .amount(null)
                    .build();
            Set<ConstraintViolation<BudgetUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "amount", "Amount cannot be null.");
        }

        @Test
        @DisplayName("should fail when amount is less than -9999999999.99")
        void shouldFailWhenAmountIsTooSmall() {
            BudgetUpdateRequest request = BudgetUpdateRequest.builder()
                    .amount(new BigDecimal("-10000000000.00"))
                    .build();
            Set<ConstraintViolation<BudgetUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "amount", "Amount must be greater than or equal to -9999999999.99");
        }

        @Test
        @DisplayName("should fail when amount is more than 9999999999.99")
        void shouldFailWhenAmountIsTooLarge() {
            BudgetUpdateRequest request = BudgetUpdateRequest.builder()
                    .amount(new BigDecimal("10000000000.00"))
                    .build();
            Set<ConstraintViolation<BudgetUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "amount", "Amount must be less than or equal to 9999999999.99");
        }
    }

    private void assertViolation(Set<ConstraintViolation<BudgetUpdateRequest>> violations, String property, String message) {
        assertFalse(violations.isEmpty(), "Should have violations for " + property);
        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals(property) && v.getMessage().equals(message)
        ), "Should have violation for " + property + " with message: " + message);
    }
}
