package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.vendor.VendorRule;
import com.mayureshpatel.pfdataservice.dto.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.TransferSuggestionDto;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification;
import com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification.TransactionFilter;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import com.mayureshpatel.pfdataservice.service.categorization.VendorCleaner;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final VendorCleaner vendorCleaner;
    private final TransactionCategorizer categorizer;
    private final CategoryRuleRepository categoryRuleRepository;

    public List<TransferSuggestionDto> findPotentialTransfers(Long userId) {
        // Look back 5 years to cover historical data
        LocalDate startDate = LocalDate.now().minusYears(5);
        List<Transaction> transactions = transactionRepository.findRecentNonTransferTransactions(userId, startDate);

        List<TransferSuggestionDto> suggestions = new ArrayList<>();
        Set<Long> matchedIds = new HashSet<>();

        for (int i = 0; i < transactions.size(); i++) {
            Transaction t1 = transactions.get(i);
            if (matchedIds.contains(t1.getId())) continue;

            for (int j = i + 1; j < transactions.size(); j++) {
                Transaction t2 = transactions.get(j);
                if (matchedIds.contains(t2.getId())) continue;

                long daysDiff = Math.abs(ChronoUnit.DAYS.between(t1.getTransactionDate(), t2.getTransactionDate()));

                // Since list is sorted by date DESC, if diff > 3, subsequent items will also be > 3
                if (daysDiff > 3) {
                    break;
                }

                // Check 1: Amounts are equal (both positive in DB)
                if (t1.getAmount().compareTo(t2.getAmount()) == 0) {

                    // Check 2: Types are different (One Income, One Expense)
                    // This implies money moving OUT of one and INTO another
                    if (t1.getType() != t2.getType()) {

                        // Check 3: Different accounts
                        if (!t1.getAccount().getId().equals(t2.getAccount().getId())) {

                            // High confidence match
                            suggestions.add(new TransferSuggestionDto(
                                    mapToDto(t1),
                                    mapToDto(t2),
                                    0.9 - (daysDiff * 0.1) // 0.9 for same day, 0.8 for 1 day diff, etc.
                            ));

                            matchedIds.add(t1.getId());
                            matchedIds.add(t2.getId());
                            break; // Stop looking for match for t1
                        }
                    }
                }
            }
        }
        return suggestions;
    }

    @Transactional
    public void markAsTransfer(Long userId, List<Long> transactionIds) {
        List<Transaction> transactions = transactionRepository.findAllById(transactionIds);

        for (Transaction t : transactions) {
            if (!t.getAccount().getUser().getId().equals(userId)) {
                throw new AccessDeniedException("Access denied for transaction " + t.getId());
            }

            // Invalidate old balance effect
            t.getAccount().undoTransaction(t);

            if (t.getType() == TransactionType.INCOME) {
                t.setType(TransactionType.TRANSFER_IN);
            } else if (t.getType() == TransactionType.EXPENSE) {
                t.setType(TransactionType.TRANSFER_OUT);
            } else {
                // Fallback for existing TRANSFER or other types
                t.setType(TransactionType.TRANSFER_OUT);
            }

            // Optionally clear category if it was categorized
            t.setCategory(null);

            // Re-apply new balance effect (TRANSFER_IN is positive, TRANSFER_OUT is negative)
            t.getAccount().applyTransaction(t);
        }

        transactionRepository.saveAll(transactions);
    }

    public Page<TransactionDto> getTransactions(Long userId, TransactionType type, Pageable pageable) {
        // Legacy support or simple filter
        TransactionFilter filter = new TransactionFilter(null, type, null, null, null, null, null, null, null);
        return getTransactions(userId, filter, pageable);
    }

    public Page<TransactionDto> getTransactions(Long userId, TransactionFilter filter, Pageable pageable) {
        return transactionRepository.findAll(TransactionSpecification.withFilter(userId, filter), pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public void deleteTransactions(Long userId, List<Long> transactionIds) {
        if (transactionIds == null || transactionIds.isEmpty()) return;

        // Validate ownership in one query
        long ownedCount = transactionRepository.countByIdInAndAccount_User_Id(transactionIds, userId);
        if (ownedCount != transactionIds.size()) {
            throw new AccessDeniedException("You do not own one or more of these transactions");
        }

        List<Transaction> transactions = transactionRepository.findAllById(transactionIds);
        for (Transaction t : transactions) {
            t.getAccount().undoTransaction(t);
        }

        transactionRepository.deleteAll(transactions);
    }

    @Transactional
    public TransactionDto createTransaction(Long userId, TransactionDto dto) {
        Account account = accountRepository.findById(dto.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this account");
        }

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionDate(dto.date());
        transaction.setAmount(dto.amount());
        transaction.setDescription(dto.description());
        transaction.setType(dto.type());

        // Set original vendor name, default to description if not provided
        String originalVendor = (dto.originalVendorName() != null && !dto.originalVendorName().isBlank())
                ? dto.originalVendorName() : dto.description();
        transaction.setOriginalVendorName(originalVendor);

        if (dto.vendorName() != null && !dto.vendorName().isBlank()) {
            transaction.getVendor().setName(dto.vendorName());
        } else {
            List<VendorRule> rules = vendorCleaner.loadRulesForUser(userId);
            // Clean based on original vendor name if possible, else description
            transaction.getVendor().setName(vendorCleaner.cleanVendorName(originalVendor, rules));
        }

        if (dto.categoryName() != null && !dto.categoryName().isBlank()) {
            Category category = categoryRepository.findByUserId(userId).stream()
                    .filter(c -> c.getName().equalsIgnoreCase(dto.categoryName()))
                    .findFirst()
                    .orElse(null);

            if (category != null) {
                // VALIDATION: Only allow child categories on transactions
                if (category.isParent()) {
                    throw new IllegalArgumentException(
                            "Only subcategories can be assigned to transactions. " +
                                    "Please select a specific subcategory under '" + category.getName() + "'."
                    );
                }
                transaction.setCategory(category);
            }
        } else {
            // Smart Categorization
            List<CategoryRule> rules = categoryRuleRepository.findByUserId(userId);
            List<Category> userCategories = categoryRepository.findByUserId(userId);
            String suggestedCategory = categorizer.guessCategory(transaction, rules, userCategories);

            if (!"Uncategorized".equals(suggestedCategory)) {
                userCategories.stream()
                        .filter(c -> c.getName().equalsIgnoreCase(suggestedCategory))
                        .findFirst()
                        .ifPresent(transaction::setCategory);
            }
        }

        // update account balance
        account.applyTransaction(transaction);

        return mapToDto(transactionRepository.save(transaction));
    }

    @Transactional
    public List<TransactionDto> updateTransactions(Long userId, List<TransactionDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return List.of();

        List<Long> ids = dtos.stream().map(TransactionDto::id).toList();
        List<Transaction> transactions = transactionRepository.findAllByIdWithAccountAndUser(ids);

        // Validate ownership and existence
        if (transactions.size() != dtos.size()) {
            throw new ResourceNotFoundException("One or more transactions not found");
        }

        return dtos.stream().map(dto -> {
            Transaction transaction = transactions.stream()
                    .filter(t -> t.getId().equals(dto.id()))
                    .findFirst()
                    .orElseThrow();

            if (!transaction.getAccount().getUser().getId().equals(userId)) {
                throw new AccessDeniedException("You do not own transaction ID: " + transaction.getId());
            }

            return updateTransactionFromDto(userId, transaction, dto);
        }).toList();
    }

    private TransactionDto updateTransactionFromDto(Long userId, Transaction transaction, TransactionDto dto) {
        // Reverse old amount effect
        Account account = transaction.getAccount();
        account.undoTransaction(transaction);

        transaction.setAmount(dto.amount());
        transaction.setTransactionDate(dto.date());
        transaction.setDescription(dto.description());
        transaction.setType(dto.type());

        // Update original vendor name if provided
        if (dto.originalVendorName() != null && !dto.originalVendorName().isBlank()) {
            transaction.setOriginalVendorName(dto.originalVendorName());
        }

        if (dto.vendorName() != null && !dto.vendorName().isBlank()) {
            transaction.getVendor().setName(dto.vendorName());
        } else {
            List<VendorRule> rules = vendorCleaner.loadRulesForUser(userId);
            // Clean based on original vendor name
            transaction.getVendor().setName(vendorCleaner.cleanVendorName(transaction.getOriginalVendorName(), rules));
        }

        if (dto.categoryName() != null && !dto.categoryName().isBlank()) {
            Category category = categoryRepository.findByUserId(userId).stream()
                    .filter(c -> c.getName().equalsIgnoreCase(dto.categoryName()))
                    .findFirst()
                    .orElse(null);

            if (category != null) {
                // VALIDATION: Only allow child categories on transactions
                if (category.isParent()) {
                    throw new IllegalArgumentException(
                            "Only subcategories can be assigned to transactions. " +
                                    "Please select a specific subcategory under '" + category.getName() + "'."
                    );
                }
                transaction.setCategory(category);
            }
        } else {
            // Smart Categorization
            List<CategoryRule> rules = categoryRuleRepository.findByUserId(userId);
            List<Category> userCategories = categoryRepository.findByUserId(userId);
            String suggestedCategory = categorizer.guessCategory(transaction, rules, userCategories);

            if (!"Uncategorized".equals(suggestedCategory)) {
                userCategories.stream()
                        .filter(c -> c.getName().equalsIgnoreCase(suggestedCategory))
                        .findFirst()
                        .ifPresent(transaction::setCategory);
            } else {
                transaction.setCategory(null);
            }
        }

        // Apply new amount effect
        account.applyTransaction(transaction);

        return mapToDto(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionDto updateTransaction(Long userId, Long transactionId, TransactionDto dto) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!transaction.getAccount().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this transaction");
        }

        return updateTransactionFromDto(userId, transaction, dto);
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!transaction.getAccount().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this transaction");
        }

        transaction.getAccount().undoTransaction(transaction);

        transactionRepository.delete(transaction);
    }

    private TransactionDto mapToDto(Transaction t) {
        return TransactionDto.builder()
                .id(t.getId())
                .date(t.getTransactionDate())
                .amount(t.getAmount())
                .description(t.getDescription())
                .originalVendorName(t.getOriginalVendorName())
                .type(t.getType())
                .vendorName(t.getVendor().getName())
                .categoryName(t.getCategory() != null ? t.getCategory().getName() : null)
                .accountId(t.getAccount() != null ? t.getAccount().getId() : null)
                .build();
    }
}