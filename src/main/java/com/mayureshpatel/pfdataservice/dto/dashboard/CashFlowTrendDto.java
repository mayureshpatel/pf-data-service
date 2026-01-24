package com.mayureshpatel.pfdataservice.dto.dashboard;

import java.math.BigDecimal;

public record CashFlowTrendDto(
    int month,
    int year,
    BigDecimal income,
    BigDecimal expense
) {}
