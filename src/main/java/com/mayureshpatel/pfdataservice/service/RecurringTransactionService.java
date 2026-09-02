package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringSuggestionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.RecurringTransactionDtoMapper;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CRUD for a user's confirmed recurring transactions, plus {@link #findSuggestions}, which
 * detects candidate recurring patterns in the user's transaction history for them to confirm.
 * Detection groups the last 12 months of expenses by merchant/description + amount, then checks
 * whether a group's inter-transaction intervals are stable enough to call weekly, bi-weekly,
 * monthly, or yearly.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;

    /**
     * Returns the user's active confirmed recurring transactions, next-occurrence first.
     *
     * @param userId the user id
     * @return the user's recurring transactions
     */
    public List<RecurringTransactionDto> getRecurringTransactions(Long userId) {
        return recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(userId).stream()
                .map(RecurringTransactionDtoMapper::toDto)
                .toList();
    }

    /**
     * Detects candidate recurring transaction patterns in the last 12 months of the user's
     * expenses that aren't already confirmed as recurring, ranked by confidence (higher
     * occurrence counts score higher).
     *
     * @param userId the user id
     * @return the detected suggestions, highest confidence first
     */
    public List<RecurringSuggestionDto> findSuggestions(Long userId) {
        // 1. get existing recurring items to exclude duplicates
        Set<String> existingMerchants = recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(userId).stream()
                .map(r -> r.getMerchant() != null && r.getMerchant().getCleanName() != null
                        ? r.getMerchant().getCleanName().toLowerCase()
                        : "")
                .collect(Collectors.toSet());

        // 2. fetch expenses from last 12 months
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        List<Transaction> transactions = transactionRepository.findExpensesSince(userId, oneYearAgo);

        // 3. group by merchant name (or description) + amount
        Map<String, List<Transaction>> groups = new HashMap<>();

        for (Transaction t : transactions) {
            String name = t.getMerchant() != null && t.getMerchant().getCleanName() != null
                    ? t.getMerchant().getCleanName()
                    : t.getDescription();
            if (name == null) continue;

            name = name.trim();
            if (existingMerchants.contains(name.toLowerCase())) continue;

            String key = name + "|" + t.getAmount();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        List<RecurringSuggestionDto> suggestions = new ArrayList<>();

        // 4. analyze groups
        for (Map.Entry<String, List<Transaction>> entry : groups.entrySet()) {
            List<Transaction> group = entry.getValue();

            if (group.size() < 3) continue;

            // filter out any transactions with null dates to prevent npe during sort
            group = group.stream()
                    .filter(t -> t.getTransactionDate() != null)
                    .collect(Collectors.toList());

            if (group.size() < 3) continue;

            group.sort(Comparator.comparing(Transaction::getTransactionDate));

            Frequency frequency = detectFrequency(group);
            if (frequency != null) {
                Transaction lastTxn = group.get(group.size() - 1);
                String[] parts = entry.getKey().split("\\|");
                String merchantName = parts[0];
                BigDecimal amount = new BigDecimal(parts[1]);

                suggestions.add(RecurringSuggestionDto.builder()
                        .merchant(new MerchantDto(null, null, null, merchantName))
                        .amount(amount)
                        .frequency(frequency)
                        .lastDate(lastTxn.getTransactionDate().toLocalDate())
                        .nextDate(calculateNextDate(lastTxn.getTransactionDate().toLocalDate(), frequency))
                        .occurrenceCount(group.size())
                        .confidenceScore(0.8 + (group.size() * 0.05))
                        .build());
            }
        }

        return suggestions.stream()
                .sorted(Comparator.comparingDouble(RecurringSuggestionDto::confidenceScore).reversed())
                .toList();
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
    private Frequency detectFrequency(List<Transaction> group) {
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < group.size(); i++) {
            intervals.add(ChronoUnit.DAYS.between(
                    group.get(i - 1).getTransactionDate(),
                    group.get(i).getTransactionDate()));
        }

        double avgInterval = intervals.stream().mapToLong(val -> val).average().orElse(0);

        boolean stable = intervals.stream().allMatch(i -> Math.abs(i - avgInterval) < 5);

        if (!stable) return null;

        if (avgInterval >= 25 && avgInterval <= 35) return Frequency.MONTHLY;
        if (avgInterval >= 6 && avgInterval <= 8) return Frequency.WEEKLY;
        if (avgInterval >= 13 && avgInterval <= 16) return Frequency.BI_WEEKLY;
        if (avgInterval >= 85 && avgInterval <= 95) return Frequency.QUARTERLY;
        if (avgInterval >= 360 && avgInterval <= 370) return Frequency.YEARLY;

        return null;
    }

    /**
     * Projects the next expected occurrence date from the last known one and a frequency.
     *
     * @param lastDate  the most recent occurrence
     * @param frequency how often it recurs
     * @return the projected next occurrence date
     */
    private LocalDate calculateNextDate(LocalDate lastDate, Frequency frequency) {
        return switch (frequency) {
            case MONTHLY -> lastDate.plusMonths(1);
            case WEEKLY -> lastDate.plusWeeks(1);
            case BI_WEEKLY -> lastDate.plusWeeks(2);
            case QUARTERLY -> lastDate.plusMonths(3);
            case YEARLY -> lastDate.plusYears(1);
        };
    }

    /**
     * Confirms a new recurring transaction, verifying the account (if given) and merchant (if
     * given) both exist and, for the account, belong to the user.
     *
     * @param userId  the user id
     * @param request the recurring transaction to create
     * @return the new record's generated id
     */
    @Transactional
    public int createRecurringTransaction(Long userId, RecurringTransactionCreateRequest request) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getAccountId() != null) {
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
            if (!account.getUserId().equals(userId)) {
                throw new AccessDeniedException("Access denied to account");
            }
        }

        if (request.getMerchantId() != null && request.getMerchantId() > 0) {
            merchantRepository.findById(request.getMerchantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));
        }

        return recurringRepository.insert(request, userId);
    }

    /**
     * Updates an existing recurring transaction owned by the user, re-verifying the account (if
     * given) and merchant (if given) the same way {@link #createRecurringTransaction} does.
     *
     * @param userId  the user id
     * @param request the recurring transaction to update, including its id
     * @return the number of rows updated
     * @throws ResourceNotFoundException if no recurring transaction with that id exists
     * @throws AccessDeniedException     if it belongs to a different user
     */
    @Transactional
    public int updateRecurringTransaction(Long userId, RecurringTransactionUpdateRequest request) {
        RecurringTransaction recurring = recurringRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found"));

        if (!recurring.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        if (request.getAccountId() != null) {
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

            if (!account.getUserId().equals(userId)) {
                throw new AccessDeniedException("Access denied to account");
            }
        }

        if (request.getMerchantId() != null && request.getMerchantId() > 0) {
            merchantRepository.findById(request.getMerchantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));
        }

        return recurringRepository.update(request, userId);
    }

    /**
     * Deletes a recurring transaction owned by the user.
     *
     * @param userId the user id
     * @param id     the recurring transaction id to delete
     * @return the number of rows deleted
     * @throws ResourceNotFoundException if no recurring transaction with that id exists
     * @throws AccessDeniedException     if it belongs to a different user
     */
    @Transactional
    public int deleteRecurringTransaction(Long userId, Long id) {
        RecurringTransaction recurring = recurringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found"));

        if (!recurring.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        return recurringRepository.delete(id, userId);
    }
}
