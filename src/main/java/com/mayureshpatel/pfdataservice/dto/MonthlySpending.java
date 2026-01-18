package com.mayureshpatel.pfdataservice.dto;

import java.math.BigDecimal;

public record MonthlySpending(int year, int month, BigDecimal total) {
}
