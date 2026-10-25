package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.report.explore.DateWindow;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreCompare;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreFilter;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreGroupBy;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreMeasure;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreResultDto;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreRowDto;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreSide;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreTransactionDto;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreTrendDto;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreTrendDto.Bucket;
import com.mayureshpatel.pfdataservice.repository.transaction.ExploreRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.ExploreRepository.ExploreRowData;
import com.mayureshpatel.pfdataservice.service.ExploreRanges.ResolvedRanges;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Answers the Reports explorer: any measure (spending, income, net, savings rate, count, average) by
 * any grouping under stacked filters, with an optional comparison against an equivalent earlier period.
 * The database does the grouping; this class turns the grouped income, expense and counts into the
 * chosen measure, joins the comparison rows, and keeps totals honest when a transaction appears in
 * several groups (tags).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExploreService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String EXPENSE = "EXPENSE";
    private static final String INCOME = "INCOME";
    private static final ExploreRowData EMPTY = new ExploreRowData(null, "", BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);

    private final ExploreRepository repository;

    /**
     * Runs the explorer for today's date.
     *
     * @param userId    the user id
     * @param measure   what to measure
     * @param side      which side count and average look at
     * @param groupBy   how to group
     * @param startDate the first day, inclusive
     * @param endDate   the last day, inclusive
     * @param compare   which earlier period to compare with
     * @param filter    the stacked filters
     * @return the groups, totals and comparison
     * @throws IllegalArgumentException if the measure only makes sense over time and the grouping is not a time bucket
     */
    public ExploreResultDto explore(Long userId, ExploreMeasure measure, ExploreSide side, ExploreGroupBy groupBy,
                                    LocalDate startDate, LocalDate endDate, ExploreCompare compare, ExploreFilter filter) {
        return explore(userId, measure, side, groupBy, startDate, endDate, compare, filter, LocalDate.now(ZoneOffset.UTC));
    }

    /**
     * Runs the explorer as of a given date (so a caller, or a test, can fix what "today" means for
     * the in-progress clamping).
     *
     * @param userId    the user id
     * @param measure   what to measure
     * @param side      which side count and average look at
     * @param groupBy   how to group
     * @param startDate the first day, inclusive
     * @param endDate   the last day, inclusive
     * @param compare   which earlier period to compare with
     * @param filter    the stacked filters
     * @param today     the date treated as today
     * @return the groups, totals and comparison
     * @throws IllegalArgumentException if the measure only makes sense over time and the grouping is not a time bucket
     */
    public ExploreResultDto explore(Long userId, ExploreMeasure measure, ExploreSide side, ExploreGroupBy groupBy,
                                    LocalDate startDate, LocalDate endDate, ExploreCompare compare, ExploreFilter filter, LocalDate today) {
        requireTimeGroupingIfTimeOnly(measure, groupBy);
        ExploreGroupBy unit = groupBy == ExploreGroupBy.MONTH || groupBy == ExploreGroupBy.WEEK ? groupBy : null;
        ResolvedRanges ranges = ExploreRanges.resolve(startDate, endDate, compare, today, unit);
        List<ExploreRowData> current = repository.findGroups(userId, groupBy, filter, ranges.current());
        DateWindow comparisonWindow = ranges.comparison();
        List<ExploreRowData> earlier = comparisonWindow == null ? List.of() : repository.findGroups(userId, groupBy, filter, comparisonWindow);

        ExploreRowData total = totalOf(userId, groupBy, filter, ranges.current(), current);
        ExploreRowData earlierTotal = comparisonWindow == null ? null : totalOf(userId, groupBy, filter, comparisonWindow, earlier);
        long days = ChronoUnit.DAYS.between(ranges.current().start(), ranges.current().end());

        int unitBuckets = unit == null ? 0 : bucketCount(unit, ranges.current());
        List<ExploreRowDto> rows = buildRows(measure, side, groupBy, compare, days, unitBuckets, current, earlier, comparisonWindow != null);
        return new ExploreResultDto(
                rows,
                ExploreMeasures.value(measure, side, total),
                earlierTotal == null ? null : ExploreMeasures.value(measure, side, earlierTotal),
                ExploreMeasures.count(measure, side, total),
                repository.countExcluded(userId, filter, ranges.current()),
                comparisonDay(comparisonWindow, true),
                comparisonDay(comparisonWindow, false),
                ranges.comparisonLabel(),
                ranges.inProgress());
    }

    private static void requireTimeGroupingIfTimeOnly(ExploreMeasure measure, ExploreGroupBy groupBy) {
        if (measure.timeOnly() && !groupBy.isTime()) {
            throw new IllegalArgumentException(measure + " can only be grouped by time (month, week or day), not by " + groupBy);
        }
    }

    /** The first or last day of the comparison window, or null when there is no comparison. */
    private static LocalDate comparisonDay(DateWindow window, boolean first) {
        if (window == null) {
            return null;
        }
        return first ? window.start().toLocalDate() : window.end().minusDays(1).toLocalDate();
    }

    /**
     * Runs the trend for today's date.
     *
     * @param userId    the user id
     * @param measure   what to measure
     * @param side      which side count and average look at
     * @param startDate the first day, inclusive
     * @param endDate   the last day, inclusive
     * @param compare   which earlier period to compare with
     * @param filter    the stacked filters
     * @return the measure per bucket, with the comparison period's buckets lined up by position
     */
    public ExploreTrendDto trend(Long userId, ExploreMeasure measure, ExploreSide side, LocalDate startDate, LocalDate endDate,
                                 ExploreCompare compare, ExploreFilter filter) {
        return trend(userId, measure, side, startDate, endDate, compare, filter, LocalDate.now(ZoneOffset.UTC));
    }

    /**
     * Runs the trend as of a given date. Buckets are days up to 31 days, weeks up to 92 days and months
     * beyond; empty buckets are filled in, and when the period runs past today the last bucket is flagged
     * partial. The comparison has as many buckets as the current period, lined up by position.
     *
     * @param userId    the user id
     * @param measure   what to measure
     * @param side      which side count and average look at
     * @param startDate the first day, inclusive
     * @param endDate   the last day, inclusive
     * @param compare   which earlier period to compare with
     * @param filter    the stacked filters
     * @param today     the date treated as today
     * @return the measure per bucket, with the comparison period's buckets lined up by position
     */
    public ExploreTrendDto trend(Long userId, ExploreMeasure measure, ExploreSide side, LocalDate startDate, LocalDate endDate,
                                 ExploreCompare compare, ExploreFilter filter, LocalDate today) {
        DateWindow shown = ExploreRanges.resolve(startDate, endDate, ExploreCompare.NONE, today).current();
        LocalDate first = shown.start().toLocalDate();
        LocalDate last = shown.end().minusDays(1).toLocalDate();
        if (last.isBefore(first)) {
            return new ExploreTrendDto(List.of(), List.of());
        }
        ExploreGroupBy granularity = ExploreBuckets.granularityFor(ChronoUnit.DAYS.between(first, last) + 1);
        int count = ExploreBuckets.count(granularity, first, last);
        ResolvedRanges ranges = ExploreRanges.resolve(startDate, endDate, compare, today, granularity);

        List<Bucket> current = buckets(userId, measure, side, granularity, filter, ranges.current(), count, ranges.inProgress());
        DateWindow comparisonWindow = ranges.comparison();
        List<Bucket> earlier = comparisonWindow == null
                ? List.of()
                : buckets(userId, measure, side, granularity, filter, comparisonWindow, count, false);
        return new ExploreTrendDto(current, earlier);
    }

    /**
     * Pages through the transactions behind an explorer number for today's date.
     *
     * @param userId    the user id
     * @param measure   what to measure (decides whether expenses, income or both are listed)
     * @param side      which side count and average look at
     * @param startDate the first day, inclusive
     * @param endDate   the last day, inclusive
     * @param filter    the stacked filters
     * @param page      the zero-based page
     * @param size      the page size (capped at 100)
     * @return the page of transactions, newest first
     */
    public Page<ExploreTransactionDto> transactions(Long userId, ExploreMeasure measure, ExploreSide side, LocalDate startDate,
                                                    LocalDate endDate, ExploreFilter filter, int page, int size) {
        return transactions(userId, measure, side, startDate, endDate, filter, page, size, LocalDate.now(ZoneOffset.UTC));
    }

    /**
     * Pages through the transactions behind an explorer number as of a given date.
     *
     * @param userId    the user id
     * @param measure   what to measure (decides whether expenses, income or both are listed)
     * @param side      which side count and average look at
     * @param startDate the first day, inclusive
     * @param endDate   the last day, inclusive
     * @param filter    the stacked filters
     * @param page      the zero-based page
     * @param size      the page size (capped at 100)
     * @param today     the date treated as today
     * @return the page of transactions, newest first
     */
    public Page<ExploreTransactionDto> transactions(Long userId, ExploreMeasure measure, ExploreSide side, LocalDate startDate,
                                                    LocalDate endDate, ExploreFilter filter, int page, int size, LocalDate today) {
        DateWindow window = ExploreRanges.resolve(startDate, endDate, ExploreCompare.NONE, today).current();
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, MAX_PAGE_SIZE)));
        return repository.findTransactions(userId, filter, window, typesFor(measure, side), pageRequest);
    }

    private static List<String> typesFor(ExploreMeasure measure, ExploreSide side) {
        return switch (measure) {
            case SPENDING -> List.of(EXPENSE);
            case INCOME -> List.of(INCOME);
            case NET, SAVINGS_RATE -> List.of(EXPENSE, INCOME);
            case COUNT, AVERAGE -> List.of(side == ExploreSide.INCOME ? INCOME : EXPENSE);
        };
    }

    private List<Bucket> buckets(Long userId, ExploreMeasure measure, ExploreSide side, ExploreGroupBy granularity, ExploreFilter filter,
                                 DateWindow window, int count, boolean lastIsPartial) {
        Map<String, ExploreRowData> byStart = new HashMap<>();
        repository.findGroups(userId, granularity, filter, window).forEach(row -> byStart.put(row.key(), row));
        LocalDate first = ExploreBuckets.bucketStart(granularity, window.start().toLocalDate());

        List<Bucket> buckets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            LocalDate start = ExploreBuckets.advance(granularity, first, i);
            ExploreRowData data = byStart.getOrDefault(start.toString(), EMPTY);
            buckets.add(new Bucket(start, ExploreMeasures.value(measure, side, data), lastIsPartial && i == count - 1));
        }
        return buckets;
    }

    /**
     * A transaction that carries two tags shows up under both tag rows, so tag rows cannot be added up;
     * the total comes from a grouping every transaction belongs to exactly once (its account).
     */
    private ExploreRowData totalOf(Long userId, ExploreGroupBy groupBy, ExploreFilter filter, DateWindow window, List<ExploreRowData> rows) {
        if (groupBy == ExploreGroupBy.TAG) {
            return ExploreMeasures.sum(repository.findGroups(userId, ExploreGroupBy.ACCOUNT, filter, window));
        }
        return ExploreMeasures.sum(rows);
    }

    private List<ExploreRowDto> buildRows(ExploreMeasure measure, ExploreSide side, ExploreGroupBy groupBy, ExploreCompare compare, long days,
                                          int unitBuckets, List<ExploreRowData> current, List<ExploreRowData> earlier, boolean comparing) {
        Map<String, ExploreRowData> earlierByKey = new HashMap<>();
        earlier.forEach(row -> earlierByKey.put(row.key(), row));

        List<ExploreRowDto> rows = new ArrayList<>();
        Set<String> matched = new HashSet<>();
        for (ExploreRowData row : current) {
            if (!ExploreMeasures.hasActivity(measure, side, row)) {
                continue;
            }
            String earlierKey = ExploreLabels.comparisonKey(groupBy, row.key(), compare, days, unitBuckets);
            ExploreRowData counterpart = earlierByKey.get(earlierKey);
            matched.add(earlierKey);
            rows.add(new ExploreRowDto(
                    row.key(),
                    ExploreLabels.label(groupBy, row.key(), row.label()),
                    ExploreMeasures.value(measure, side, row),
                    comparing ? comparisonValue(measure, side, counterpart) : null,
                    ExploreMeasures.count(measure, side, row)));
        }
        if (comparing && !groupBy.isTime()) {
            earlier.stream()
                    .filter(row -> !matched.contains(row.key()) && ExploreMeasures.hasActivity(measure, side, row))
                    .forEach(row -> rows.add(new ExploreRowDto(
                            row.key(), ExploreLabels.label(groupBy, row.key(), row.label()), BigDecimal.ZERO,
                            ExploreMeasures.value(measure, side, row), 0L)));
        }
        rows.sort(Comparator.comparing(ExploreRowDto::value, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(row -> Objects.toString(row.label(), "")));
        return rows;
    }

    private static int bucketCount(ExploreGroupBy unit, DateWindow window) {
        LocalDate first = window.start().toLocalDate();
        LocalDate last = window.end().minusDays(1).toLocalDate();
        return last.isBefore(first) ? 0 : ExploreBuckets.count(unit, first, last);
    }

    private BigDecimal comparisonValue(ExploreMeasure measure, ExploreSide side, ExploreRowData counterpart) {
        return counterpart == null ? BigDecimal.ZERO : ExploreMeasures.value(measure, side, counterpart);
    }
}
