package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;

public final class BudgetDtoMapper {

    private BudgetDtoMapper() {
    }

    public static BudgetDto toDto(Budget budget) {
        if (budget == null) {
            return null;
        }

        CategoryDto category = null;
        if (budget.getCategoryId() != null) {
            category = CategoryDto.builder()
                    .id(budget.getCategoryId())
                    .build();
        }

        return new BudgetDto(
                budget.getId(),
                budget.getUserId(),
                category,
                budget.getAmount(),
                budget.getMonth(),
                budget.getYear()
        );
    }
}
