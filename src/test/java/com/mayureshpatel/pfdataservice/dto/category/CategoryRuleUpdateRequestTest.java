package com.mayureshpatel.pfdataservice.dto.category;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CategoryRuleUpdateRequest Validation Tests")
class CategoryRuleUpdateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("should pass when all fields are valid")
    void shouldPassWithValidData() {
        CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                .id(1L)
                .categoryId(1L)
                .keywords(List.of("PUBLIX"))
                .priority(1)
                .build();

        Set<ConstraintViolation<CategoryRuleUpdateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations");
    }

    @Nested
    @DisplayName("Field: id")
    class IdValidationTests {
        @Test
        @DisplayName("should fail when id is null")
        void shouldFailWhenIdIsNull() {
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .id(null)
                    .categoryId(1L)
                    .keywords(List.of("PUBLIX"))
                    .build();
            Set<ConstraintViolation<CategoryRuleUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Rule ID cannot be null.")));
        }

        @Test
        @DisplayName("should fail when id is not positive")
        void shouldFailWhenIdIsNotPositive() {
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .id(0L)
                    .categoryId(1L)
                    .keywords(List.of("PUBLIX"))
                    .build();
            Set<ConstraintViolation<CategoryRuleUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Rule ID must be a positive number.")));
        }
    }

    @Nested
    @DisplayName("Field: categoryId")
    class CategoryIdValidationTests {
        @Test
        @DisplayName("should fail when categoryId is null")
        void shouldFailWhenCategoryIdIsNull() {
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .id(1L)
                    .categoryId(null)
                    .keywords(List.of("PUBLIX"))
                    .build();
            Set<ConstraintViolation<CategoryRuleUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Category ID cannot be null.")));
        }

        @Test
        @DisplayName("should fail when categoryId is not positive")
        void shouldFailWhenCategoryIdIsNotPositive() {
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .id(1L)
                    .categoryId(0L)
                    .keywords(List.of("PUBLIX"))
                    .build();
            Set<ConstraintViolation<CategoryRuleUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Category ID must be a positive number.")));
        }
    }

    @Nested
    @DisplayName("Field: keywords (PF-315)")
    class KeywordsValidationTests {
        @Test
        @DisplayName("should fail when keywords is empty")
        void shouldFailWhenKeywordsIsEmpty() {
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .id(1L)
                    .categoryId(1L)
                    .keywords(List.of())
                    .build();
            Set<ConstraintViolation<CategoryRuleUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("At least one keyword is required.")));
        }

        @Test
        @DisplayName("should fail when a keyword in the list is blank")
        void shouldFailWhenAKeywordIsBlank() {
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .id(1L)
                    .categoryId(1L)
                    .keywords(List.of("PUBLIX", ""))
                    .build();
            Set<ConstraintViolation<CategoryRuleUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Keyword cannot be blank.")));
        }

        @Test
        @DisplayName("should fail when a keyword in the list exceeds 255 characters")
        void shouldFailWhenAKeywordIsTooLong() {
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .id(1L)
                    .categoryId(1L)
                    .keywords(List.of("a".repeat(256)))
                    .build();
            Set<ConstraintViolation<CategoryRuleUpdateRequest>> violations = validator.validate(request);
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
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .id(1L)
                    .categoryId(1L)
                    .keywords(List.of("PUBLIX"))
                    .priority(-1)
                    .build();
            Set<ConstraintViolation<CategoryRuleUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Priority must be a positive number or zero.")));
        }
    }

    @Nested
    @DisplayName("Field: minAmount / maxAmount (PF-314)")
    class AmountRangeValidationTests {
        @Test
        @DisplayName("should pass when both are null (no range set)")
        void shouldPassWhenBothAmountsAreNull() {
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .id(1L)
                    .categoryId(1L)
                    .keywords(List.of("PUBLIX"))
                    .minAmount(null)
                    .maxAmount(null)
                    .build();
            Set<ConstraintViolation<CategoryRuleUpdateRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("should fail when minAmount is negative")
        void shouldFailWhenMinAmountIsNegative() {
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .id(1L)
                    .categoryId(1L)
                    .keywords(List.of("PUBLIX"))
                    .minAmount(new java.math.BigDecimal("-0.01"))
                    .build();
            Set<ConstraintViolation<CategoryRuleUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Minimum amount must be a positive number or zero.")));
        }

        @Test
        @DisplayName("should fail when maxAmount is negative")
        void shouldFailWhenMaxAmountIsNegative() {
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .id(1L)
                    .categoryId(1L)
                    .keywords(List.of("PUBLIX"))
                    .maxAmount(new java.math.BigDecimal("-0.01"))
                    .build();
            Set<ConstraintViolation<CategoryRuleUpdateRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Maximum amount must be a positive number or zero.")));
        }
    }
}
