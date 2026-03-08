package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;

public final class BudgetDtoMapper {

    private BudgetDtoMapper() {
    }

    public static BudgetDto toDto(Budget budget) {
        if (budget == null) {
            return null;
        }

        return new BudgetDto(
                budget.getId(),
                budget.getUserId(),
                budget.getCategory() == null ? null : CategoryDtoMapper.toDto(budget.getCategory()),
                budget.getAmount(),
                budget.getMonth(),
                budget.getYear()
        );
    }
}
