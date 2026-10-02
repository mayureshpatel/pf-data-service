package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;

/** Converts the {@link Budget} domain object into its API-facing {@link BudgetDto}. */
public final class BudgetDtoMapper {

    private BudgetDtoMapper() {
    }

    /**
     * @param budget the domain budget to convert, or {@code null}
     * @return the equivalent DTO, or {@code null} if {@code budget} was {@code null}
     */
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
