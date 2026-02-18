package com.mayureshpatel.pfdataservice.dto.budget;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Represents a budget object.
 *
 * @param id       the budget id
 * @param category the budget category
 * @param amount   the budget amount
 * @param month    the budget month
 * @param year     the budget year
 */
@Builder
public record BudgetDto(
        Long id,
        User user,
        Category category,
        BigDecimal amount,
        Integer month,
        Integer year
) {
}
