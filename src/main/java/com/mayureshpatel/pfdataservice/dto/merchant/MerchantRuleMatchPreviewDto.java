package com.mayureshpatel.pfdataservice.dto.merchant;

import java.util.List;

/**
 * The PF-905 match preview for a draft merchant rule: counts over every matching transaction in
 * the user's ledger, plus a capped list of rows.
 *
 * @param targetMerchantName   the name of the draft's target merchant, null when none was given
 * @param totalMatches         how many transactions the draft matches in total
 * @param willAssignCount      how many of those the draft would assign a merchant to
 * @param losesToOtherRuleCount how many assignable ones a higher-ranked saved rule would take
 * @param alreadyHasMerchantCount how many already have a merchant
 * @param linkedCount          how many have no merchant but an exact description link
 * @param truncated            true when {@code rows} holds fewer rows than {@code totalMatches}
 * @param rows                 the capped rows, will-assign first, newest first per verdict
 * @param willAssignIds        the ids of every transaction the draft would give a merchant, not only those in
 *                            {@code rows} (newest first, at most {@value #MAX_WILL_ASSIGN_IDS}), so the ledger
 *                            can apply the rule to all of them when more match than the rows list
 */
public record MerchantRuleMatchPreviewDto(
        String targetMerchantName,
        int totalMatches,
        int willAssignCount,
        int losesToOtherRuleCount,
        int alreadyHasMerchantCount,
        int linkedCount,
        boolean truncated,
        List<MerchantRuleMatchRowDto> rows,
        List<Long> willAssignIds
) {

    /** The most ids {@code willAssignIds} carries, the same cap one create-and-apply call accepts. */
    public static final int MAX_WILL_ASSIGN_IDS = 2000;

    /**
     * @param targetMerchantName the draft's target merchant name, may be null
     * @return a preview with no matches, used when the draft has no usable keyword
     */
    public static MerchantRuleMatchPreviewDto empty(String targetMerchantName) {
        return new MerchantRuleMatchPreviewDto(targetMerchantName, 0, 0, 0, 0, 0, false, List.of(), List.of());
    }
}
