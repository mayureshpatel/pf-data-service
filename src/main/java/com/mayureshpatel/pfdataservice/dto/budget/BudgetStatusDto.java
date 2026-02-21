package com.mayureshpatel.pfdataservice.dto.budget;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record BudgetStatusDto(
        Category category,
        BigDecimal budgetedAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        double percentageUsed
) {
}
