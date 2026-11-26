package com.mayureshpatel.pfdataservice.dto.transaction;

import java.math.BigDecimal;

/**
 * One live transaction reduced to what a rule matches on, so counting rule matches over a whole
 * ledger reads two columns instead of whole transactions (PF-981).
 *
 * @param description the raw description
 * @param amount      the signed amount
 */
public record DescriptionAmountDto(String description, BigDecimal amount) {
}
