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

@DisplayName("BudgetCreateRequest Unit Tests")
class BudgetCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("should pass when all fields are valid")
    void shouldPassWithValidData() {
        BudgetCreateRequest request = BudgetCreateRequest.builder()
                .userId(1L)
                .categoryId(1L)
                .amount(new BigDecimal("1000.00"))
                .month(1)
                .year(2024)
                .build();

        Set<ConstraintViolation<BudgetCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Nested
    @DisplayName("Field: userId")
    class UserIdValidationTests {
        @Test
        @DisplayName("should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .userId(null)
                    .build();
            Set<ConstraintViolation<BudgetCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "userId", "User ID cannot be null.");
        }

        @Test
        @DisplayName("should fail when userId is not positive")
        void shouldFailWhenUserIdIsNotPositive() {
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .userId(0L)
                    .build();
            Set<ConstraintViolation<BudgetCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "userId", "User ID must be a positive number.");
        }
    }

    @Nested
    @DisplayName("Field: categoryId")
    class CategoryIdValidationTests {
        @Test
        @DisplayName("should fail when categoryId is null")
        void shouldFailWhenCategoryIdIsNull() {
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .categoryId(null)
                    .build();
            Set<ConstraintViolation<BudgetCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "categoryId", "Category ID cannot be null.");
        }

        @Test
        @DisplayName("should fail when categoryId is not positive")
        void shouldFailWhenCategoryIdIsNotPositive() {
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .categoryId(-1L)
                    .build();
            Set<ConstraintViolation<BudgetCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "categoryId", "Category ID must be a positive number.");
        }
    }

    @Nested
    @DisplayName("Field: amount")
    class AmountValidationTests {
        @Test
        @DisplayName("should fail when amount is null")
        void shouldFailWhenAmountIsNull() {
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .amount(null)
                    .build();
            Set<ConstraintViolation<BudgetCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "amount", "Amount cannot be null.");
        }

        @Test
        @DisplayName("should fail when amount is less than -9999999999.99")
        void shouldFailWhenAmountIsTooSmall() {
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .amount(new BigDecimal("-10000000000.00"))
                    .build();
            Set<ConstraintViolation<BudgetCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "amount", "Amount must be greater than or equal to -9999999999.99");
        }

        @Test
        @DisplayName("should fail when amount is more than 9999999999.99")
        void shouldFailWhenAmountIsTooLarge() {
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .amount(new BigDecimal("10000000000.00"))
                    .build();
            Set<ConstraintViolation<BudgetCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "amount", "Amount must be less than or equal to 9999999999.99");
        }
    }

    @Nested
    @DisplayName("Field: month")
    class MonthValidationTests {
        @Test
        @DisplayName("should fail when month is null")
        void shouldFailWhenMonthIsNull() {
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .month(null)
                    .build();
            Set<ConstraintViolation<BudgetCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "month", "Month cannot be null.");
        }

        @Test
        @DisplayName("should fail when month is 0")
        void shouldFailWhenMonthIsZero() {
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .month(0)
                    .build();
            Set<ConstraintViolation<BudgetCreateRequest>> violations = validator.validate(request);
            // This will have multiple violations: @Positive and @DecimalMin(1)
            assertTrue(violations.size() >= 1);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("month")));
        }

        @Test
        @DisplayName("should fail when month is 13")
        void shouldFailWhenMonthIsTooLarge() {
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .month(13)
                    .build();
            Set<ConstraintViolation<BudgetCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "month", "Month must be less than or equal to 12");
        }
    }

    @Nested
    @DisplayName("Field: year")
    class YearValidationTests {
        @Test
        @DisplayName("should fail when year is null")
        void shouldFailWhenYearIsNull() {
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .year(null)
                    .build();
            Set<ConstraintViolation<BudgetCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "year", "Year cannot be null.");
        }

        @Test
        @DisplayName("should fail when year is less than 1900")
        void shouldFailWhenYearIsTooSmall() {
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .year(1899)
                    .build();
            Set<ConstraintViolation<BudgetCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "year", "Year must be greater than or equal to 1900");
        }

        @Test
        @DisplayName("should fail when year is greater than 9999")
        void shouldFailWhenYearIsTooLarge() {
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .year(10000)
                    .build();
            Set<ConstraintViolation<BudgetCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "year", "Year must be less than or equal to 9999");
        }
    }

    private void assertViolation(Set<ConstraintViolation<BudgetCreateRequest>> violations, String property, String message) {
        assertFalse(violations.isEmpty(), "Should have violations for " + property);
        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals(property) && v.getMessage().equals(message)
        ), "Should have violation for " + property + " with message: " + message);
    }
}
