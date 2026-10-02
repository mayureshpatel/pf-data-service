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

/** Verifies {@link MerchantUpdateRequest}'s jakarta bean-validation constraints, its builder/all-args/no-args construction paths, {@code toBuilder}, and {@code toString}. */
@DisplayName("MerchantUpdateRequest Validation Tests")
class MerchantUpdateRequestTest {

    private static final String MERCHANT_NAME = "Starbucks";

    private Validator validator;

    @BeforeEach
    void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("should pass validation with valid data")
    void shouldPassWithValidData() {
        MerchantUpdateRequest request = MerchantUpdateRequest.builder()
                .id(1L)
                .name(MERCHANT_NAME)
                .build();

        Set<ConstraintViolation<MerchantUpdateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    /** {@code id} must be non-null and positive. */
    @Nested
    @DisplayName("Field: id")
    class IdValidationTests {
        @Test
        @DisplayName("should fail when id is null")
        void shouldFailWhenIdIsNull() {
            MerchantUpdateRequest request = MerchantUpdateRequest.builder()
                    .id(null)
                    .name(MERCHANT_NAME)
                    .build();

            Set<ConstraintViolation<MerchantUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Merchant ID cannot be null.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when id is not positive")
        void shouldFailWhenIdIsNotPositive() {
            MerchantUpdateRequest request = MerchantUpdateRequest.builder()
                    .id(0L)
                    .name(MERCHANT_NAME)
                    .build();

            Set<ConstraintViolation<MerchantUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Merchant ID must be a positive number.".equals(v.getMessage())));
        }
    }

    /** {@code name} must be non-blank and no more than 255 characters. */
    @Nested
    @DisplayName("Field: name")
    class NameValidationTests {
        @Test
        @DisplayName("should fail when name is blank")
        void shouldFailWhenNameIsBlank() {
            MerchantUpdateRequest request = MerchantUpdateRequest.builder()
                    .id(1L)
                    .name("")
                    .build();

            Set<ConstraintViolation<MerchantUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Merchant name cannot be blank.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when name exceeds 255 characters")
        void shouldFailWhenNameIsTooLong() {
            MerchantUpdateRequest request = MerchantUpdateRequest.builder()
                    .id(1L)
                    .name("A".repeat(256))
                    .build();

            Set<ConstraintViolation<MerchantUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Merchant name must be less than 255 characters.".equals(v.getMessage())));
        }
    }

    @Test
    @DisplayName("should test all-args constructor and getters")
    void testAllArgsConstructorAndGetters() {
        MerchantUpdateRequest request = new MerchantUpdateRequest(1L, MERCHANT_NAME, "Atlanta", "GA", "30301", "USA");
        assertEquals(1L, request.getId());
        assertEquals(MERCHANT_NAME, request.getName());
        assertEquals("Atlanta", request.getCity());
        assertEquals("GA", request.getState());
        assertEquals("30301", request.getPostalCode());
        assertEquals("USA", request.getCountry());
    }

    @Test
    @DisplayName("should test default constructor")
    void testDefaultConstructor() {
        MerchantUpdateRequest request = new MerchantUpdateRequest();
        assertNull(request.getId());
        assertNull(request.getName());
        assertNull(request.getCity());
        assertNull(request.getState());
        assertNull(request.getPostalCode());
        assertNull(request.getCountry());
    }

    @Test
    @DisplayName("should test toBuilder")
    void testToBuilder() {
        MerchantUpdateRequest request = MerchantUpdateRequest.builder()
                .id(1L)
                .name(MERCHANT_NAME)
                .build();

        MerchantUpdateRequest updated = request.toBuilder().name("Starbucks (Downtown)").build();
        assertEquals(1L, updated.getId());
        assertEquals("Starbucks (Downtown)", updated.getName());
    }

    @Test
    @DisplayName("should test toString")
    void testToString() {
        MerchantUpdateRequest request = MerchantUpdateRequest.builder()
                .id(1L)
                .name(MERCHANT_NAME)
                .build();

        assertNotNull(request.toString());
    }
}
