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

@DisplayName("CategoryCreateRequest Validation Tests")
class CategoryCreateRequestTest {

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
                .name("Groceries")
                .type("EXPENSE")
                .color("#FF0000")
                .icon("shopping-cart")
                .parentId(2L)
                .build();

        Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Nested
    @DisplayName("Field: userId")
    class UserIdValidationTests {
        @Test
        @DisplayName("should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .userId(null)
                    .name("Groceries")
                    .build();
            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("User ID cannot be null.")));
        }

        @Test
        @DisplayName("should fail when userId is not positive")
        void shouldFailWhenUserIdIsNotPositive() {
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .userId(0L)
                    .name("Groceries")
                    .build();
            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("User ID must be a positive number.")));
        }
    }

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
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Category name cannot be blank.")));
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
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Category name must be less than 50 characters.")));
        }
    }

    @Nested
    @DisplayName("Field: type")
    class TypeValidationTests {
        @Test
        @DisplayName("should fail when type exceeds 20 characters")
        void shouldFailWhenTypeIsTooLong() {
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .userId(1L)
                    .name("Groceries")
                    .type("a".repeat(21))
                    .build();
            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Category type must be less than 20 characters.")));
        }
    }

    @Nested
    @DisplayName("Field: color")
    class ColorValidationTests {
        @Test
        @DisplayName("should fail when color exceeds 20 characters")
        void shouldFailWhenColorIsTooLong() {
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .userId(1L)
                    .name("Groceries")
                    .color("a".repeat(21))
                    .build();
            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Category color must be less than 20 characters.")));
        }
    }

    @Nested
    @DisplayName("Field: icon")
    class IconValidationTests {
        @Test
        @DisplayName("should fail when icon exceeds 50 characters")
        void shouldFailWhenIconIsTooLong() {
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .userId(1L)
                    .name("Groceries")
                    .icon("a".repeat(51))
                    .build();
            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Category icon must be less than 50 characters.")));
        }
    }

    @Nested
    @DisplayName("Field: parentId")
    class ParentIdValidationTests {
        @Test
        @DisplayName("should fail when parentId is not positive")
        void shouldFailWhenParentIdIsNotPositive() {
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .userId(1L)
                    .name("Groceries")
                    .parentId(0L)
                    .build();
            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Parent ID must be a positive number.")));
        }
    }
}
