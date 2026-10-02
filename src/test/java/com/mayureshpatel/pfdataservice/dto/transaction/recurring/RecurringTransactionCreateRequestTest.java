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

/** Verifies {@code RecurringTransactionCreateRequest}'s jakarta bean-validation constraints pass on a fully valid request and fail correctly per-field, one {@code @Nested} class per field below. Notably absent: despite {@code RecurringTransaction} inheriting a {@code category} field from {@code Transaction}, neither this request nor {@link RecurringTransactionUpdateRequestTest} has a {@code categoryId} field to validate. */
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

    /** {@code userId} must be non-null and positive. */
    @Nested
    @DisplayName("Field: userId")
    class UserIdValidationTests {
        @Test
        @DisplayName("should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            RecurringTransactionCreateRequest request = createValidBuilder().userId(null).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "User ID cannot be null.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when userId is not positive")
        void shouldFailWhenUserIdIsNotPositive() {
            RecurringTransactionCreateRequest request = createValidBuilder().userId(0L).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "User ID must be a positive number.".equals(v.getMessage())));
        }
    }

    /** {@code accountId} must be non-null and positive. */
    @Nested
    @DisplayName("Field: accountId")
    class AccountIdValidationTests {
        @Test
        @DisplayName("should fail when accountId is null")
        void shouldFailWhenAccountIdIsNull() {
            RecurringTransactionCreateRequest request = createValidBuilder().accountId(null).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Account ID cannot be null.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when accountId is not positive")
        void shouldFailWhenAccountIdIsNotPositive() {
            RecurringTransactionCreateRequest request = createValidBuilder().accountId(0L).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Account ID must be a positive number.".equals(v.getMessage())));
        }
    }

    /** {@code amount} must be non-null and within +/-9999999999.99. */
    @Nested
    @DisplayName("Field: amount")
    class AmountValidationTests {
        @Test
        @DisplayName("should fail when amount is null")
        void shouldFailWhenAmountIsNull() {
            RecurringTransactionCreateRequest request = createValidBuilder().amount(null).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Amount cannot be null.".equals(v.getMessage())));
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

    /** {@code frequency} must be non-null and one of the recognized {@link Frequency} codes -- {@code "DAILY"} isn't a supported cadence. */
    @Nested
    @DisplayName("Field: frequency")
    class FrequencyValidationTests {
        @Test
        @DisplayName("should fail when frequency is null")
        void shouldFailWhenFrequencyIsNull() {
            RecurringTransactionCreateRequest request = createValidBuilder().frequency(null).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Frequency cannot be null.".equals(v.getMessage())));
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

    /** {@code lastDate} must be in the past -- it records the most recent occurrence actually observed, which can't be a future date. Not present on {@link RecurringTransactionUpdateRequestTest}: once set at creation it isn't user-editable. */
    @Nested
    @DisplayName("Field: lastDate")
    class LastDateValidationTests {
        @Test
        @DisplayName("should fail when lastDate is in the future")
        void shouldFailWhenLastDateIsInFuture() {
            RecurringTransactionCreateRequest request = createValidBuilder().lastDate(LocalDate.now().plusDays(1)).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Last date must be in the past.".equals(v.getMessage())));
        }
    }

    /** {@code nextDate} must be non-null and in the future -- the projected date of the next occurrence. */
    @Nested
    @DisplayName("Field: nextDate")
    class NextDateValidationTests {
        @Test
        @DisplayName("should fail when nextDate is null")
        void shouldFailWhenNextDateIsNull() {
            RecurringTransactionCreateRequest request = createValidBuilder().nextDate(null).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Next date cannot be null.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when nextDate is in the past")
        void shouldFailWhenNextDateIsInPast() {
            RecurringTransactionCreateRequest request = createValidBuilder().nextDate(LocalDate.now().minusDays(1)).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Next date must be in the future.".equals(v.getMessage())));
        }
    }

    /** {@code merchantId} must be non-null and positive -- unlike a plain {@code Transaction}, a recurring transaction always requires a known merchant since it's matched/detected by merchant+amount+frequency. */
    @Nested
    @DisplayName("Field: merchantId")
    class MerchantIdValidationTests {
        @Test
        @DisplayName("should fail when merchantId is null")
        void shouldFailWhenMerchantIdIsNull() {
            RecurringTransactionCreateRequest request = createValidBuilder().merchantId(null).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Merchant ID cannot be null.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when merchantId is not positive")
        void shouldFailWhenMerchantIdIsNotPositive() {
            RecurringTransactionCreateRequest request = createValidBuilder().merchantId(0L).build();
            Set<ConstraintViolation<RecurringTransactionCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Merchant ID must be a positive number.".equals(v.getMessage())));
        }
    }
}
