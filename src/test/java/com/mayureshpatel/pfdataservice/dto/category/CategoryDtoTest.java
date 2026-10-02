package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@code CategoryDto}'s record accessors map their constructor arguments 1:1, plus its own bean-validation annotations in {@link ValidationTests} -- unlike most DTOs in this codebase, this response record is itself validated, not just its request-side counterparts. */
@DisplayName("CategoryDto Tests")
class CategoryDtoTest {

    private static final String ICON = "icon";
    private static final String COLOR = "color";
    private static final String CATEGORY_NAME = "Groceries";

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("should correctly map all fields")
    void shouldPopulateFields() {
        CategoryDto parent = new CategoryDto(1L, 1L, "Parent", CategoryType.EXPENSE, null, ICON, COLOR);
        CategoryDto dto = new CategoryDto(2L, 1L, "Child", CategoryType.EXPENSE, parent, "child-icon", "child-color");

        assertEquals(2L, dto.id());
        assertEquals(1L, dto.userId());
        assertEquals("Child", dto.name());
        assertEquals(CategoryType.EXPENSE, dto.type());
        assertEquals(parent, dto.parent());
        assertEquals("child-icon", dto.icon());
        assertEquals("child-color", dto.color());
    }

    /** {@code name} (non-blank, <=50 chars), {@code type} (non-null), {@code icon} (<=50 chars), and {@code color} (<=20 chars) constraints. */
    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {
        @Test
        @DisplayName("should pass with valid data")
        void shouldPassWithValidData() {
            CategoryDto dto = new CategoryDto(1L, 1L, CATEGORY_NAME, CategoryType.EXPENSE, null, ICON, COLOR);
            Set<ConstraintViolation<CategoryDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("should fail when name is blank")
        void shouldFailWhenNameIsBlank() {
            CategoryDto dto = new CategoryDto(1L, 1L, "", CategoryType.EXPENSE, null, ICON, COLOR);
            Set<ConstraintViolation<CategoryDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Category name is required".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when name is too long")
        void shouldFailWhenNameIsTooLong() {
            CategoryDto dto = new CategoryDto(1L, 1L, "a".repeat(51), CategoryType.EXPENSE, null, ICON, COLOR);
            Set<ConstraintViolation<CategoryDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Category name must be less than 50 characters".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when type is null")
        void shouldFailWhenCategoryTypeIsNull() {
            CategoryDto dto = new CategoryDto(1L, 1L, CATEGORY_NAME, null, null, ICON, COLOR);
            Set<ConstraintViolation<CategoryDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Category type is required".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when icon is too long")
        void shouldFailWhenIconIsTooLong() {
            CategoryDto dto = new CategoryDto(1L, 1L, CATEGORY_NAME, CategoryType.EXPENSE, null, "a".repeat(51), COLOR);
            Set<ConstraintViolation<CategoryDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Icon name must be less than 50 characters".equals(v.getMessage())));
        }

        @Test
        @DisplayName("should fail when color is too long")
        void shouldFailWhenColorIsTooLong() {
            CategoryDto dto = new CategoryDto(1L, 1L, CATEGORY_NAME, CategoryType.EXPENSE, null, ICON, "a".repeat(21));
            Set<ConstraintViolation<CategoryDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> "Color must be less than 20 characters".equals(v.getMessage())));
        }
    }
}
