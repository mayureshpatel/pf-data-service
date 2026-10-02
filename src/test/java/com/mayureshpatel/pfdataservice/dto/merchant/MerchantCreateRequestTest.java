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

/** Verifies {@link MerchantCreateRequest}'s jakarta bean-validation constraints, its builder/all-args/no-args construction paths, {@code toBuilder}, and {@code toString}. Post-PF-845, a merchant is identified purely by its own {@code name}/{@code city}/{@code state}/{@code postalCode}/{@code country} fields -- there's no clean-name/original-name split to validate here. */
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
                .name("Starbucks")
                .build();

        Set<ConstraintViolation<MerchantCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("should pass validation with location fields populated")
    void shouldPassWithLocationFields() {
        MerchantCreateRequest request = MerchantCreateRequest.builder()
                .userId(1L)
                .name("Starbucks")
                .city("Atlanta")
                .state("GA")
                .postalCode("30301")
                .country("USA")
                .build();

        Set<ConstraintViolation<MerchantCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    /** {@code userId} must be non-null and positive. */
    @Nested
    @DisplayName("Field: userId")
    class UserIdValidationTests {
        @Test
        @DisplayName("should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(null)
                    .name("Starbucks")
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
                    .name("Starbucks")
                    .build();

            Set<ConstraintViolation<MerchantCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("User ID must be a positive number.")));
        }
    }

    /** {@code name} must be non-blank (a PF-845 regression case: unlike the pre-PF-845 shape, this codebase no longer derives a display name from an unstructured description, so a merchant is always deliberately named) and no more than 255 characters. */
    @Nested
    @DisplayName("Field: name")
    class NameValidationTests {
        @Test
        @DisplayName("PF-845: should fail when name is blank -- a merchant is always deliberately named")
        void shouldFailWhenNameIsBlank() {
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(1L)
                    .name("")
                    .build();

            Set<ConstraintViolation<MerchantCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Merchant name cannot be blank.")));
        }

        @Test
        @DisplayName("should fail when name exceeds 255 characters")
        void shouldFailWhenNameIsTooLong() {
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(1L)
                    .name("A".repeat(256))
                    .build();

            Set<ConstraintViolation<MerchantCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Merchant name must be less than 255 characters.")));
        }
    }

    /** {@code city} (<=120 characters) and {@code postalCode} (<=20 characters) are both optional but length-capped when present; {@code state}/{@code country} carry no length constraint of their own. */
    @Nested
    @DisplayName("Field: location")
    class LocationValidationTests {
        @Test
        @DisplayName("should fail when city exceeds 120 characters")
        void shouldFailWhenCityIsTooLong() {
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(1L)
                    .name("Starbucks")
                    .city("A".repeat(121))
                    .build();

            Set<ConstraintViolation<MerchantCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("City must be less than 120 characters.")));
        }

        @Test
        @DisplayName("should fail when postalCode exceeds 20 characters")
        void shouldFailWhenPostalCodeIsTooLong() {
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(1L)
                    .name("Starbucks")
                    .postalCode("A".repeat(21))
                    .build();

            Set<ConstraintViolation<MerchantCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Postal code must be less than 20 characters.")));
        }
    }

    @Test
    @DisplayName("should test all-args constructor and getters")
    void testAllArgsConstructorAndGetters() {
        MerchantCreateRequest request = new MerchantCreateRequest(1L, "Starbucks", "Atlanta", "GA", "30301", "USA");
        assertEquals(1L, request.getUserId());
        assertEquals("Starbucks", request.getName());
        assertEquals("Atlanta", request.getCity());
        assertEquals("GA", request.getState());
        assertEquals("30301", request.getPostalCode());
        assertEquals("USA", request.getCountry());
    }

    @Test
    @DisplayName("should test default constructor")
    void testDefaultConstructor() {
        MerchantCreateRequest request = new MerchantCreateRequest();
        assertNull(request.getUserId());
        assertNull(request.getName());
        assertNull(request.getCity());
        assertNull(request.getState());
        assertNull(request.getPostalCode());
        assertNull(request.getCountry());
    }

    @Test
    @DisplayName("should test toBuilder")
    void testToBuilder() {
        MerchantCreateRequest request = MerchantCreateRequest.builder()
                .userId(1L)
                .name("Starbucks")
                .build();

        MerchantCreateRequest updated = request.toBuilder().name("Starbucks (Downtown)").build();
        assertEquals(1L, updated.getUserId());
        assertEquals("Starbucks (Downtown)", updated.getName());
    }

    @Test
    @DisplayName("should test toString")
    void testToString() {
        MerchantCreateRequest request = MerchantCreateRequest.builder()
                .userId(1L)
                .name("Starbucks")
                .build();

        assertNotNull(request.toString());
    }
}
