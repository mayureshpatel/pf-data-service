package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.transaction.RecurringSuggestionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.RecurringTransactionDto;
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public List<RecurringTransactionDto> getRecurringTransactions(Long userId) {
        return recurringRepository.findByUserIdAndActiveTrueOrderByNextDateAsc(userId).stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<RecurringSuggestionDto> findSuggestions(Long userId) {
        // 1. Get existing recurring items to exclude duplicates
        Set<String> existingMerchants = recurringRepository.findByUserIdAndActiveTrueOrderByNextDateAsc(userId).stream()
                .map(r -> r.getMerchantName().toLowerCase())
                .collect(Collectors.toSet());

        // 2. Fetch expenses from last 12 months
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        List<Transaction> transactions = transactionRepository.findExpensesSince(userId, oneYearAgo);

        // 3. Group by Merchant Name (or Description) + Amount
        // Key format: "merchant_name|amount"
        Map<String, List<Transaction>> groups = new HashMap<>();
        
        for (Transaction t : transactions) {
            String name = t.getVendorName() != null ? t.getVendorName() : t.getDescription();
            if (name == null) continue;
            
            // Normalize name (simple cleanup)
            name = name.trim();
            if (existingMerchants.contains(name.toLowerCase())) continue;

            // Grouping key: Name + Amount (to distinguish Netflix Basic vs Premium if user upgraded, or different subscriptions)
            // But strict amount matching might miss small variations ($15.99 vs $16.99). 
            // For MVP, let's group by Name only first, then check amounts? 
            // Better: Group by Name + Amount to be safe for distinct bills.
            String key = name + "|" + t.getAmount();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        List<RecurringSuggestionDto> suggestions = new ArrayList<>();

        // 4. Analyze groups
        for (Map.Entry<String, List<Transaction>> entry : groups.entrySet()) {
            List<Transaction> group = entry.getValue();
            
            // Need at least 3 occurrences to be confident about a pattern
            if (group.size() < 3) continue;

            // Sort by date ASC
            group.sort(Comparator.comparing(Transaction::getDate));

            // Analyze intervals
            Frequency frequency = detectFrequency(group);
            if (frequency != null) {
                Transaction lastTxn = group.get(group.size() - 1);
                String[] parts = entry.getKey().split("\\|");
                String merchantName = parts[0];
                BigDecimal amount = new BigDecimal(parts[1]);

                suggestions.add(RecurringSuggestionDto.builder()
                        .merchantName(merchantName)
                        .amount(amount)
                        .frequency(frequency)
                        .lastDate(lastTxn.getDate())
                        .nextDate(calculateNextDate(lastTxn.getDate(), frequency))
                        .occurrenceCount(group.size())
                        .confidenceScore(0.8 + (group.size() * 0.05)) // Base 0.8, increases with count
                        .build());
            }
        }

        // Sort by confidence
        return suggestions.stream()
                .sorted(Comparator.comparingDouble(RecurringSuggestionDto::confidenceScore).reversed())
                .toList();
    }

    private Frequency detectFrequency(List<Transaction> group) {
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < group.size(); i++) {
            intervals.add(ChronoUnit.DAYS.between(group.get(i - 1).getDate(), group.get(i).getDate()));
        }

        double avgInterval = intervals.stream().mapToLong(val -> val).average().orElse(0);
        
        // Check for stability (Standard Deviation or simple range check)
        // Simple range check for MVP
        boolean stable = intervals.stream().allMatch(i -> Math.abs(i - avgInterval) < 5); // Allow 5 days variance

        if (!stable) return null;

        if (avgInterval >= 25 && avgInterval <= 35) return Frequency.MONTHLY;
        if (avgInterval >= 6 && avgInterval <= 8) return Frequency.WEEKLY;
        if (avgInterval >= 13 && avgInterval <= 16) return Frequency.BI_WEEKLY;
        if (avgInterval >= 360 && avgInterval <= 370) return Frequency.YEARLY;

        return null;
    }

    private LocalDate calculateNextDate(LocalDate lastDate, Frequency frequency) {
        return switch (frequency) {
            case MONTHLY -> lastDate.plusMonths(1);
            case WEEKLY -> lastDate.plusWeeks(1);
            case BI_WEEKLY -> lastDate.plusWeeks(2);
            case QUARTERLY -> lastDate.plusMonths(3);
            case YEARLY -> lastDate.plusYears(1);
        };
    }

    @Transactional
    public RecurringTransactionDto createRecurringTransaction(Long userId, RecurringTransactionDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = null;
        if (dto.accountId() != null) {
            account = accountRepository.findById(dto.accountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
            if (!account.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("Access denied to account");
            }
        }

        RecurringTransaction recurring = RecurringTransaction.builder()
                .user(user)
                .account(account)
                .merchantName(dto.merchantName())
                .amount(dto.amount())
                .frequency(dto.frequency())
                .lastDate(dto.lastDate())
                .nextDate(dto.nextDate())
                .active(true)
                .build();

        return mapToDto(recurringRepository.save(recurring));
    }

    @Transactional
    public RecurringTransactionDto updateRecurringTransaction(Long userId, Long id, RecurringTransactionDto dto) {
        RecurringTransaction recurring = recurringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found"));

        if (!recurring.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        if (dto.accountId() != null) {
            Account account = accountRepository.findById(dto.accountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
            if (!account.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("Access denied to account");
            }
            recurring.setAccount(account);
        } else {
            recurring.setAccount(null);
        }

        recurring.setMerchantName(dto.merchantName());
        recurring.setAmount(dto.amount());
        recurring.setFrequency(dto.frequency());
        recurring.setLastDate(dto.lastDate());
        recurring.setNextDate(dto.nextDate());
        recurring.setActive(dto.active());

        return mapToDto(recurringRepository.save(recurring));
    }

    @Transactional
    public void deleteRecurringTransaction(Long userId, Long id) {
        RecurringTransaction recurring = recurringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found"));

        if (!recurring.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        recurring.setDeletedAt(java.time.LocalDateTime.now());
        recurring.setActive(false);
        recurringRepository.save(recurring);
    }

    private RecurringTransactionDto mapToDto(RecurringTransaction r) {
        return RecurringTransactionDto.builder()
                .id(r.getId())
                .accountId(r.getAccount() != null ? r.getAccount().getId() : null)
                .accountName(r.getAccount() != null ? r.getAccount().getName() : null)
                .merchantName(r.getMerchantName())
                .amount(r.getAmount())
                .frequency(r.getFrequency())
                .lastDate(r.getLastDate())
                .nextDate(r.getNextDate())
                .active(r.isActive())
                .build();
    }
}
