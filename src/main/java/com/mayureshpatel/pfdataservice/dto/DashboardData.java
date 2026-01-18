package com.mayureshpatel.pfdataservice.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record DashboardData(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netSavings,
        List<CategoryTotal> categoryBreakdown
) {
}
