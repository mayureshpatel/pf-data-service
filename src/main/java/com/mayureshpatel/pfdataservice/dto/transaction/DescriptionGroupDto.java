package com.mayureshpatel.pfdataservice.dto.transaction;

import java.util.List;

/**
 * A group of the user's transaction descriptions that share the same merchant text once store
 * numbers and a trailing location are stripped ("KROGER #431 ROSWELL GA" and "KROGER #512"), as
 * listed by the merchants page's Unlinked tab so the user links once per group.
 *
 * @param key              the lowercase group key (see {@code DescriptionGroupKeys})
 * @param displayName      the key's words in the most-used variant's own case
 * @param variantCount     how many distinct descriptions are in the group
 * @param transactionCount how many non-deleted transactions use any of them
 * @param variants         the group's descriptions, most used first
 * @param similarKeys      keys of other groups that extend this one by whole words or that this
 *                         one extends ("kroger" and "kroger fuel ctr"), sorted; look-alikes to check
 *                         before linking, never merged
 */
public record DescriptionGroupDto(
        String key,
        String displayName,
        int variantCount,
        long transactionCount,
        List<TransactionDescriptionDto> variants,
        List<String> similarKeys
) {
}
