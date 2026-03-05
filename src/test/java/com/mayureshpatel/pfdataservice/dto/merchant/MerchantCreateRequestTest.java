package com.mayureshpatel.pfdataservice.dto.merchant;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MerchantCreateRequest Validation Tests")
class MerchantCreateRequestTest {

    private Validator validator;

    @BeforeEach
    void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("should pass validation with valid data")
    void shouldPassWithValidData() {
        MerchantCreateRequest request = MerchantCreateRequest.builder()
                .userId(1L)
                .originalName("Starbucks")
                .cleanName("Starbucks")
                .build();

        Set<ConstraintViolation<MerchantCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Nested
    @DisplayName("Field: userId")
    class UserIdValidationTests {
        @Test
        @DisplayName("should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(null)
                    .originalName("Starbucks")
                    .cleanName("Starbucks")
                    .build();

            Set<ConstraintViolation<MerchantCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("User ID cannot be null.")));
        }

        @Test
        @DisplayName("should fail when userId is not positive")
        void shouldFailWhenUserIdIsNotPositive() {
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(0L)
                    .originalName("Starbucks")
                    .cleanName("Starbucks")
                    .build();

            Set<ConstraintViolation<MerchantCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("User ID must be a positive number.")));
        }
    }

    @Nested
    @DisplayName("Field: originalName")
    class OriginalNameValidationTests {
        @Test
        @DisplayName("should fail when originalName is blank")
        void shouldFailWhenOriginalNameIsBlank() {
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(1L)
                    .originalName("")
                    .cleanName("Starbucks")
                    .build();

            Set<ConstraintViolation<MerchantCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Merchant name cannot be blank.")));
        }

        @Test
        @DisplayName("should fail when originalName exceeds 255 characters")
        void shouldFailWhenOriginalNameIsTooLong() {
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(1L)
                    .originalName("A".repeat(256))
                    .cleanName("Starbucks")
                    .build();

            Set<ConstraintViolation<MerchantCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Merchant name must be less than 255 characters.")));
        }
    }

    @Nested
    @DisplayName("Field: cleanName")
    class CleanNameValidationTests {
        @Test
        @DisplayName("should fail when cleanName is blank")
        void shouldFailWhenCleanNameIsBlank() {
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(1L)
                    .originalName("Starbucks")
                    .cleanName("")
                    .build();

            Set<ConstraintViolation<MerchantCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Merchant name cannot be blank.")));
        }

        @Test
        @DisplayName("should fail when cleanName exceeds 255 characters")
        void shouldFailWhenCleanNameIsTooLong() {
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(1L)
                    .originalName("Starbucks")
                    .cleanName("A".repeat(256))
                    .build();

            Set<ConstraintViolation<MerchantCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Merchant name must be less than 255 characters.")));
        }
    }

    @Test
    @DisplayName("should test all-args constructor and getters")
    void testAllArgsConstructorAndGetters() {
        MerchantCreateRequest request = new MerchantCreateRequest(1L, "Original", "Clean");
        assertEquals(1L, request.getUserId());
        assertEquals("Original", request.getOriginalName());
        assertEquals("Clean", request.getCleanName());
    }

    @Test
    @DisplayName("should test default constructor")
    void testDefaultConstructor() {
        MerchantCreateRequest request = new MerchantCreateRequest();
        assertNull(request.getUserId());
        assertNull(request.getOriginalName());
        assertNull(request.getCleanName());
    }

    @Test
    @DisplayName("should test toBuilder")
    void testToBuilder() {
        MerchantCreateRequest request = MerchantCreateRequest.builder()
                .userId(1L)
                .originalName("Original")
                .cleanName("Clean")
                .build();

        MerchantCreateRequest updated = request.toBuilder().cleanName("Updated").build();
        assertEquals(1L, updated.getUserId());
        assertEquals("Original", updated.getOriginalName());
        assertEquals("Updated", updated.getCleanName());
    }

    @Test
    @DisplayName("should test toString")
    void testToString() {
        MerchantCreateRequest request = MerchantCreateRequest.builder()
                .userId(1L)
                .originalName("Original")
                .cleanName("Clean")
                .build();

        assertNotNull(request.toString());
    }
}
