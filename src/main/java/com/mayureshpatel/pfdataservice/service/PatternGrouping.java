package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The grouping side of recurring-pattern detection: which transactions belong to the same candidate
 * pattern (by merchant name or raw description, folded like description links), what to call a group,
 * and whether a group's price changed. Split out of {@link RecurringSuggestionFinder}, which keeps the
 * interval and frequency judgement.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class PatternGrouping {

    /**
     * Groups by merchant name (or description) alone -- NOT also by amount. A subscription that
     * changed price partway through its history (routine: rate increases, promotional periods
     * ending) used to split into one independent group per price, each evaluated as its own
     * recurring candidate -- confirmed live, Netflix's real 56-month history across 4 price tiers
     * surfaced as multiple simultaneous, conflicting suggestions for the same subscription.
     * {@code RecurringSuggestionFinder}'s frequency detection only ever looks at dates, never amount, so grouping by name alone
     * doesn't change frequency detection at all -- see PF-834.
     */
    public static Map<String, List<Transaction>> groupEligibleExpenses(List<Transaction> transactions, Set<String> excludedKeys) {
        Map<String, List<Transaction>> groups = new HashMap<>();

        for (Transaction t : transactions) {
            String key = DescriptionKeys.fold(nameOf(t));
            if (key.isEmpty() || excludedKeys.contains(key)) continue;

            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        return groups;
    }

    /**
     * The text a transaction is grouped under: its merchant's name when it has one, otherwise its
     * raw description.
     */
    public static String nameOf(Transaction t) {
        return t.getMerchant() != null ? t.getMerchant().getName() : t.getDescription();
    }

    /**
     * The spelling shown for a group: the raw name used by most of its transactions.
     */
    public static String displayName(List<Transaction> group) {
        return group.stream()
                .map(t -> nameOf(t).strip())
                .collect(Collectors.groupingBy(n -> n, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
    }


    /**
     * The price before the newest unbroken run of equal amounts and the date that run began, or
     * {@code null} when the amount never changed.
     */
    public static PriceChange findPriceChange(List<Transaction> datedGroup) {
        BigDecimal latest = datedGroup.get(datedGroup.size() - 1).getAmount();
        if (latest == null) {
            return null;
        }
        int runStart = datedGroup.size() - 1;
        while (runStart > 0 && latest.compareTo(amountOrZero(datedGroup.get(runStart - 1))) == 0) {
            runStart--;
        }
        if (runStart == 0) {
            return null;
        }
        return new PriceChange(
                datedGroup.get(runStart - 1).getAmount(),
                datedGroup.get(runStart).getTransactionDate().toLocalDate());
    }

    private static BigDecimal amountOrZero(Transaction t) {
        return t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO;
    }

    /**
     * A price change inside a group.
     *
     * @param previousAmount the price before the newest unbroken run at the current amount
     * @param changedOn      the date that run began
     */
    public record PriceChange(BigDecimal previousAmount, LocalDate changedOn) {
    }
}
