package com.mayureshpatel.pfdataservice.dto.dashboard;

import java.math.BigDecimal;

/**
 * Represents cash flow trend data for a specific month and year.
 * @param month month of the trend data
 * @param year year of the trend data
 * @param income total income for the month
 * @param expense total expense for the month
 */
public record CashFlowTrendDto(
    int month,
    int year,
    BigDecimal income,
    BigDecimal expense
) {}
