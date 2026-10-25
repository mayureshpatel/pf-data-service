package com.mayureshpatel.pfdataservice.dto.report.explore;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The query parameters shared by the explorer's trend and transaction-list endpoints: the same measure,
 * period and stacked filters as {@link ExploreRequest}, without a grouping.
 *
 * @param measure       what to measure (spending, income, net, savings rate, count, average)
 * @param side          which side count and average look at (default spending)
 * @param startDate     the first day, inclusive
 * @param endDate       the last day, inclusive (a range past today is treated as in progress)
 * @param compare       which earlier period to compare with (default none; the transaction list ignores it)
 * @param accountIds    restrict to these accounts
 * @param categoryId    restrict to a category (a parent includes its subcategories)
 * @param uncategorized restrict to transactions with no category
 * @param merchantId    restrict to a merchant
 * @param noMerchant    restrict to transactions with no merchant
 * @param tagId         restrict to transactions carrying a tag
 * @param noTag         restrict to transactions carrying no tag
 * @param minAmount     smallest amount to include
 * @param maxAmount     largest amount to include
 */
public record ExploreScopeRequest(
        @NotNull ExploreMeasure measure,
        ExploreSide side,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        ExploreCompare compare,
        List<Long> accountIds,
        Long categoryId,
        Boolean uncategorized,
        Long merchantId,
        Boolean noMerchant,
        Long tagId,
        Boolean noTag,
        BigDecimal minAmount,
        BigDecimal maxAmount
) {

    /**
     * @return the side, defaulting to spending
     */
    public ExploreSide sideOrDefault() {
        return side == null ? ExploreSide.SPENDING : side;
    }

    /**
     * @return the comparison, defaulting to none
     */
    public ExploreCompare compareOrDefault() {
        return compare == null ? ExploreCompare.NONE : compare;
    }

    /**
     * @return the stacked filters the request describes
     */
    public ExploreFilter toFilter() {
        return ExploreFilter.of(accountIds, categoryId, uncategorized, merchantId, noMerchant, tagId, noTag, minAmount, maxAmount);
    }
}
