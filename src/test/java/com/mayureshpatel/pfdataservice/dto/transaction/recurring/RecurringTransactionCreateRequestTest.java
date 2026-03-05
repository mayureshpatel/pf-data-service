package com.mayureshpatel.pfdataservice.dto.transaction.recurring;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RecurringTransactionCreateRequest Validation Tests")
class RecurringTransactionCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private RecurringTransactionCreateRequest.RecurringTransactionCreateRequestBuilder createValidBuilder() {
        return RecurringTransactionCreateRequest.builder()
                .userId(1L)
                .accountId(2L)
                .amount(new BigDecimal("100.00"))
                .frequency("MONTHLY")
                .lastDate(LocalDate.now().minusMonths(1))
                .nextDate(LocalDate.now().plusMonths(1))
                .merchantId(3L)
                .active(true);
    }

    @Test
    @DisplayName("should pass when all fields are valid")
    void shouldPassWithValidData() {
        RecurringTransactionCreateRequest request = createValidBuilder().build();
        Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Nested
    @DisplayName("Field: userId")
    class UserIdValidationTests {
        @Test
        @DisplayName("should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            RecurringTransactionCreateRequest request = createValidBuilder().userId(null).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("User ID cannot be null.")));
        }

        @Test
        @DisplayName("should fail when userId is not positive")
        void shouldFailWhenUserIdIsNotPositive() {
            RecurringTransactionCreateRequest request = createValidBuilder().userId(0L).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("User ID must be a positive number.")));
        }
    }

    @Nested
    @DisplayName("Field: accountId")
    class AccountIdValidationTests {
        @Test
        @DisplayName("should fail when accountId is null")
        void shouldFailWhenAccountIdIsNull() {
            RecurringTransactionCreateRequest request = createValidBuilder().accountId(null).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Account ID cannot be null.")));
        }

        @Test
        @DisplayName("should fail when accountId is not positive")
        void shouldFailWhenAccountIdIsNotPositive() {
            RecurringTransactionCreateRequest request = createValidBuilder().accountId(0L).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Account ID must be a positive number.")));
        }
    }

    @Nested
    @DisplayName("Field: amount")
    class AmountValidationTests {
        @Test
        @DisplayName("should fail when amount is null")
        void shouldFailWhenAmountIsNull() {
            RecurringTransactionCreateRequest request = createValidBuilder().amount(null).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Amount cannot be null.")));
        }

        @Test
        @DisplayName("should fail when amount is below minimum")
        void shouldFailWhenAmountIsBelowMin() {
            RecurringTransactionCreateRequest request = createValidBuilder().amount(new BigDecimal("-10000000000.00")).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Amount must be greater than or equal to")));
        }

        @Test
        @DisplayName("should fail when amount is above maximum")
        void shouldFailWhenAmountIsAboveMax() {
            RecurringTransactionCreateRequest request = createValidBuilder().amount(new BigDecimal("10000000000.00")).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Amount must be less than or equal to")));
        }
    }

    @Nested
    @DisplayName("Field: frequency")
    class FrequencyValidationTests {
        @Test
        @DisplayName("should fail when frequency is null")
        void shouldFailWhenFrequencyIsNull() {
            RecurringTransactionCreateRequest request = createValidBuilder().frequency(null).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Frequency cannot be null.")));
        }

        @Test
        @DisplayName("should fail when frequency pattern is invalid")
        void shouldFailWhenFrequencyIsInvalid() {
            RecurringTransactionCreateRequest request = createValidBuilder().frequency("DAILY").build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Frequency must be one of")));
        }
    }

    @Nested
    @DisplayName("Field: lastDate")
    class LastDateValidationTests {
        @Test
        @DisplayName("should fail when lastDate is in the future")
        void shouldFailWhenLastDateIsInFuture() {
            RecurringTransactionCreateRequest request = createValidBuilder().lastDate(LocalDate.now().plusDays(1)).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Last date must be in the past.")));
        }
    }

    @Nested
    @DisplayName("Field: nextDate")
    class NextDateValidationTests {
        @Test
        @DisplayName("should fail when nextDate is null")
        void shouldFailWhenNextDateIsNull() {
            RecurringTransactionCreateRequest request = createValidBuilder().nextDate(null).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Next date cannot be null.")));
        }

        @Test
        @DisplayName("should fail when nextDate is in the past")
        void shouldFailWhenNextDateIsInPast() {
            RecurringTransactionCreateRequest request = createValidBuilder().nextDate(LocalDate.now().minusDays(1)).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Next date must be in the future.")));
        }
    }

    @Nested
    @DisplayName("Field: merchantId")
    class MerchantIdValidationTests {
        @Test
        @DisplayName("should fail when merchantId is null")
        void shouldFailWhenMerchantIdIsNull() {
            RecurringTransactionCreateRequest request = createValidBuilder().merchantId(null).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Merchant ID cannot be null.")));
        }

        @Test
        @DisplayName("should fail when merchantId is not positive")
        void shouldFailWhenMerchantIdIsNotPositive() {
            RecurringTransactionCreateRequest request = createValidBuilder().merchantId(0L).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Merchant ID must be a positive number.")));
        }
    }
}
