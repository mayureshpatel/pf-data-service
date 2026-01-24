package com.mayureshpatel.pfdataservice.dto.dashboard;

import java.math.BigDecimal;

public record YtdSummaryDto(
    int year,
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal netSavings,
    BigDecimal avgSavingsRate
) {}
