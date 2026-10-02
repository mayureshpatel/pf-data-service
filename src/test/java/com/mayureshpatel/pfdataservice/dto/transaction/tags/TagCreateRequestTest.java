package com.mayureshpatel.pfdataservice.dto.transaction.tags;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies {@code TagCreateRequest}'s jakarta bean-validation constraints pass on a fully valid request (color included and, separately, color omitted -- it's optional) and fail correctly per-field, one {@code @Nested} class per field below. */
@DisplayName("TagCreateRequest Validation Tests")
class TagCreateRequestTest {

    private static final String TAG_NAME = "Travel";

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("should pass when all fields are valid")
    void shouldPassWithValidData() {
        TagCreateRequest request = TagCreateRequest.builder()
                .userId(1L)
                .name(TAG_NAME)
                .color("#123456")
                .build();

        Set<ConstraintViolation<TagCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Test
    @DisplayName("should pass when color is null (optional)")
    void shouldPassWithNullColor() {
        TagCreateRequest request = TagCreateRequest.builder().userId(1L).name(TAG_NAME).color(null).build();
        Set<ConstraintViolation<TagCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    /** {@code userId} must be non-null and positive. */
    @Nested
    @DisplayName("Field: userId")
    class UserIdValidationTests {
        @Test
        @DisplayName("should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            TagCreateRequest request = TagCreateRequest.builder().userId(null).name(TAG_NAME).build();
            Set<ConstraintViolation<TagCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "User ID cannot be null.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when userId is not positive")
        void shouldFailWhenUserIdIsNotPositive() {
            TagCreateRequest request = TagCreateRequest.builder().userId(0L).name(TAG_NAME).build();
            Set<ConstraintViolation<TagCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "User ID must be a positive number.".equals(v.getMessage())));
        }
    }

    /** {@code name} must be non-blank and no more than 50 characters. */
    @Nested
    @DisplayName("Field: name")
    class NameValidationTests {
        @Test
        @DisplayName("should fail when name is blank")
        void shouldFailWhenNameIsBlank() {
            TagCreateRequest request = TagCreateRequest.builder().userId(1L).name("").build();
            Set<ConstraintViolation<TagCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Name cannot be blank.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when name exceeds 50 characters")
        void shouldFailWhenNameIsTooLong() {
            TagCreateRequest request = TagCreateRequest.builder().userId(1L).name("a".repeat(51)).build();
            Set<ConstraintViolation<TagCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Name cannot exceed 50 characters.".equals(v.getMessage())));
        }
    }
}
