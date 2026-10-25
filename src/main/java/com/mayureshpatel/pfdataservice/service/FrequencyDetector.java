package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import lombok.NoArgsConstructor;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * The interval side of recurring-pattern detection: whether a date-sorted group of transactions recurs
 * at a stable gap, and which {@link Frequency} that gap is. Split out of {@link RecurringSuggestionFinder},
 * which keeps the grouping, the exclusions and the building of suggestions.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class FrequencyDetector {

    /** with this many gaps or more, a few stray gaps (a missed month, a double charge) are tolerated. */
    private static final int OUTLIER_MIN_GAPS = 8;
    private static final double OUTLIER_SHARE_REQUIRED = 0.85;

    /**
     * Determines whether a date-sorted group of transactions recurs at a stable interval.
     * "Stable" means the gaps stay within the frequency's own tolerance of the group's typical gap (all of them for a short history, 85% of them for a long one).
     * <br><br>
     * Classifies the average interval into one bucket per {@link Frequency} value (WEEKLY,
     * BI_WEEKLY, MONTHLY, QUARTERLY, YEARLY) -- there is deliberately no bucket for every possible
     * interval. A stable ~9-12 or ~17-24 day average returns {@code null}, same as an unstable
     * group: no {@link Frequency} value corresponds to those cadences, so there's nothing to
     * classify it as (PF-205). Add a new bucket here only if a new {@link Frequency} value is
     * added to model it.
     *
     * @param group the transactions to check, already sorted by date
     * @return the detected frequency, or {@code null} if the intervals aren't stable enough to
     * call recurring, or are stable but don't correspond to any supported frequency
     */
    public static Frequency detect(List<Transaction> group) {
        List<Long> intervals = computeIntervals(group);
        boolean longHistory = intervals.size() >= OUTLIER_MIN_GAPS;
        double center = longHistory ? median(intervals) : intervals.stream().mapToLong(val -> val).average().orElse(0);
        Frequency frequency = classifyFrequency(center);
        if (frequency == null) {
            return null;
        }
        double tolerance = toleranceDays(frequency);
        long within = intervals.stream().filter(i -> Math.abs(i - center) < tolerance).count();
        boolean stable = longHistory
                ? within >= Math.ceil(OUTLIER_SHARE_REQUIRED * intervals.size())
                : within == intervals.size();

        return stable ? frequency : null;
    }

    private static double median(List<Long> values) {
        List<Long> sorted = values.stream().sorted().toList();
        int mid = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(mid) : (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
    }

    /**
     * How far (in days, exclusive) a single gap may stray from the group's average gap before the
     * group stops looking recurring: tight for weekly charges, looser for quarterly and yearly ones
     * whose dates drift more with weekends and holidays.
     */
    private static double toleranceDays(Frequency frequency) {
        return switch (frequency) {
            case WEEKLY -> 2;
            case BI_WEEKLY, MONTHLY -> 5;
            case QUARTERLY -> 10;
            case YEARLY -> 14;
        };
    }

    /**
     * The day-gaps between each consecutive pair of transactions.
     */
    private static List<Long> computeIntervals(List<Transaction> group) {
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < group.size(); i++) {
            intervals.add(ChronoUnit.DAYS.between(
                    group.get(i - 1).getTransactionDate(),
                    group.get(i).getTransactionDate()));
        }
        return intervals;
    }

    /**
     * Classifies a stable average interval into one {@link Frequency} bucket. Split across two
     * methods purely to stay under PMD's own method-level complexity threshold -- the exact
     * boundary values are unchanged and still covered by their own dedicated boundary tests, see
     * PF-816.
     */
    private static Frequency classifyFrequency(double avgInterval) {
        Frequency shortTerm = classifyShortTermFrequency(avgInterval);
        return shortTerm != null ? shortTerm : classifyLongTermFrequency(avgInterval);
    }

    private static Frequency classifyShortTermFrequency(double avgInterval) {
        if (avgInterval >= 25 && avgInterval <= 35) return Frequency.MONTHLY;
        if (avgInterval >= 6 && avgInterval <= 8) return Frequency.WEEKLY;
        if (avgInterval >= 13 && avgInterval <= 16) return Frequency.BI_WEEKLY;
        return null;
    }

    private static Frequency classifyLongTermFrequency(double avgInterval) {
        if (avgInterval >= 85 && avgInterval <= 95) return Frequency.QUARTERLY;
        if (avgInterval >= 360 && avgInterval <= 370) return Frequency.YEARLY;
        return null;
    }
}
