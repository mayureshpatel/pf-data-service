package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthGroupBy;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthGroupDto;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthStoryDto;
import com.mayureshpatel.pfdataservice.repository.transaction.NetWorthRepository.StoryFlows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the included accounts into the breakdown's rows: one per account type or per account, each with its
 * balance at the as-of month end, its balance before the range, an optional comparison balance, a series with one
 * value per point, and (for accounts) a story. Assets come first, largest first, then debts, largest owed first.
 */
final class NetWorthGroupBuilder {

    private static final String ASSET = "ASSET";
    private static final String DEBT = "DEBT";

    private NetWorthGroupBuilder() {
    }

    /**
     * What the builder needs besides the accounts and the books.
     *
     * @param monthEnds          the range's month ends, oldest first
     * @param openingEnd         the month end before the range
     * @param asOfEnd            the month end the balances are taken at
     * @param comparisonMonthEnd the month end of the comparison window, or null when not comparing
     */
    record Context(List<LocalDate> monthEnds, LocalDate openingEnd, LocalDate asOfEnd, LocalDate comparisonMonthEnd) {
    }

    /**
     * @param groupBy  group by account type or by account
     * @param included the accounts to group
     * @param books    the balance source
     * @param context  the dates to read balances at
     * @param stories  each account's deposits, withdrawals and other movements over the range
     * @return the rows, assets first
     */
    static List<NetWorthGroupDto> build(NetWorthGroupBy groupBy, List<Account> included, NetWorthBooks books, Context context,
                                        Map<Long, StoryFlows> stories) {
        Map<String, List<Account>> members = new LinkedHashMap<>();
        for (Account account : included) {
            String key = groupBy == NetWorthGroupBy.TYPE ? account.getType().getCode() : String.valueOf(account.getId());
            members.computeIfAbsent(key, k -> new ArrayList<>()).add(account);
        }
        Comparator<NetWorthGroupDto> assetsFirst = Comparator.comparing((NetWorthGroupDto g) -> DEBT.equals(g.kind()))
                .thenComparing(g -> DEBT.equals(g.kind()) ? g.balance() : g.balance().negate());
        return members.entrySet().stream()
                .map(e -> group(groupBy, e.getKey(), e.getValue(), books, context, stories))
                .sorted(assetsFirst)
                .toList();
    }

    private static NetWorthGroupDto group(NetWorthGroupBy groupBy, String key, List<Account> accounts, NetWorthBooks books,
                                          Context context, Map<Long, StoryFlows> stories) {
        Account first = accounts.get(0);
        boolean byType = groupBy == NetWorthGroupBy.TYPE;
        List<BigDecimal> series = context.monthEnds().stream().map(end -> books.sumOrNull(accounts, end)).toList();
        BigDecimal comparisonBalance = context.comparisonMonthEnd() == null ? null : books.sum(accounts, context.comparisonMonthEnd());
        return new NetWorthGroupDto(key, byType ? first.getType().getLabel() : first.getName(),
                first.getType().isAsset() ? ASSET : DEBT, first.getType().getCode(), accounts.size(),
                books.sum(accounts, context.asOfEnd()), books.sum(accounts, context.openingEnd()), comparisonBalance, series,
                byType ? null : story(first, books, context, stories.get(first.getId())));
    }

    private static NetWorthStoryDto story(Account account, NetWorthBooks books, Context context, StoryFlows flows) {
        BigDecimal opening = books.rawBalance(account, context.openingEnd());
        BigDecimal closing = books.rawBalance(account, context.monthEnds().get(context.monthEnds().size() - 1));
        StoryFlows shown = flows == null ? new StoryFlows(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO) : flows;
        return new NetWorthStoryDto(opening, closing,
                shown.deposits(), shown.withdrawals(), shown.interestAndAdjustments());
    }
}
