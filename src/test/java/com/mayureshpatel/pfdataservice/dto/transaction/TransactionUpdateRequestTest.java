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

@DisplayName("TransactionUpdateRequest Validation Tests")
class TransactionUpdateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private TransactionUpdateRequest.TransactionUpdateRequestBuilder createValidBuilder() {
        return TransactionUpdateRequest.builder()
                .id(1L)
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
        TransactionUpdateRequest request = createValidBuilder().build();
        Set<ConstraintViolation<TransactionUpdateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Nested
    @DisplayName("Field: id")
    class IdValidationTests {
        @Test
        @DisplayName("should fail when id is null")
        void shouldFailWhenIdIsNull() {
            TransactionUpdateRequest request = createValidBuilder().id(null).build();
            Set<ConstraintViolation<TransactionUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Transaction ID cannot be null.")));
        }

        @Test
        @DisplayName("should fail when id is not positive")
        void shouldFailWhenIdIsNotPositive() {
            TransactionUpdateRequest request = createValidBuilder().id(0L).build();
            Set<ConstraintViolation<TransactionUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Transaction ID must be a positive number.")));
        }
    }

    @Nested
    @DisplayName("Field: categoryId")
    class CategoryIdValidationTests {
        @Test
        @DisplayName("should fail when categoryId is not positive")
        void shouldFailWhenCategoryIdIsNotPositive() {
            TransactionUpdateRequest request = createValidBuilder().categoryId(0L).build();
            Set<ConstraintViolation<TransactionUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Category ID must be a positive number.")));
        }
    }

    @Nested
    @DisplayName("Field: amount")
    class AmountValidationTests {
        @Test
        @DisplayName("should fail when amount is null")
        void shouldFailWhenAmountIsNull() {
            TransactionUpdateRequest request = createValidBuilder().amount(null).build();
            Set<ConstraintViolation<TransactionUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Starting balance cannot be null.")));
        }

        @Test
        @DisplayName("should fail when amount is below minimum")
        void shouldFailWhenAmountIsBelowMin() {
            TransactionUpdateRequest request = createValidBuilder().amount(new BigDecimal("-10000000000.00")).build();
            Set<ConstraintViolation<TransactionUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("transaction amount must be greater than or equal to")));
        }

        @Test
        @DisplayName("should fail when amount is above maximum")
        void shouldFailWhenAmountIsAboveMax() {
            TransactionUpdateRequest request = createValidBuilder().amount(new BigDecimal("10000000000.00")).build();
            Set<ConstraintViolation<TransactionUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("transaction amount must be less than or equal to")));
        }
    }

    @Nested
    @DisplayName("Field: transactionDate")
    class TransactionDateValidationTests {
        @Test
        @DisplayName("should fail when transactionDate is null")
        void shouldFailWhenTransactionDateIsNull() {
            TransactionUpdateRequest request = createValidBuilder().transactionDate(null).build();
            Set<ConstraintViolation<TransactionUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Transaction date cannot be null.")));
        }
    }

    @Nested
    @DisplayName("Field: description")
    class DescriptionValidationTests {
        @Test
        @DisplayName("should fail when description is blank")
        void shouldFailWhenDescriptionIsBlank() {
            TransactionUpdateRequest request = createValidBuilder().description("").build();
            Set<ConstraintViolation<TransactionUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Description cannot be blank.")));
        }

        @Test
        @DisplayName("should fail when description exceeds 255 characters")
        void shouldFailWhenDescriptionIsTooLong() {
            TransactionUpdateRequest request = createValidBuilder().description("a".repeat(256)).build();
            Set<ConstraintViolation<TransactionUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Description must be less than 255 characters.")));
        }
    }

    @Nested
    @DisplayName("Field: type")
    class TypeValidationTests {
        @Test
        @DisplayName("should fail when type is blank")
        void shouldFailWhenTypeIsBlank() {
            TransactionUpdateRequest request = createValidBuilder().type("").build();
            Set<ConstraintViolation<TransactionUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Type cannot be blank.")));
        }

        @Test
        @DisplayName("should fail when type exceeds 20 characters")
        void shouldFailWhenTypeIsTooLong() {
            TransactionUpdateRequest request = createValidBuilder().type("a".repeat(21)).build();
            Set<ConstraintViolation<TransactionUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Type must be less than 20 characters.")));
        }
    }

    @Nested
    @DisplayName("Field: merchantId")
    class MerchantIdValidationTests {
        @Test
        @DisplayName("should fail when merchantId is not positive")
        void shouldFailWhenMerchantIdIsNotPositive() {
            TransactionUpdateRequest request = createValidBuilder().merchantId(0L).build();
            Set<ConstraintViolation<TransactionUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Merchant ID must be a positive number.")));
        }
    }
}
