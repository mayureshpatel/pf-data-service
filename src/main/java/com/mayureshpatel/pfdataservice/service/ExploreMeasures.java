package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreMeasure;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreSide;
import com.mayureshpatel.pfdataservice.repository.transaction.ExploreRepository.ExploreRowData;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Turns a group's income, expense and counts into the value of any explorer measure, and says whether
 * the group has anything to show for it. Definitions match the Dashboard: spending is expense, income is
 * income, net is income minus spending, and the savings rate is net over income.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ExploreMeasures {

    private static final int RATE_SCALE = 4;
    private static final int AVERAGE_SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /**
     * @param measure the measure
     * @param side    the side count and average look at
     * @param data    the group's totals
     * @return the measure's value, or null when it has none (a savings rate without income, an average without transactions)
     */
    public static BigDecimal value(ExploreMeasure measure, ExploreSide side, ExploreRowData data) {
        return switch (measure) {
            case SPENDING -> data.expense();
            case INCOME -> data.income();
            case NET -> data.income().subtract(data.expense());
            case SAVINGS_RATE -> savingsRate(data);
            case COUNT -> BigDecimal.valueOf(sideCount(side, data));
            case AVERAGE -> average(side, data);
        };
    }

    /**
     * @param measure the measure
     * @param side    the side count and average look at
     * @param data    the group's totals
     * @return whether the group has any transaction on the side the measure looks at
     */
    public static boolean hasActivity(ExploreMeasure measure, ExploreSide side, ExploreRowData data) {
        return count(measure, side, data) > 0;
    }

    /**
     * @param measure the measure
     * @param side    the side count and average look at
     * @param data    the group's totals
     * @return how many transactions the measure covers
     */
    public static long count(ExploreMeasure measure, ExploreSide side, ExploreRowData data) {
        return switch (measure) {
            case SPENDING -> data.expenseCount();
            case INCOME -> data.incomeCount();
            case NET, SAVINGS_RATE -> data.incomeCount() + data.expenseCount();
            case COUNT, AVERAGE -> sideCount(side, data);
        };
    }

    /**
     * @param rows groups to add up
     * @return one group holding the sums of all of them
     */
    public static ExploreRowData sum(List<ExploreRowData> rows) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        long incomeCount = 0;
        long expenseCount = 0;
        for (ExploreRowData row : rows) {
            income = income.add(row.income());
            expense = expense.add(row.expense());
            incomeCount += row.incomeCount();
            expenseCount += row.expenseCount();
        }
        return new ExploreRowData(null, "Total", income, expense, incomeCount, expenseCount);
    }

    private static long sideCount(ExploreSide side, ExploreRowData data) {
        return side == ExploreSide.INCOME ? data.incomeCount() : data.expenseCount();
    }

    private static BigDecimal savingsRate(ExploreRowData data) {
        if (data.income().signum() == 0) {
            return null;
        }
        return data.income().subtract(data.expense())
                .divide(data.income(), RATE_SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
    }

    private static BigDecimal average(ExploreSide side, ExploreRowData data) {
        long count = sideCount(side, data);
        if (count == 0) {
            return null;
        }
        BigDecimal total = side == ExploreSide.INCOME ? data.income() : data.expense();
        return total.divide(BigDecimal.valueOf(count), AVERAGE_SCALE, RoundingMode.HALF_UP);
    }
}
