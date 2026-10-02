package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringSuggestionDto;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects candidate recurring transaction patterns in a user's transaction history for them to
 * confirm (PF-809: extracted from {@code RecurringTransactionService}, whose CRUD-plus-detection
 * combination had grown into a PMD {@code GodClass} once detection's own internals were split
 * into enough named helper methods to resolve ITS complexity findings -- the method-level split
 * that fixed Cyclomatic/NPath/Cognitive complexity there lowered this class's Tight Class
 * Cohesion just enough to newly cross the GodClass threshold, since most of the new helpers are
 * pure functions that don't touch either class's instance state. Splitting the two genuinely
 * separate concerns, CRUD vs. detection, into their own classes resolved both: each stayed
 * cohesive around only the fields its own methods actually use).
 * <p>
 * Groups the last 12 months of expenses by merchant/description + amount, then checks whether a
 * group's inter-transaction intervals are stable enough to call weekly, bi-weekly, monthly,
 * quarterly, or yearly.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecurringSuggestionFinder {

    private static final int MIN_OCCURRENCES_FOR_RECURRING_PATTERN = 3;

    private final RecurringTransactionRepository recurringRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Detects candidate recurring transaction patterns in the last 12 months of the user's
     * expenses that aren't already confirmed as recurring, ranked by confidence (higher
     * occurrence counts score higher).
     *
     * @param userId the user id
     * @return the detected suggestions, highest confidence first
     */
    public List<RecurringSuggestionDto> findSuggestions(Long userId) {
        Set<String> existingMerchants = findExistingMerchantNames(userId);

        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        List<Transaction> transactions = transactionRepository.findExpensesSince(userId, oneYearAgo);

        Map<String, List<Transaction>> groups = groupEligibleExpensesByMerchant(transactions, existingMerchants);

        List<RecurringSuggestionDto> suggestions = new ArrayList<>();
        for (Map.Entry<String, List<Transaction>> entry : groups.entrySet()) {
            RecurringSuggestionDto suggestion = analyzeGroup(entry.getKey(), entry.getValue());
            if (suggestion != null) {
                suggestions.add(suggestion);
            }
        }

        return suggestions.stream()
                .sorted(Comparator.comparingDouble(RecurringSuggestionDto::confidenceScore).reversed())
                .toList();
    }

    /**
     * The user's already-confirmed recurring merchants, to exclude as duplicate suggestions.
     */
    private Set<String> findExistingMerchantNames(Long userId) {
        return recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(userId).stream()
                .map(r -> r.getMerchant() != null ? r.getMerchant().getName().toLowerCase(Locale.ROOT) : "")
                .collect(Collectors.toSet());
    }

    /**
     * Groups by merchant name (or description) alone -- NOT also by amount. A subscription that
     * changed price partway through its history (routine: rate increases, promotional periods
     * ending) used to split into one independent group per price, each evaluated as its own
     * recurring candidate -- confirmed live, Netflix's real 56-month history across 4 price tiers
     * surfaced as multiple simultaneous, conflicting suggestions for the same subscription.
     * {@link #detectFrequency} only ever looked at dates, never amount, so grouping by name alone
     * doesn't change frequency detection at all -- see PF-834.
     */
    private static Map<String, List<Transaction>> groupEligibleExpensesByMerchant(List<Transaction> transactions, Set<String> existingMerchants) {
        Map<String, List<Transaction>> groups = new HashMap<>();

        for (Transaction t : transactions) {
            String name = t.getMerchant() != null ? t.getMerchant().getName() : t.getDescription();
            if (name == null) continue;

            name = name.trim();
            if (existingMerchants.contains(name.toLowerCase(Locale.ROOT))) continue;

            groups.computeIfAbsent(name, k -> new ArrayList<>()).add(t);
        }

        return groups;
    }

    /**
     * Checks one merchant's group of expenses for a stable recurring pattern, building the
     * suggestion if one's found.
     *
     * @return the suggestion, or {@code null} if the group is too small or its intervals aren't
     * stable enough to call recurring
     */
    private static RecurringSuggestionDto analyzeGroup(String merchantName, List<Transaction> group) {
        if (group.size() < MIN_OCCURRENCES_FOR_RECURRING_PATTERN) {
            return null;
        }

        // filter out any transactions with null dates to prevent npe during sort
        List<Transaction> datedGroup = group.stream()
                .filter(t -> t.getTransactionDate() != null)
                .collect(Collectors.toList());

        if (datedGroup.size() < MIN_OCCURRENCES_FOR_RECURRING_PATTERN) {
            return null;
        }

        datedGroup.sort(Comparator.comparing(Transaction::getTransactionDate));

        Frequency frequency = detectFrequency(datedGroup);
        if (frequency == null) {
            return null;
        }

        Transaction lastTxn = datedGroup.get(datedGroup.size() - 1);
        return RecurringSuggestionDto.builder()
                .merchant(MerchantDto.builder().name(merchantName).build())
                .amount(lastTxn.getAmount())
                .frequency(frequency)
                .lastDate(lastTxn.getTransactionDate().toLocalDate())
                .nextDate(calculateNextDate(lastTxn.getTransactionDate().toLocalDate(), frequency))
                .occurrenceCount(datedGroup.size())
                .confidenceScore(calculateConfidenceScore(datedGroup.size()))
                .build();
    }

    /**
     * Determines whether a date-sorted group of transactions recurs at a stable interval.
     * "Stable" means every consecutive gap stays within 5 days of the group's average gap.
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
    private static Frequency detectFrequency(List<Transaction> group) {
        List<Long> intervals = computeIntervals(group);
        double avgInterval = intervals.stream().mapToLong(val -> val).average().orElse(0);
        boolean stable = intervals.stream().allMatch(i -> Math.abs(i - avgInterval) < 5);

        return stable ? classifyFrequency(avgInterval) : null;
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

    /**
     * Scores how confident a detected pattern is, as a real 0-100 percentage. The prior formula
     * (uncapped {@code 0.8 + occurrenceCount * 0.05}) was never actually a percentage -- displayed
     * with a literal "%" suffix and color-coded against 0-100 thresholds on the frontend, it read
     * as "1.35%" for the *strongest* real suggestion in a live dataset, and the color tiers never
     * differentiated anything since real values never approached even 2.0. See PF-835.
     * <p>
     * {@link #MIN_OCCURRENCES_FOR_RECURRING_PATTERN} (the minimum to even qualify as a suggestion
     * at all) starts at a substantial 50%; each occurrence past that adds 4 points, capped at 100%
     * once a pattern has repeated enough (a dozen-plus occurrences) to be about as confident as
     * this heuristic can meaningfully get.
     *
     * @param occurrenceCount how many transactions matched the pattern
     * @return a confidence percentage in [0, 100]
     */
    private static double calculateConfidenceScore(int occurrenceCount) {
        double score = 50.0 + (occurrenceCount - MIN_OCCURRENCES_FOR_RECURRING_PATTERN) * 4.0;
        return Math.min(100.0, score);
    }

    /**
     * Projects the next expected occurrence date from the last known one and a frequency.
     *
     * @param lastDate  the most recent occurrence
     * @param frequency how often it recurs
     * @return the projected next occurrence date
     */
    private static LocalDate calculateNextDate(LocalDate lastDate, Frequency frequency) {
        return switch (frequency) {
            case MONTHLY -> lastDate.plusMonths(1);
            case WEEKLY -> lastDate.plusWeeks(1);
            case BI_WEEKLY -> lastDate.plusWeeks(2);
            case QUARTERLY -> lastDate.plusMonths(3);
            case YEARLY -> lastDate.plusYears(1);
        };
    }
}
