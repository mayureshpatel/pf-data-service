package com.mayureshpatel.pfdataservice.dto.dashboard;

import java.math.BigDecimal;

public record MonthlySpending(int year, int month, BigDecimal total) {
}
