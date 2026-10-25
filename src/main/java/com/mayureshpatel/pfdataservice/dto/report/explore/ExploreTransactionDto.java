package com.mayureshpatel.pfdataservice.dto.report.explore;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One transaction behind an explorer number, the last step of a drill-down.
 *
 * @param id           the transaction id
 * @param date         the transaction day (UTC)
 * @param description  the raw bank description
 * @param merchantName the merchant's name, or null when it has none
 * @param accountName  the account's name
 * @param categoryName the category's name, or null when it has none
 * @param amount       the amount
 * @param type         income or expense
 */
public record ExploreTransactionDto(Long id, LocalDate date, String description, String merchantName, String accountName,
                                    String categoryName, BigDecimal amount, TransactionType type) {
}
