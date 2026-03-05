package com.mayureshpatel.pfdataservice.dto.account;

import com.mayureshpatel.pfdataservice.domain.account.Account;
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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AccountUpdateRequest Unit Tests")
class AccountUpdateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("should pass when all fields are valid")
    void shouldPassWithValidData() {
        AccountUpdateRequest request = AccountUpdateRequest.builder()
                .id(1L)
                .name("Savings")
                .type("SAVINGS")
                .currencyCode("USD")
                .bankName("Test Bank")
                .version(1L)
                .build();

        Set<ConstraintViolation<AccountUpdateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("should correctly map to domain object")
    void shouldConvertToDomain() {
        AccountUpdateRequest request = AccountUpdateRequest.builder()
                .id(1L)
                .name("Savings")
                .type("SAVINGS")
                .currencyCode("USD")
                .bankName("Test Bank")
                .version(2L)
                .build();

        Account account = request.toDomain();

        assertEquals(request.getId(), account.getId());
        assertEquals(request.getName(), account.getName());
        assertEquals(request.getType(), account.getTypeCode());
        assertEquals(request.getCurrencyCode(), account.getCurrencyCode());
        assertEquals(request.getBankName(), account.getBankCode());
        assertEquals(request.getVersion(), account.getVersion());
    }

    @Nested
    @DisplayName("Field: id")
    class IdValidationTests {
        @Test
        @DisplayName("should fail when id is null")
        void shouldFailWhenIdIsNull() {
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .id(null)
                    .build();
            Set<ConstraintViolation<AccountUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "id", "Account ID cannot be null.");
        }

        @Test
        @DisplayName("should fail when id is not positive")
        void shouldFailWhenIdIsNotPositive() {
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .id(0L)
                    .build();
            Set<ConstraintViolation<AccountUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "id", "Account ID must be a positive number.");
        }
    }

    @Nested
    @DisplayName("Field: name")
    class NameValidationTests {
        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   "})
        @DisplayName("should fail when name is blank")
        void shouldFailWhenNameIsBlank(String blankName) {
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .name(blankName)
                    .build();
            Set<ConstraintViolation<AccountUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "name", "Account name cannot be blank.");
        }

        @Test
        @DisplayName("should fail when name exceeds 100 characters")
        void shouldFailWhenNameIsTooLong() {
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .name("A".repeat(101))
                    .build();
            Set<ConstraintViolation<AccountUpdateRequest>> violations = validator.validate(request);
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
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .type(blankType)
                    .build();
            Set<ConstraintViolation<AccountUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "type", "Account type cannot be blank.");
        }

        @Test
        @DisplayName("should fail when type exceeds 20 characters")
        void shouldFailWhenTypeIsTooLong() {
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .type("A".repeat(21))
                    .build();
            Set<ConstraintViolation<AccountUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "type", "Account type must be less than 20 characters.");
        }
    }

    @Nested
    @DisplayName("Field: currencyCode")
    class CurrencyCodeValidationTests {
        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   "})
        @DisplayName("should fail when currencyCode is blank")
        void shouldFailWhenCurrencyCodeIsBlank(String blankCurrency) {
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .currencyCode(blankCurrency)
                    .build();
            Set<ConstraintViolation<AccountUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "currencyCode", "Currency code cannot be blank.");
        }

        @ParameterizedTest
        @ValueSource(strings = {"US", "USDE"})
        @DisplayName("should fail when currencyCode is not exactly 3 characters")
        void shouldFailWhenCurrencyCodeIsWrongLength(String wrongLength) {
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .currencyCode(wrongLength)
                    .build();
            Set<ConstraintViolation<AccountUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "currencyCode", "Currency code must be exactly 3 characters.");
        }
    }

    @Nested
    @DisplayName("Field: bankName")
    class BankNameValidationTests {
        @Test
        @DisplayName("should fail when bankName exceeds 50 characters")
        void shouldFailWhenBankNameIsTooLong() {
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .bankName("A".repeat(51))
                    .build();
            Set<ConstraintViolation<AccountUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "bankName", "Bank name must be less than 50 characters.");
        }
    }

    @Nested
    @DisplayName("Field: version")
    class VersionValidationTests {
        @Test
        @DisplayName("should fail when version is null")
        void shouldFailWhenVersionIsNull() {
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .version(null)
                    .build();
            Set<ConstraintViolation<AccountUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "version", "Version cannot be null.");
        }

        @Test
        @DisplayName("should fail when version is not positive")
        void shouldFailWhenVersionIsNotPositive() {
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .version(0L)
                    .build();
            Set<ConstraintViolation<AccountUpdateRequest>> violations = validator.validate(request);
            assertViolation(violations, "version", "Version must be a positive number.");
        }
    }

    private void assertViolation(Set<ConstraintViolation<AccountUpdateRequest>> violations, String property, String message) {
        assertFalse(violations.isEmpty(), "Should have violations for " + property);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals(property) && v.getMessage().equals(message)
        ), "Should have violation for " + property + " with message: " + message);
    }
}
