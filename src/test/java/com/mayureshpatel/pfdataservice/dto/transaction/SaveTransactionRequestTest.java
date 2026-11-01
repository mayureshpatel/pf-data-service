package com.mayureshpatel.pfdataservice.dto.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.dto.ValidatorTestBase;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@code SaveTransactionRequest}'s jakarta bean-validation constraints pass on a fully valid request and fail correctly per-field, one {@code @Nested} class per field below. */
@DisplayName("SaveTransactionRequest Validation Tests")
class SaveTransactionRequestTest extends ValidatorTestBase {

    private static final String FILE_NAME = "file.csv";
    private static final String FILE_HASH = "hash123";

    @Test
    @DisplayName("should pass when all fields are valid")
    void shouldPassWithValidData() {
        TransactionDto transaction = TransactionDto.builder().id(1L).build();
        SaveTransactionRequest request = new SaveTransactionRequest(
                List.of(transaction), FILE_NAME, FILE_HASH, 10L
        );

        Set<ConstraintViolation<SaveTransactionRequest>> violations = validator().validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    /** {@code transactions} must be non-null and non-empty -- both fail with the same "must not be empty" message. */
    @Nested
    @DisplayName("Field: transactions")
    class TransactionsValidationTests {
        @Test
        @DisplayName("should fail when transactions list is null")
        void shouldFailWhenTransactionsIsNull() {
            SaveTransactionRequest request = new SaveTransactionRequest(null, FILE_NAME, FILE_HASH, 10L);
            Set<ConstraintViolation<SaveTransactionRequest>> violations = validator().validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Transactions list must not be empty".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when transactions list is empty")
        void shouldFailWhenTransactionsIsEmpty() {
            SaveTransactionRequest request = new SaveTransactionRequest(Collections.emptyList(), FILE_NAME, FILE_HASH, 10L);
            Set<ConstraintViolation<SaveTransactionRequest>> violations = validator().validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Transactions list must not be empty".equals(v.getMessage())));
        }
    }

    /** {@code fileName} must be non-blank. */
    @Nested
    @DisplayName("Field: fileName")
    class FileNameValidationTests {
        @Test
        @DisplayName("should fail when fileName is blank")
        void shouldFailWhenFileNameIsBlank() {
            SaveTransactionRequest request = new SaveTransactionRequest(List.of(TransactionDto.builder().id(1L).build()), "", FILE_HASH, 10L);
            Set<ConstraintViolation<SaveTransactionRequest>> violations = validator().validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "File name cannot be blank".equals(v.getMessage())));
        }
    }

    /** {@code fileHash} must be non-blank -- it's the value the import pipeline uses to detect a re-uploaded duplicate file. */
    @Nested
    @DisplayName("Field: fileHash")
    class FileHashValidationTests {
        @Test
        @DisplayName("should fail when fileHash is blank")
        void shouldFailWhenFileHashIsBlank() {
            SaveTransactionRequest request = new SaveTransactionRequest(List.of(TransactionDto.builder().id(1L).build()), FILE_NAME, "", 10L);
            Set<ConstraintViolation<SaveTransactionRequest>> violations = validator().validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "File hash cannot be blank".equals(v.getMessage())));
        }
    }

    /**
     * {@code merchantsReviewed} (PF-898) defaults to {@code false} when a client omits it, so the
     * legacy per-account save keeps its description-match fallback, and reads {@code true} when
     * the import dialog sends it.
     */
    @Nested
    @DisplayName("Field: merchantsReviewed")
    class MerchantsReviewedTests {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Test
        @DisplayName("should default to false when omitted from the json body")
        void merchantsReviewed_omitted_defaultsToFalse() throws Exception {
            // arrange
            String json = "{\"transactions\":[],\"fileName\":\"a.csv\",\"fileHash\":\"h\",\"accountId\":1}";

            // act
            SaveTransactionRequest request = objectMapper.readValue(json, SaveTransactionRequest.class);

            // assert & verify
            assertFalse(request.merchantsReviewed());
        }

        @Test
        @DisplayName("should read true when sent in the json body")
        void merchantsReviewed_sentTrue_readsTrue() throws Exception {
            // arrange
            String json = "{\"transactions\":[],\"fileName\":\"a.csv\",\"fileHash\":\"h\",\"accountId\":1,\"merchantsReviewed\":true}";

            // act
            SaveTransactionRequest request = objectMapper.readValue(json, SaveTransactionRequest.class);

            // assert & verify
            assertTrue(request.merchantsReviewed());
        }

        @Test
        @DisplayName("the legacy four-argument constructor should leave the flag false")
        void legacyConstructor_leavesFlagFalse() {
            // act
            SaveTransactionRequest request = new SaveTransactionRequest(List.of(), FILE_NAME, FILE_HASH, 10L);

            // assert & verify
            assertFalse(request.merchantsReviewed());
        }
    }
}
