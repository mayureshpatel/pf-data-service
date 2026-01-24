package com.mayureshpatel.pfdataservice.dto.dashboard;

import java.math.BigDecimal;

public record DashboardPulseDto(
    BigDecimal currentIncome,
    BigDecimal previousIncome,
    BigDecimal currentExpense,
    BigDecimal previousExpense,
    BigDecimal currentSavingsRate, // Percentage 0-100
    BigDecimal previousSavingsRate
) {}
