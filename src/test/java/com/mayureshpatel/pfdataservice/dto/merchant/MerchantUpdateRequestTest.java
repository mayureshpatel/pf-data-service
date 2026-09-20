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

@DisplayName("MerchantUpdateRequest Validation Tests")
class MerchantUpdateRequestTest {

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
                .name("Starbucks")
                .build();

        Set<ConstraintViolation<MerchantUpdateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Nested
    @DisplayName("Field: id")
    class IdValidationTests {
        @Test
        @DisplayName("should fail when id is null")
        void shouldFailWhenIdIsNull() {
            MerchantUpdateRequest request = MerchantUpdateRequest.builder()
                    .id(null)
                    .name("Starbucks")
                    .build();

            Set<ConstraintViolation<MerchantUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Merchant ID cannot be null.")));
        }

        @Test
        @DisplayName("should fail when id is not positive")
        void shouldFailWhenIdIsNotPositive() {
            MerchantUpdateRequest request = MerchantUpdateRequest.builder()
                    .id(0L)
                    .name("Starbucks")
                    .build();

            Set<ConstraintViolation<MerchantUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Merchant ID must be a positive number.")));
        }
    }

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
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Merchant name cannot be blank.")));
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
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Merchant name must be less than 255 characters.")));
        }
    }

    @Test
    @DisplayName("should test all-args constructor and getters")
    void testAllArgsConstructorAndGetters() {
        MerchantUpdateRequest request = new MerchantUpdateRequest(1L, "Starbucks", "Atlanta", "GA", "30301", "USA");
        assertEquals(1L, request.getId());
        assertEquals("Starbucks", request.getName());
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
                .name("Starbucks")
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
                .name("Starbucks")
                .build();

        assertNotNull(request.toString());
    }
}
