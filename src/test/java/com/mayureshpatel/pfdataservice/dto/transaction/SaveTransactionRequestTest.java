package com.mayureshpatel.pfdataservice.dto.transaction;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SaveTransactionRequest Validation Tests")
class SaveTransactionRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("should pass when all fields are valid")
    void shouldPassWithValidData() {
        TransactionDto transaction = TransactionDto.builder().id(1L).build();
        SaveTransactionRequest request = new SaveTransactionRequest(
                List.of(transaction), "file.csv", "hash123", 10L
        );

        Set<ConstraintViolation<SaveTransactionRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Nested
    @DisplayName("Field: transactions")
    class TransactionsValidationTests {
        @Test
        @DisplayName("should fail when transactions list is null")
        void shouldFailWhenTransactionsIsNull() {
            SaveTransactionRequest request = new SaveTransactionRequest(null, "file.csv", "hash123", 10L);
            Set<ConstraintViolation<SaveTransactionRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Transactions list must not be empty")));
        }

        @Test
        @DisplayName("should fail when transactions list is empty")
        void shouldFailWhenTransactionsIsEmpty() {
            SaveTransactionRequest request = new SaveTransactionRequest(Collections.emptyList(), "file.csv", "hash123", 10L);
            Set<ConstraintViolation<SaveTransactionRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Transactions list must not be empty")));
        }
    }

    @Nested
    @DisplayName("Field: fileName")
    class FileNameValidationTests {
        @Test
        @DisplayName("should fail when fileName is blank")
        void shouldFailWhenFileNameIsBlank() {
            SaveTransactionRequest request = new SaveTransactionRequest(List.of(TransactionDto.builder().id(1L).build()), "", "hash123", 10L);
            Set<ConstraintViolation<SaveTransactionRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("File name cannot be blank")));
        }
    }

    @Nested
    @DisplayName("Field: fileHash")
    class FileHashValidationTests {
        @Test
        @DisplayName("should fail when fileHash is blank")
        void shouldFailWhenFileHashIsBlank() {
            SaveTransactionRequest request = new SaveTransactionRequest(List.of(TransactionDto.builder().id(1L).build()), "file.csv", "", 10L);
            Set<ConstraintViolation<SaveTransactionRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("File hash cannot be blank")));
        }
    }
}
