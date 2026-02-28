package com.mayureshpatel.pfdataservice.dto.budget;

import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record BudgetStatusDto(
        CategoryDto category,
        BigDecimal budgetedAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        double percentageUsed
) {
}
