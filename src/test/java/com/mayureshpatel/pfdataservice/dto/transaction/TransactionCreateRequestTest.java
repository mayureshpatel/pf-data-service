package com.mayureshpatel.pfdataservice.dto.transaction;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@link TransactionCreateRequest}'s jakarta bean-validation constraints pass on a fully valid request and fail correctly per-field, one {@code @Nested} class per field below. */
@DisplayName("TransactionCreateRequest Validation Tests")
class TransactionCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private TransactionCreateRequest.TransactionCreateRequestBuilder createValidBuilder() {
        return TransactionCreateRequest.builder()
                .accountId(1L)
                .categoryId(2L)
                .amount(new BigDecimal("100.00"))
                .transactionDate(OffsetDateTime.now())
                .description("Groceries")
                .type("EXPENSE")
                .merchantId(3L);
    }

    @Test
    @DisplayName("should pass when all fields are valid")
    void shouldPassWithValidData() {
        TransactionCreateRequest request = createValidBuilder().build();
        Set<ConstraintViolation<TransactionCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    /** {@code accountId} must be non-null and positive. */
    @Nested
    @DisplayName("Field: accountId")
    class AccountIdValidationTests {
        @Test
        @DisplayName("should fail when accountId is null")
        void shouldFailWhenAccountIdIsNull() {
            TransactionCreateRequest request = createValidBuilder().accountId(null).build();
            Set<ConstraintViolation<TransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Account ID cannot be null.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when accountId is not positive")
        void shouldFailWhenAccountIdIsNotPositive() {
            TransactionCreateRequest request = createValidBuilder().accountId(0L).build();
            Set<ConstraintViolation<TransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Account ID must be a positive number.".equals(v.getMessage())));
        }
    }

    /** {@code categoryId} is optional (an import can arrive uncategorized) but must be positive when present. */
    @Nested
    @DisplayName("Field: categoryId")
    class CategoryIdValidationTests {
        @Test
        @DisplayName("should fail when categoryId is not positive")
        void shouldFailWhenCategoryIdIsNotPositive() {
            TransactionCreateRequest request = createValidBuilder().categoryId(0L).build();
            Set<ConstraintViolation<TransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Category ID must be a positive number.".equals(v.getMessage())));
        }
    }

    /** {@code amount} must be non-null and within +/-9999999999.99. Its null-check violation message ("Starting balance cannot be null.") is copy-pasted from {@code AccountCreateRequest}'s constraint message rather than written for a transaction amount -- cosmetic, but visible to any API caller that surfaces validation messages directly. */
    @Nested
    @DisplayName("Field: amount")
    class AmountValidationTests {
        @Test
        @DisplayName("should fail when amount is null")
        void shouldFailWhenAmountIsNull() {
            TransactionCreateRequest request = createValidBuilder().amount(null).build();
            Set<ConstraintViolation<TransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Starting balance cannot be null.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when amount is below minimum")
        void shouldFailWhenAmountIsBelowMin() {
            TransactionCreateRequest request = createValidBuilder().amount(new BigDecimal("-10000000000.00")).build();
            Set<ConstraintViolation<TransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("transaction amount must be greater than or equal to")));
        }

        @Test
        @DisplayName("should fail when amount is above maximum")
        void shouldFailWhenAmountIsAboveMax() {
            TransactionCreateRequest request = createValidBuilder().amount(new BigDecimal("10000000000.00")).build();
            Set<ConstraintViolation<TransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("transaction amount must be less than or equal to")));
        }
    }

    /** {@code transactionDate} must be non-null. */
    @Nested
    @DisplayName("Field: transactionDate")
    class TransactionDateValidationTests {
        @Test
        @DisplayName("should fail when transactionDate is null")
        void shouldFailWhenTransactionDateIsNull() {
            TransactionCreateRequest request = createValidBuilder().transactionDate(null).build();
            Set<ConstraintViolation<TransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Transaction date cannot be null.".equals(v.getMessage())));
        }
    }

    /** {@code description} must be non-blank and no more than 255 characters. */
    @Nested
    @DisplayName("Field: description")
    class DescriptionValidationTests {
        @Test
        @DisplayName("should fail when description is blank")
        void shouldFailWhenDescriptionIsBlank() {
            TransactionCreateRequest request = createValidBuilder().description("").build();
            Set<ConstraintViolation<TransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Description cannot be blank.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when description exceeds 255 characters")
        void shouldFailWhenDescriptionIsTooLong() {
            TransactionCreateRequest request = createValidBuilder().description("a".repeat(256)).build();
            Set<ConstraintViolation<TransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Description must be less than 255 characters.".equals(v.getMessage())));
        }
    }

    /** {@code type} (the raw string later resolved to a {@code TransactionType}) must be non-blank and no more than 20 characters. */
    @Nested
    @DisplayName("Field: type")
    class TypeValidationTests {
        @Test
        @DisplayName("should fail when type is blank")
        void shouldFailWhenTypeIsBlank() {
            TransactionCreateRequest request = createValidBuilder().type("").build();
            Set<ConstraintViolation<TransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Type cannot be blank.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when type exceeds 20 characters")
        void shouldFailWhenTypeIsTooLong() {
            TransactionCreateRequest request = createValidBuilder().type("a".repeat(21)).build();
            Set<ConstraintViolation<TransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Type must be less than 20 characters.".equals(v.getMessage())));
        }
    }

    /** {@code merchantId} is optional but must be positive when present. */
    @Nested
    @DisplayName("Field: merchantId")
    class MerchantIdValidationTests {
        @Test
        @DisplayName("should fail when merchantId is not positive")
        void shouldFailWhenMerchantIdIsNotPositive() {
            TransactionCreateRequest request = createValidBuilder().merchantId(0L).build();
            Set<ConstraintViolation<TransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Merchant ID must be a positive number.".equals(v.getMessage())));
        }
    }
}
