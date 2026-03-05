package com.mayureshpatel.pfdataservice.dto;

import com.mayureshpatel.pfdataservice.dto.account.AccountCreateRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gold Standard examples for DTO unit testing.
 * Demonstrates testing for both Request validation and Response structure.
 */
@DisplayName("DTO Gold Standard Tests")
class DtoGoldStandardTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Request DTO Validation (e.g., AccountCreateRequest)")
    class RequestValidationTests {

        @Test
        @DisplayName("should pass when all fields are valid")
        void shouldPassWithValidData() {
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .name("Savings")
                    .type("SAVINGS")
                    .currencyCode("USD")
                    .startingBalance(BigDecimal.ZERO)
                    .build();

            Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Should have no violations");
        }

        @Nested
        @DisplayName("Field: name")
        class NameValidationTests {
            @Test
            @DisplayName("should fail when name is blank")
            void shouldFailWhenNameIsBlank() {
                AccountCreateRequest request = AccountCreateRequest.builder().name("").build();
                Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
                
                assertFalse(violations.isEmpty());
                assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
            }

            @Test
            @DisplayName("should fail when name exceeds 100 characters (Schema Limit)")
            void shouldFailWhenNameIsTooLong() {
                AccountCreateRequest request = AccountCreateRequest.builder()
                        .name("A".repeat(101))
                        .build();
                Set<ConstraintViolation<AccountCreateRequest>> violations = validator.validate(request);
                
                assertFalse(violations.isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("Response DTO Structure (e.g., AccountDto)")
    class ResponseStructureTests {

        @Test
        @DisplayName("should correctly map all fields")
        void shouldPopulateFields() {
            AccountDto dto = new AccountDto(
                    1L, null, "Account Name", null, BigDecimal.TEN, null, null
            );

            assertEquals(1L, dto.id());
            assertEquals("Account Name", dto.name());
            assertEquals(BigDecimal.TEN, dto.currentBalance());
        }
    }
}
