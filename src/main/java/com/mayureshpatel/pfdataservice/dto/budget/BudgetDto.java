package com.mayureshpatel.pfdataservice.dto.budget;

import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
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
        CategoryDto category,
        BigDecimal amount,
        Integer month,
        Integer year
) {

    /**
     * Maps a {@link Budget} to a {@link BudgetDto}
     *
     * @param budget the budget to map
     * @return the mapped {@link BudgetDto}
     */
    public static BudgetDto mapToDto(Budget budget) {
        return BudgetDto.builder()
                .id(budget.getId())
                .category(budget.getCategory())
                .amount(budget.getAmount())
                .month(budget.getMonth())
                .year(budget.getYear())
                .build();
    }
}
