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

/** Verifies {@link CategoryUpdateRequest}'s jakarta bean-validation constraints pass on a fully valid request and fail correctly per-field, one {@code @Nested} class per field below. */
@DisplayName("CategoryUpdateRequest Validation Tests")
class CategoryUpdateRequestTest {

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
        CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                .id(1L)
                .userId(1L)
                .name(CATEGORY_NAME)
                .type("EXPENSE")
                .color("#FF0000")
                .icon("shopping-cart")
                .parentId(2L)
                .build();

        Set<ConstraintViolation<CategoryUpdateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    /** {@code id} must be non-null and positive. */
    @Nested
    @DisplayName("Field: id")
    class IdValidationTests {
        @Test
        @DisplayName("should fail when id is null")
        void shouldFailWhenIdIsNull() {
            CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                    .id(null)
                    .userId(1L)
                    .name(CATEGORY_NAME)
                    .build();
            Set<ConstraintViolation<CategoryUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Category ID cannot be null.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when id is not positive")
        void shouldFailWhenIdIsNotPositive() {
            CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                    .id(0L)
                    .userId(1L)
                    .name(CATEGORY_NAME)
                    .build();
            Set<ConstraintViolation<CategoryUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Category ID must be a positive number.".equals(v.getMessage())));
        }
    }

    /** {@code userId} must be non-null and positive. */
    @Nested
    @DisplayName("Field: userId")
    class UserIdValidationTests {
        @Test
        @DisplayName("should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                    .id(1L)
                    .userId(null)
                    .name(CATEGORY_NAME)
                    .build();
            Set<ConstraintViolation<CategoryUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "User ID cannot be null.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when userId is not positive")
        void shouldFailWhenUserIdIsNotPositive() {
            CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                    .id(1L)
                    .userId(0L)
                    .name(CATEGORY_NAME)
                    .build();
            Set<ConstraintViolation<CategoryUpdateRequest>> violations = validator.validate(request);
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
            CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                    .id(1L)
                    .userId(1L)
                    .name("")
                    .build();
            Set<ConstraintViolation<CategoryUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Category name cannot be blank.".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when name exceeds 50 characters")
        void shouldFailWhenNameIsTooLong() {
            CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                    .id(1L)
                    .userId(1L)
                    .name("a".repeat(51))
                    .build();
            Set<ConstraintViolation<CategoryUpdateRequest>> violations = validator.validate(request);
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
            CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                    .id(1L)
                    .userId(1L)
                    .name(CATEGORY_NAME)
                    .type("a".repeat(21))
                    .build();
            Set<ConstraintViolation<CategoryUpdateRequest>> violations = validator.validate(request);
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
            CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                    .id(1L)
                    .userId(1L)
                    .name(CATEGORY_NAME)
                    .color("a".repeat(21))
                    .build();
            Set<ConstraintViolation<CategoryUpdateRequest>> violations = validator.validate(request);
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
            CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                    .id(1L)
                    .userId(1L)
                    .name(CATEGORY_NAME)
                    .icon("a".repeat(51))
                    .build();
            Set<ConstraintViolation<CategoryUpdateRequest>> violations = validator.validate(request);
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
            CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                    .id(1L)
                    .userId(1L)
                    .name(CATEGORY_NAME)
                    .parentId(0L)
                    .build();
            Set<ConstraintViolation<CategoryUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Parent ID must be a positive number.".equals(v.getMessage())));
        }
    }
}
