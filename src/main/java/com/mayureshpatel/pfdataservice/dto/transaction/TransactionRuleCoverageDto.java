package com.mayureshpatel.pfdataservice.dto.transaction;

/**
 * Whether the user's saved rules already cover one transaction's description (PF-919): one flag
 * per kind of rule, so the ledger can mark the rows that still need a rule without asking per row.
 *
 * @param transactionId       the transaction id
 * @param categoryRuleCovers  true if at least one category rule matches the description and amount
 * @param merchantRuleCovers  true if at least one merchant rule matches the description and amount
 */
public record TransactionRuleCoverageDto(
        Long transactionId,
        boolean categoryRuleCovers,
        boolean merchantRuleCovers
) {
}
