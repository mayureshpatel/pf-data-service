package com.mayureshpatel.pfdataservice.dto.budget;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record BudgetStatusDto(
    Long categoryId,
    String categoryName,
    BigDecimal budgetedAmount,
    BigDecimal spentAmount,
    BigDecimal remainingAmount,
    double percentageUsed
) {}
