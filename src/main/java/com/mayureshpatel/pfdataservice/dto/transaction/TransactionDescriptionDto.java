package com.mayureshpatel.pfdataservice.dto.transaction;

/**
 * One distinct transaction description in a user's history, as listed by the merchants page's
 * description picker. Descriptions that differ only in case or whitespace are one row (the same
 * light normalization merchant description links match on).
 *
 * @param description      the most common raw spelling of the description
 * @param transactionCount how many of the user's non-deleted transactions use it
 * @param merchantId       the merchant it is linked to, or {@code null} when unlinked
 * @param merchantName     that merchant's name, or {@code null} when unlinked
 */
public record TransactionDescriptionDto(
        String description,
        long transactionCount,
        Long merchantId,
        String merchantName
) {
}
