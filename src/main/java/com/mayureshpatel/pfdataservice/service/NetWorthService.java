package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.dto.report.explore.DateWindow;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthCompare;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthComparisonDto;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthGroupBy;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthMovedDto;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthPointDto;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthSummaryDto;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.NetWorthRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.NetWorthRepository.IncomeExpense;
import com.mayureshpatel.pfdataservice.repository.transaction.NetWorthRepository.StoryFlows;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Builds the Reports net worth view: month-end totals of assets and debts, the groups (account types or
 * accounts) with their series, the cards' figures, what moved net worth, and an optional comparison. All
 * balances come from {@link NetWorthBalances} over each account's current balance and its monthly net flows,
 * so the latest point always equals the Accounts page. Everything is read-only and owner-scoped.
 */
@Service
@RequiredArgsConstructor
public class NetWorthService {

    private static final int SPENDING_MONTHS = 3;

    private final AccountRepository accountRepository;
    private final NetWorthRepository netWorthRepository;

    /** The window a comparison looks at. */
    private record CompareWindow(String label, LocalDate start, LocalDate end) {

        LocalDate monthEnd() {
            return YearMonth.from(end).atEndOfMonth();
        }
    }

    /**
     * Builds the breakdown.
     *
     * @param userId     the authenticated user
     * @param startDate  any date in the first month of the range
     * @param endDate    any date in the last month of the range
     * @param groupBy    group by account type or by account
     * @param accountIds restrict to these accounts, or empty for all
     * @param typeCode   restrict to one account type (a drill), or null for all
     * @param compare    which earlier window to compare with
     * @param asOf       the month the summary and the groups' balances are taken at, or null for the last month
     * @param today      today's date; a month end on or after it takes each account's current balance
     * @return the whole view in one response
     * @throws IllegalArgumentException for a reversed range, an unknown type, or an {@code asOf} outside the range
     */
    public NetWorthBreakdownDto breakdown(Long userId, LocalDate startDate, LocalDate endDate, NetWorthGroupBy groupBy,
                                          List<Long> accountIds, String typeCode, NetWorthCompare compare,
                                          YearMonth asOf, LocalDate today) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("The end date must not be before the start date.");
        }
        List<Account> included = select(accountRepository.findAllByUserId(userId), accountIds, typeCode);
        YearMonth firstMonth = YearMonth.from(startDate);
        YearMonth asOfMonth = resolveAsOf(asOf, firstMonth, YearMonth.from(endDate));
        CompareWindow window = compareWindow(compare, startDate, endDate, today);
        Map<Long, Map<YearMonth, BigDecimal>> flows = flows(userId, included, window, firstMonth);
        NetWorthBooks books = new NetWorthBooks(flows, today);

        List<LocalDate> monthEnds = NetWorthBalances.monthEnds(startDate, endDate);
        LocalDate openingEnd = firstMonth.minusMonths(1).atEndOfMonth();
        List<NetWorthPointDto> points = monthEnds.stream()
                .map(end -> point(included, books, end, today)).toList();
        NetWorthPointDto opening = point(included, books, openingEnd, today);
        LocalDate asOfEnd = asOfMonth.atEndOfMonth();

        DateWindow rangeWindow = window(firstMonth.atDay(1), YearMonth.from(endDate).plusMonths(1).atDay(1));
        List<Long> ids = included.stream().map(Account::getId).toList();
        return new NetWorthBreakdownDto(points, opening, asOfEnd,
                summary(userId, included, books, asOfEnd, endDate, today),
                moved(userId, ids, points, opening, rangeWindow),
                comparison(window, included, books),
                NetWorthGroupBuilder.build(groupBy, included, books,
                        new NetWorthGroupBuilder.Context(monthEnds, openingEnd, asOfEnd, window == null ? null : window.monthEnd()),
                        stories(userId, groupBy, included, rangeWindow)));
    }

    private static YearMonth resolveAsOf(YearMonth asOf, YearMonth firstMonth, YearMonth lastMonth) {
        YearMonth month = asOf == null ? lastMonth : asOf;
        if (month.isBefore(firstMonth) || month.isAfter(lastMonth)) {
            throw new IllegalArgumentException("The month must be inside the range.");
        }
        return month;
    }

    /** Reads every account's monthly flows from the earliest month any balance needs (the range, or just after the comparison). */
    private Map<Long, Map<YearMonth, BigDecimal>> flows(Long userId, List<Account> included, CompareWindow window, YearMonth firstMonth) {
        if (included.isEmpty()) {
            return Map.of();
        }
        YearMonth earliest = window == null ? firstMonth : minMonth(firstMonth, YearMonth.from(window.end()).plusMonths(1));
        return netWorthRepository.findMonthlyNetFlow(userId, earliest.atDay(1));
    }

    private List<Account> select(List<Account> all, List<Long> accountIds, String typeCode) {
        if (typeCode != null && all.stream().noneMatch(a -> typeCode.equals(a.getType().getCode()))) {
            throw new IllegalArgumentException("Unknown account type: " + typeCode);
        }
        return all.stream()
                .filter(a -> accountIds == null || accountIds.isEmpty() || accountIds.contains(a.getId()))
                .filter(a -> typeCode == null || typeCode.equals(a.getType().getCode()))
                .toList();
    }

    private static YearMonth minMonth(YearMonth first, YearMonth second) {
        return first.isBefore(second) ? first : second;
    }

    private static DateWindow window(LocalDate startInclusive, LocalDate endExclusive) {
        return new DateWindow(startInclusive.atStartOfDay().atOffset(ZoneOffset.UTC), endExclusive.atStartOfDay().atOffset(ZoneOffset.UTC));
    }

    private CompareWindow compareWindow(NetWorthCompare compare, LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (compare == null || compare == NetWorthCompare.NONE) {
            return null;
        }
        if (compare == NetWorthCompare.YEAR_AGO) {
            return new CompareWindow("a year ago", startDate.minusYears(1), endDate.minusYears(1));
        }
        LocalDate shownEnd = endDate.isAfter(today) ? today : endDate;
        long days = ChronoUnit.DAYS.between(startDate, shownEnd) + 1;
        LocalDate end = startDate.minusDays(1);
        return new CompareWindow("the previous period", end.minusDays(days - 1), end);
    }

    private NetWorthPointDto point(List<Account> accounts, NetWorthBooks books, LocalDate monthEnd, LocalDate today) {
        NetWorthBooks.Position position = books.position(accounts, monthEnd);
        return new NetWorthPointDto(monthEnd, position.assets(), position.debts(), position.netWorth(), !monthEnd.isBefore(today));
    }

    private NetWorthSummaryDto summary(Long userId, List<Account> included, NetWorthBooks books, LocalDate asOfEnd, LocalDate endDate, LocalDate today) {
        NetWorthBooks.Position position = books.position(included, asOfEnd);
        BigDecimal average = BigDecimal.ZERO;
        if (!included.isEmpty()) {
            LocalDate shownEnd = endDate.isAfter(today) ? today : endDate;
            YearMonth last = YearMonth.from(shownEnd);
            DateWindow spendWindow = window(last.minusMonths(SPENDING_MONTHS - 1L).atDay(1), last.plusMonths(1).atDay(1));
            BigDecimal spent = netWorthRepository.findExpense(userId, included.stream().map(Account::getId).toList(), spendWindow);
            average = spent.divide(BigDecimal.valueOf(SPENDING_MONTHS), 2, RoundingMode.HALF_UP);
        }
        BigDecimal covered = average.signum() == 0 ? null : position.liquid().divide(average, 1, RoundingMode.HALF_UP);
        return new NetWorthSummaryDto(position.netWorth(), position.assets(), position.debts(), position.liquid(), average, covered);
    }

    private NetWorthMovedDto moved(Long userId, List<Long> ids, List<NetWorthPointDto> points, NetWorthPointDto opening, DateWindow range) {
        BigDecimal change = points.get(points.size() - 1).netWorth().subtract(opening.netWorth());
        BigDecimal saved = BigDecimal.ZERO;
        if (!ids.isEmpty()) {
            IncomeExpense totals = netWorthRepository.findIncomeExpense(userId, ids, range);
            saved = totals.income().subtract(totals.expense());
        }
        return new NetWorthMovedDto(change, saved, change.subtract(saved));
    }

    private Map<Long, StoryFlows> stories(Long userId, NetWorthGroupBy groupBy, List<Account> included, DateWindow range) {
        return groupBy == NetWorthGroupBy.ACCOUNT && !included.isEmpty() ? netWorthRepository.findStoryFlows(userId, range) : Map.of();
    }

    private NetWorthComparisonDto comparison(CompareWindow window, List<Account> included, NetWorthBooks books) {
        if (window == null) {
            return null;
        }
        NetWorthBooks.Position position = books.position(included, window.monthEnd());
        return new NetWorthComparisonDto(window.label(), window.start(), window.end(), position.netWorth(), position.assets(), position.debts());
    }
}
