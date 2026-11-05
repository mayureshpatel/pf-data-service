package com.mayureshpatel.pfdataservice.dto.merchant;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * One transaction a draft merchant rule matches in the PF-905 match preview, with its verdict
 * and the saved rules that also match it.
 *
 * @param transactionId   the transaction id
 * @param date            the transaction date
 * @param description     the raw description the rule was matched against
 * @param amount          the signed transaction amount
 * @param accountName     the name of the transaction's account, may be null
 * @param currentMerchant the transaction's current merchant, null when it has none
 * @param verdict         what the draft would do to this transaction
 * @param otherRules      the saved rules that also match, in effective (priority) order
 * @param winningRule     the saved rule that beats the draft, set only for {@code LOSES_TO_OTHER_RULE}
 */
public record MerchantRuleMatchRowDto(
        Long transactionId,
        OffsetDateTime date,
        String description,
        BigDecimal amount,
        String accountName,
        MerchantDto currentMerchant,
        MerchantRuleVerdict verdict,
        List<MerchantRuleRefDto> otherRules,
        MerchantRuleRefDto winningRule
) {
}
