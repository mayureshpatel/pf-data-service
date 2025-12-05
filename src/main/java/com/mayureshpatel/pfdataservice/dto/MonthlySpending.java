package com.mayureshpatel.pfdataservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlySpending {
    private int year;
    private int month;
    private BigDecimal total;
}
