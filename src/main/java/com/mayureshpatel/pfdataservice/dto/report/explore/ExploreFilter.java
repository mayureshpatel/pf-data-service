package com.mayureshpatel.pfdataservice.dto.report.explore;

import java.math.BigDecimal;
import java.util.List;

/**
 * The filters the explorer stacks as the user drills. Empty lists, nulls and false flags mean "no
 * restriction". A category filter on a parent also matches its subcategories.
 *
 * @param accountIds    restrict to these accounts (empty for all)
 * @param categoryId    restrict to a category (a parent includes its subcategories)
 * @param uncategorized restrict to transactions with no category
 * @param merchantId    restrict to a merchant
 * @param noMerchant    restrict to transactions with no merchant
 * @param tagId         restrict to transactions carrying a tag
 * @param noTag         restrict to transactions carrying no tag
 * @param minAmount     smallest amount to include
 * @param maxAmount     largest amount to include
 */
public record ExploreFilter(
        List<Long> accountIds,
        Long categoryId,
        boolean uncategorized,
        Long merchantId,
        boolean noMerchant,
        Long tagId,
        boolean noTag,
        BigDecimal minAmount,
        BigDecimal maxAmount
) {

    /**
     * @return a filter that restricts nothing
     */
    public static ExploreFilter none() {
        return new ExploreFilter(List.of(), null, false, null, false, null, false, null, null);
    }
}
