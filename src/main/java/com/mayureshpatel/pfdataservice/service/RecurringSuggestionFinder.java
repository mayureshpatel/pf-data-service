package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringSuggestionDto;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantDescriptionLinkRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.DismissedPatternRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Groups the last three years of expenses by merchant/description + amount, then checks whether a
 * group's inter-transaction intervals are stable enough to call weekly, bi-weekly, monthly,
 * quarterly, or yearly.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecurringSuggestionFinder {

    private static final int MIN_OCCURRENCES_FOR_RECURRING_PATTERN = 3;
    /** a yearly charge is judged on two sightings: three would need a three-year-old first charge. */
    private static final int MIN_OCCURRENCES_FOR_YEARLY_PATTERN = 2;
    private static final int LOOKBACK_YEARS = 3;

    private final RecurringTransactionRepository recurringRepository;
    private final TransactionRepository transactionRepository;
    private final DismissedPatternRepository dismissedRepository;
    private final MerchantDescriptionLinkRepository linkRepository;

    /**
     * Detects candidate recurring transaction patterns in the last three years of the user's
     * expenses that aren't already confirmed as recurring, ranked by confidence (higher
     * occurrence counts score higher).
     *
     * @param userId the user id
     * @return the detected suggestions, highest confidence first
     */
    public List<RecurringSuggestionDto> findSuggestions(Long userId) {
        Set<String> excludedKeys = findExcludedKeys(userId);

        LocalDate lookbackStart = LocalDate.now().minusYears(LOOKBACK_YEARS);
        List<Transaction> transactions = transactionRepository.findExpensesSince(userId, lookbackStart);

        Map<String, List<Transaction>> groups = PatternGrouping.groupEligibleExpenses(transactions, excludedKeys);

        List<RecurringSuggestionDto> suggestions = new ArrayList<>();
        for (Map.Entry<String, List<Transaction>> entry : groups.entrySet()) {
            RecurringSuggestionDto suggestion = analyzeGroup(entry.getKey(), entry.getValue());
            if (suggestion != null) {
                suggestions.add(suggestion);
            }
        }

        Map<String, Merchant> linked = suggestions.isEmpty()
                ? Map.of()
                : linkRepository.findMerchantsByNormalizedDescriptions(
                        userId, suggestions.stream().map(RecurringSuggestionDto::patternKey).toList());

        return suggestions.stream()
                .map(suggestion -> withLinkedMerchant(suggestion, linked.get(suggestion.patternKey())))
                .sorted(Comparator.comparingDouble(RecurringSuggestionDto::confidenceScore).reversed())
                .toList();
    }

    /**
     * Points a suggestion at the merchant its text is already linked to, when there is one, so the
     * review can offer that merchant instead of a duplicate.
     */
    private static RecurringSuggestionDto withLinkedMerchant(RecurringSuggestionDto suggestion, Merchant linked) {
        return linked == null
                ? suggestion
                : suggestion.toBuilder().linkedMerchantId(linked.getId()).linkedMerchantName(linked.getName()).build();
    }

    /**
     * The folded keys detection must not suggest: patterns the user dismissed, plus the names of every
     * tracked item's merchant (paused ones included -- pausing is a decision, not a gap) and every
     * description linked to those merchants.
     */
    private Set<String> findExcludedKeys(Long userId) {
        Set<String> excluded = new HashSet<>(dismissedRepository.findKeysByUserId(userId));
        Set<Long> trackedMerchantIds = new HashSet<>();
        for (RecurringTransaction tracked : recurringRepository.findAllByUserId(userId)) {
            if (tracked.getMerchant() == null) continue;
            excluded.add(DescriptionKeys.fold(tracked.getMerchant().getName()));
            if (tracked.getMerchant().getId() != null) {
                trackedMerchantIds.add(tracked.getMerchant().getId());
            }
        }
        excluded.addAll(linkRepository.findNormalizedDescriptionsByMerchantIds(userId, trackedMerchantIds));
        return excluded;
    }

    /**
     * Checks one merchant's group of expenses for a stable recurring pattern, building the
     * suggestion if one's found.
     *
     * @return the suggestion, or {@code null} if the group is too small or its intervals aren't
     * stable enough to call recurring
     */
    private static RecurringSuggestionDto analyzeGroup(String patternKey, List<Transaction> group) {
        // filter out any transactions with null dates to prevent npe during sort
        List<Transaction> datedGroup = group.stream()
                .filter(t -> t.getTransactionDate() != null)
                .collect(Collectors.toList());

        if (datedGroup.size() < MIN_OCCURRENCES_FOR_YEARLY_PATTERN) {
            return null;
        }

        datedGroup.sort(Comparator.comparing(Transaction::getTransactionDate));

        Frequency frequency = FrequencyDetector.detect(datedGroup);
        if (frequency == null || !hasEnoughSightings(datedGroup.size(), frequency)) {
            return null;
        }

        Transaction lastTxn = datedGroup.get(datedGroup.size() - 1);
        LocalDate lastDate = lastTxn.getTransactionDate().toLocalDate();
        if (frequency.advance(frequency.advance(lastDate)).isBefore(LocalDate.now())) {
            // missed two or more periods in a row: a cancelled subscription, not a live pattern
            return null;
        }
        PatternGrouping.PriceChange change = PatternGrouping.findPriceChange(datedGroup);
        return RecurringSuggestionDto.builder()
                .merchant(MerchantDto.builder().name(PatternGrouping.displayName(datedGroup)).build())
                .amount(lastTxn.getAmount())
                .frequency(frequency)
                .lastDate(lastDate)
                .nextDate(calculateNextDate(lastDate, frequency))
                .occurrenceCount(datedGroup.size())
                .confidenceScore(calculateConfidenceScore(datedGroup.size()))
                .accountId(lastTxn.getAccount() != null ? lastTxn.getAccount().getId() : null)
                .accountName(lastTxn.getAccount() != null ? lastTxn.getAccount().getName() : null)
                .previousAmount(change != null ? change.previousAmount() : null)
                .priceChangedOn(change != null ? change.changedOn() : null)
                .patternKey(patternKey)
                .build();
    }

    /**
     * A yearly pattern needs only two sightings; every other frequency needs three.
     */
    private static boolean hasEnoughSightings(int sightings, Frequency frequency) {
        int required = frequency == Frequency.YEARLY
                ? MIN_OCCURRENCES_FOR_YEARLY_PATTERN
                : MIN_OCCURRENCES_FOR_RECURRING_PATTERN;
        return sightings >= required;
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
        return frequency.advance(lastDate);
    }
}
