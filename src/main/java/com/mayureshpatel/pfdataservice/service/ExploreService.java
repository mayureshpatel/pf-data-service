package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.report.explore.DateWindow;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreCompare;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreFilter;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreGroupBy;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreMeasure;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreResultDto;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreRowDto;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreSide;
import com.mayureshpatel.pfdataservice.repository.transaction.ExploreRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.ExploreRepository.ExploreRowData;
import com.mayureshpatel.pfdataservice.service.ExploreRanges.ResolvedRanges;
import lombok.RequiredArgsConstructor;
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
        if (measure.timeOnly() && !groupBy.isTime()) {
            throw new IllegalArgumentException(measure + " can only be grouped by time (month, week or day), not by " + groupBy);
        }
        ResolvedRanges ranges = ExploreRanges.resolve(startDate, endDate, compare, today);
        List<ExploreRowData> current = repository.findGroups(userId, groupBy, filter, ranges.current());
        DateWindow comparisonWindow = ranges.comparison();
        List<ExploreRowData> earlier = comparisonWindow == null ? List.of() : repository.findGroups(userId, groupBy, filter, comparisonWindow);

        ExploreRowData total = totalOf(userId, groupBy, filter, ranges.current(), current);
        ExploreRowData earlierTotal = comparisonWindow == null ? null : totalOf(userId, groupBy, filter, comparisonWindow, earlier);
        long days = ChronoUnit.DAYS.between(ranges.current().start(), ranges.current().end());

        List<ExploreRowDto> rows = buildRows(measure, side, groupBy, compare, days, current, earlier, comparisonWindow != null);
        return new ExploreResultDto(
                rows,
                ExploreMeasures.value(measure, side, total),
                earlierTotal == null ? null : ExploreMeasures.value(measure, side, earlierTotal),
                ExploreMeasures.count(measure, side, total),
                repository.countExcluded(userId, filter, ranges.current()),
                comparisonWindow == null ? null : comparisonWindow.start().toLocalDate(),
                comparisonWindow == null ? null : comparisonWindow.end().minusDays(1).toLocalDate(),
                ranges.comparisonLabel(),
                ranges.inProgress());
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
                                          List<ExploreRowData> current, List<ExploreRowData> earlier, boolean comparing) {
        Map<String, ExploreRowData> earlierByKey = new HashMap<>();
        earlier.forEach(row -> earlierByKey.put(row.key(), row));

        List<ExploreRowDto> rows = new ArrayList<>();
        Set<String> matched = new HashSet<>();
        for (ExploreRowData row : current) {
            if (!ExploreMeasures.hasActivity(measure, side, row)) {
                continue;
            }
            String earlierKey = ExploreLabels.comparisonKey(groupBy, row.key(), compare, days);
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

    private BigDecimal comparisonValue(ExploreMeasure measure, ExploreSide side, ExploreRowData counterpart) {
        return counterpart == null ? BigDecimal.ZERO : ExploreMeasures.value(measure, side, counterpart);
    }
}
