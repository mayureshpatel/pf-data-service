package com.mayureshpatel.pfdataservice.dto.budget;

import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Represents a budget object.
 *
 * @param id       the budget id
 * @param userId   the budget user
 * @param category the budget category
 * @param amount   the budget amount
 * @param month    the budget month
 * @param year     the budget year
 */
@Builder
public record BudgetDto(
        Long id,
        Long userId,
        
        @NotNull(message = "Category is required")
        CategoryDto category,
        
        @NotNull(message = "Amount is required")
        @PositiveOrZero(message = "Amount must be zero or positive")
        BigDecimal amount,
        
        @NotNull(message = "Month is required")
        @Min(value = 1, message = "Month must be between 1 and 12")
        @Max(value = 12, message = "Month must be between 1 and 12")
        Integer month,
        
        @NotNull(message = "Year is required")
        @Min(value = 1900, message = "Year must be valid")
        Integer year
) {
}

