package com.mayureshpatel.pfdataservice.dto.budget;

import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
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
        CategoryDto category,
        BigDecimal amount,
        Integer month,
        Integer year
) {
}
