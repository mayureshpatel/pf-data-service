package com.mayureshpatel.pfdataservice.dto.category;

import java.util.List;

/**
 * The PF-900 match preview for a draft rule: counts over every matching transaction in the
 * user's ledger, plus a capped list of rows.
 *
 * @param totalMatches            how many transactions the draft matches in total
 * @param willCategorizeCount     how many of those the draft would categorize
 * @param losesToOtherRuleCount   how many uncategorized ones an earlier-ranked rule would take
 * @param alreadyCategorizedCount how many already have a category
 * @param truncated               true when {@code rows} holds fewer rows than {@code totalMatches}
 * @param rows                    the capped rows, will-categorize first, newest first per verdict
 */
public record RuleMatchPreviewDto(
        int totalMatches,
        int willCategorizeCount,
        int losesToOtherRuleCount,
        int alreadyCategorizedCount,
        boolean truncated,
        List<RuleMatchRowDto> rows
) {

    /**
     * @return a preview with no matches, used when the draft has no usable keyword
     */
    public static RuleMatchPreviewDto empty() {
        return new RuleMatchPreviewDto(0, 0, 0, 0, false, List.of());
    }
}
