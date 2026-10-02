package com.mayureshpatel.pfdataservice.dto.category;

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

/** Verifies {@link CategoryCreateRequest}'s jakarta bean-validation constraints pass on a fully valid request and fail correctly per-field, one {@code @Nested} class per field below. */
@DisplayName("CategoryCreateRequest Validation Tests")
class CategoryCreateRequestTest {

    private static final String CATEGORY_NAME = "Groceries";

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("should pass when all fields are valid")
    void shouldPassWithValidData() {
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .userId(1L)
                .name(CATEGORY_NAME)
                .type("EXPENSE")
                .color("#FF0000")
                .icon("shopping-cart")
                .parentId(2L)
                .build();

        Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    /** {@code userId} must be non-null and positive. */
    @Nested
    @DisplayName("Field: userId")
    class UserIdValidationTests {
        @Test
        @DisplayName("should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .userId(null)
                    .name(CATEGORY_NAME)
                    .build();
            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "User ID cannot be null.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when userId is not positive")
        void shouldFailWhenUserIdIsNotPositive() {
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .userId(0L)
                    .name(CATEGORY_NAME)
                    .build();
            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);
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
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .userId(1L)
                    .name("")
                    .build();
            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Category name cannot be blank.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when name exceeds 50 characters")
        void shouldFailWhenNameIsTooLong() {
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .userId(1L)
                    .name("a".repeat(51))
                    .build();
            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Category name must be less than 50 characters.".equals(v.getMessage())));
        }
    }

    /** {@code type} is optional but capped at 20 characters when present. */
    @Nested
    @DisplayName("Field: type")
    class TypeValidationTests {
        @Test
        @DisplayName("should fail when type exceeds 20 characters")
        void shouldFailWhenTypeIsTooLong() {
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .userId(1L)
                    .name(CATEGORY_NAME)
                    .type("a".repeat(21))
                    .build();
            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Category type must be less than 20 characters.".equals(v.getMessage())));
        }
    }

    /** {@code color} is optional but capped at 20 characters when present. */
    @Nested
    @DisplayName("Field: color")
    class ColorValidationTests {
        @Test
        @DisplayName("should fail when color exceeds 20 characters")
        void shouldFailWhenColorIsTooLong() {
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .userId(1L)
                    .name(CATEGORY_NAME)
                    .color("a".repeat(21))
                    .build();
            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Category color must be less than 20 characters.".equals(v.getMessage())));
        }
    }

    /** {@code icon} is optional but capped at 50 characters when present. */
    @Nested
    @DisplayName("Field: icon")
    class IconValidationTests {
        @Test
        @DisplayName("should fail when icon exceeds 50 characters")
        void shouldFailWhenIconIsTooLong() {
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .userId(1L)
                    .name(CATEGORY_NAME)
                    .icon("a".repeat(51))
                    .build();
            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Category icon must be less than 50 characters.".equals(v.getMessage())));
        }
    }

    /** {@code parentId} is optional (for a top-level category) but must be positive when present. */
    @Nested
    @DisplayName("Field: parentId")
    class ParentIdValidationTests {
        @Test
        @DisplayName("should fail when parentId is not positive")
        void shouldFailWhenParentIdIsNotPositive() {
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .userId(1L)
                    .name(CATEGORY_NAME)
                    .parentId(0L)
                    .build();
            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Parent ID must be a positive number.".equals(v.getMessage())));
        }
    }
}
