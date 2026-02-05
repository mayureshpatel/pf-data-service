package com.mayureshpatel.pfdataservice.dto.dashboard;

import java.math.BigDecimal;

/**
 * Represents pulse data for the dashboard.
 * @param currentIncome income for the current selected period
 * @param previousIncome income for the previous relative period
 * @param currentExpense expense for the current selected period
 * @param previousExpense expense for the previous relative period
 * @param currentSavingsRate savings rate for the current selected period
 * @param previousSavingsRate savings rate for the previous relative period
 */
public record DashboardPulseDto(
    BigDecimal currentIncome,
    BigDecimal previousIncome,
    BigDecimal currentExpense,
    BigDecimal previousExpense,
    BigDecimal currentSavingsRate, // Percentage 0-100
    BigDecimal previousSavingsRate
) {}
