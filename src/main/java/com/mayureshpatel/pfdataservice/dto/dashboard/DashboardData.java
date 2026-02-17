package com.mayureshpatel.pfdataservice.dto.dashboard;

import com.mayureshpatel.pfdataservice.dto.category.CategoryTotal;
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
