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

@DisplayName("CategoryRuleCreateRequest Validation Tests")
class CategoryRuleCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("should pass when all fields are valid")
    void shouldPassWithValidData() {
        CategoryRuleCreateRequest request = CategoryRuleCreateRequest.builder()
                .userId(1L)
                .categoryId(1L)
                .keyword("PUBLIX")
                .priority(1)
                .build();

        Set<ConstraintViolation<CategoryRuleCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Nested
    @DisplayName("Field: userId")
    class UserIdValidationTests {
        @Test
        @DisplayName("should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            CategoryRuleCreateRequest request = CategoryRuleCreateRequest.builder()
                    .userId(null)
                    .categoryId(1L)
                    .keyword("PUBLIX")
                    .build();
            Set<ConstraintViolation<CategoryRuleCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("User ID cannot be null.")));
        }

        @Test
        @DisplayName("should fail when userId is not positive")
        void shouldFailWhenUserIdIsNotPositive() {
            CategoryRuleCreateRequest request = CategoryRuleCreateRequest.builder()
                    .userId(0L)
                    .categoryId(1L)
                    .keyword("PUBLIX")
                    .build();
            Set<ConstraintViolation<CategoryRuleCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("User ID must be a positive number.")));
        }
    }

    @Nested
    @DisplayName("Field: categoryId")
    class CategoryIdValidationTests {
        @Test
        @DisplayName("should fail when categoryId is null")
        void shouldFailWhenCategoryIdIsNull() {
            CategoryRuleCreateRequest request = CategoryRuleCreateRequest.builder()
                    .userId(1L)
                    .categoryId(null)
                    .keyword("PUBLIX")
                    .build();
            Set<ConstraintViolation<CategoryRuleCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Category ID cannot be null.")));
        }

        @Test
        @DisplayName("should fail when categoryId is not positive")
        void shouldFailWhenCategoryIdIsNotPositive() {
            CategoryRuleCreateRequest request = CategoryRuleCreateRequest.builder()
                    .userId(1L)
                    .categoryId(0L)
                    .keyword("PUBLIX")
                    .build();
            Set<ConstraintViolation<CategoryRuleCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Category ID must be a positive number.")));
        }
    }

    @Nested
    @DisplayName("Field: keyword")
    class KeywordValidationTests {
        @Test
        @DisplayName("should fail when keyword is blank")
        void shouldFailWhenKeywordIsBlank() {
            CategoryRuleCreateRequest request = CategoryRuleCreateRequest.builder()
                    .userId(1L)
                    .categoryId(1L)
                    .keyword("")
                    .build();
            Set<ConstraintViolation<CategoryRuleCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Keyword cannot be blank.")));
        }

        @Test
        @DisplayName("should fail when keyword exceeds 255 characters")
        void shouldFailWhenKeywordIsTooLong() {
            CategoryRuleCreateRequest request = CategoryRuleCreateRequest.builder()
                    .userId(1L)
                    .categoryId(1L)
                    .keyword("a".repeat(256))
                    .build();
            Set<ConstraintViolation<CategoryRuleCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Keyword cannot exceed 255 characters.")));
        }
    }

    @Nested
    @DisplayName("Field: priority")
    class PriorityValidationTests {
        @Test
        @DisplayName("should fail when priority is negative")
        void shouldFailWhenPriorityIsNegative() {
            CategoryRuleCreateRequest request = CategoryRuleCreateRequest.builder()
                    .userId(1L)
                    .categoryId(1L)
                    .keyword("PUBLIX")
                    .priority(-1)
                    .build();
            Set<ConstraintViolation<CategoryRuleCreateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Priority must be a positive number or zero.")));
        }
    }
}
