package com.mayureshpatel.pfdataservice.dto.account;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AccountCreateRequest Unit Tests")
class AccountCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("should pass when all fields are valid")
    void shouldPassWithValidData() {
        AccountCreateRequest request = AccountCreateRequest.builder()
                .userId(1L)
                .name("Savings")
                .type("SAVINGS")
                .startingBalance(BigDecimal.ZERO)
                .currencyCode("USD")
                .bankName("Test Bank")
                .build();

        Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Nested
    @DisplayName("Field: userId")
    class UserIdValidationTests {
        @Test
        @DisplayName("should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .userId(null)
                    .build();
            Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "userId", "User ID cannot be null.");
        }

        @Test
        @DisplayName("should fail when userId is not positive")
        void shouldFailWhenUserIdIsNotPositive() {
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .userId(0L)
                    .build();
            Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "userId", "User ID must be a positive number.");
        }
    }

    @Nested
    @DisplayName("Field: name")
    class NameValidationTests {
        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   "})
        @DisplayName("should fail when name is blank")
        void shouldFailWhenNameIsBlank(String blankName) {
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .name(blankName)
                    .build();
            Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "name", "Account name cannot be blank.");
        }

        @Test
        @DisplayName("should fail when name exceeds 100 characters")
        void shouldFailWhenNameIsTooLong() {
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .name("A".repeat(101))
                    .build();
            Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "name", "Account name must be less than 100 characters.");
        }
    }

    @Nested
    @DisplayName("Field: type")
    class TypeValidationTests {
        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   "})
        @DisplayName("should fail when type is blank")
        void shouldFailWhenTypeIsBlank(String blankType) {
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .type(blankType)
                    .build();
            Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "type", "Account type cannot be blank.");
        }

        @Test
        @DisplayName("should fail when type exceeds 20 characters")
        void shouldFailWhenTypeIsTooLong() {
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .type("A".repeat(21))
                    .build();
            Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "type", "Account type must be less than 20 characters.");
        }
    }

    @Nested
    @DisplayName("Field: startingBalance")
    class StartingBalanceValidationTests {
        @Test
        @DisplayName("should fail when startingBalance is null")
        void shouldFailWhenStartingBalanceIsNull() {
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .startingBalance(null)
                    .build();
            Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "startingBalance", "Starting balance cannot be null.");
        }

        @Test
        @DisplayName("should fail when startingBalance is less than -9999999999.99")
        void shouldFailWhenStartingBalanceIsTooSmall() {
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .startingBalance(new BigDecimal("-10000000000.00"))
                    .build();
            Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "startingBalance", "Starting balance must be greater than or equal to -9999999999.99");
        }

        @Test
        @DisplayName("should fail when startingBalance is greater than 9999999999.99")
        void shouldFailWhenStartingBalanceIsTooLarge() {
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .startingBalance(new BigDecimal("10000000000.00"))
                    .build();
            Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "startingBalance", "Starting balance must be less than or equal to 9999999999.99");
        }
    }

    @Nested
    @DisplayName("Field: currencyCode")
    class CurrencyCodeValidationTests {
        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   "})
        @DisplayName("should fail when currencyCode is blank")
        void shouldFailWhenCurrencyCodeIsBlank(String blankCurrency) {
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .currencyCode(blankCurrency)
                    .build();
            Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "currencyCode", "Currency code cannot be blank.");
        }

        @ParameterizedTest
        @ValueSource(strings = {"US", "USDE"})
        @DisplayName("should fail when currencyCode is not exactly 3 characters")
        void shouldFailWhenCurrencyCodeIsWrongLength(String wrongLength) {
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .currencyCode(wrongLength)
                    .build();
            Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "currencyCode", "Currency code must be exactly 3 characters.");
        }
    }

    @Nested
    @DisplayName("Field: bankName")
    class BankNameValidationTests {
        @Test
        @DisplayName("should fail when bankName exceeds 50 characters")
        void shouldFailWhenBankNameIsTooLong() {
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .bankName("A".repeat(51))
                    .build();
            Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
            assertViolation(violations, "bankName", "Bank name must be less than 50 characters.");
        }

        @Test
        @DisplayName("should pass when bankName is null")
        void shouldPassWhenBankNameIsNull() {
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .userId(1L)
                    .name("Savings")
                    .type("SAVINGS")
                    .startingBalance(BigDecimal.ZERO)
                    .currencyCode("USD")
                    .bankName(null)
                    .build();

            Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Should have no violations");
        }
    }

    private void assertViolation(Set<ConstraintViolation<AccountCreateRequest>> violations, String property, String message) {
        assertFalse(violations.isEmpty(), "Should have violations for " + property);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals(property) && v.getMessage().equals(message)
        ), "Should have violation for " + property + " with message: " + message);
    }
}
