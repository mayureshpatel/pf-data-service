package com.mayureshpatel.pfdataservice.dto.category;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * One transaction a draft rule matches in the PF-900 match preview, with its verdict and the
 * existing rules that also match it.
 *
 * @param transactionId   the transaction id
 * @param date            the transaction date
 * @param description     the raw description the rule was matched against
 * @param amount          the signed transaction amount
 * @param accountName     the name of the transaction's account, may be null
 * @param currentCategory the transaction's current category, null when uncategorized
 * @param verdict         what the draft would do to this transaction
 * @param otherRules      the existing rules that also match, in effective (priority) order
 */
public record RuleMatchRowDto(
        Long transactionId,
        OffsetDateTime date,
        String description,
        BigDecimal amount,
        String accountName,
        CategoryDto currentCategory,
        RuleVerdict verdict,
        List<RuleRefDto> otherRules
) {
}
