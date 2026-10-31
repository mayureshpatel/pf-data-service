package com.mayureshpatel.pfdataservice.dto.report.networth;

import java.math.BigDecimal;

/**
 * What moved net worth over the range. {@code saved + other} always equals {@code change}.
 *
 * @param change the last point's net worth minus the opening point's
 * @param saved  income minus spending on the included accounts
 * @param other  the remainder: growth, interest and adjustments
 */
public record NetWorthMovedDto(BigDecimal change, BigDecimal saved, BigDecimal other) {
}
