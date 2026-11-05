package com.mayureshpatel.pfdataservice.dto.merchant;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * One change applying the user's merchant rules would make to an existing transaction (PF-907):
 * a merchant-less transaction that would get a merchant, and the rule that decided it.
 *
 * @param transactionId the transaction that would change
 * @param date          the transaction date
 * @param description   the raw description the rules were matched against
 * @param amount        the signed transaction amount
 * @param accountName   the name of the transaction's account, may be null
 * @param merchant      the merchant that would be assigned (id and name)
 * @param rule          the highest-ranked rule that decided it
 */
public record MerchantRuleChangePreviewDto(
        Long transactionId,
        OffsetDateTime date,
        String description,
        BigDecimal amount,
        String accountName,
        MerchantDto merchant,
        MerchantRuleRefDto rule
) {
}
