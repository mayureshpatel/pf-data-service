package com.mayureshpatel.pfdataservice.dto.transaction.fileimport;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@code FileImportCreateRequest}'s jakarta bean-validation constraints pass on a fully valid request and fail correctly per-field, one {@code @Nested} class per field below. */
@DisplayName("FileImportCreateRequest Validation Tests")
class FileImportCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private FileImportCreateRequest.FileImportCreateRequestBuilder createValidBuilder() {
        return FileImportCreateRequest.builder()
                .accountId("1")
                .fileName("test.csv")
                .fileHash("hash123")
                .fileContent("content");
    }

    @Test
    @DisplayName("should pass when all fields are valid")
    void shouldPassWithValidData() {
        FileImportCreateRequest request = createValidBuilder().build();
        Set<ConstraintViolation<FileImportCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    /** {@code accountId} must be non-blank -- unlike other DTOs' numeric account ids, this one is carried as a string. */
    @Nested
    @DisplayName("Field: accountId")
    class AccountIdValidationTests {
        @Test
        @DisplayName("should fail when accountId is blank")
        void shouldFailWhenAccountIdIsBlank() {
            FileImportCreateRequest request = createValidBuilder().accountId("").build();
            Set<ConstraintViolation<FileImportCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Account ID cannot be blank".equals(v.getMessage())));
        }
    }

    /** {@code fileName} must be non-blank and no more than 255 characters. */
    @Nested
    @DisplayName("Field: fileName")
    class FileNameValidationTests {
        @Test
        @DisplayName("should fail when fileName is blank")
        void shouldFailWhenFileNameIsBlank() {
            FileImportCreateRequest request = createValidBuilder().fileName("").build();
            Set<ConstraintViolation<FileImportCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "File name cannot be blank".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when fileName exceeds 255 characters")
        void shouldFailWhenFileNameIsTooLong() {
            FileImportCreateRequest request = createValidBuilder().fileName("a".repeat(256)).build();
            Set<ConstraintViolation<FileImportCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "File name must be less than 255 characters".equals(v.getMessage())));
        }
    }

    /** {@code fileHash} must be non-blank and no more than 64 characters (long enough for a hex-encoded SHA-256 digest). */
    @Nested
    @DisplayName("Field: fileHash")
    class FileHashValidationTests {
        @Test
        @DisplayName("should fail when fileHash is blank")
        void shouldFailWhenFileHashIsBlank() {
            FileImportCreateRequest request = createValidBuilder().fileHash("").build();
            Set<ConstraintViolation<FileImportCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "File hash cannot be blank".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when fileHash exceeds 64 characters")
        void shouldFailWhenFileHashIsTooLong() {
            FileImportCreateRequest request = createValidBuilder().fileHash("a".repeat(65)).build();
            Set<ConstraintViolation<FileImportCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "File hash must be less than 64 characters".equals(v.getMessage())));
        }
    }

    /** {@code fileContent} must be non-blank -- the raw uploaded file body, carried inline on the request rather than as a separate multipart part. */
    @Nested
    @DisplayName("Field: fileContent")
    class FileContentValidationTests {
        @Test
        @DisplayName("should fail when fileContent is blank")
        void shouldFailWhenFileContentIsBlank() {
            FileImportCreateRequest request = createValidBuilder().fileContent("").build();
            Set<ConstraintViolation<FileImportCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "File content cannot be blank".equals(v.getMessage())));
        }
    }
}
