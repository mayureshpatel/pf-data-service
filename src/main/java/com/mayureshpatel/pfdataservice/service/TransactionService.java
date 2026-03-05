package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.transaction.*;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.CategoryDtoMapper;
import com.mayureshpatel.pfdataservice.mapper.MerchantDtoMapper;
import com.mayureshpatel.pfdataservice.mapper.TransactionDtoMapper;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification;
import com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification.TransactionFilter;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import com.mayureshpatel.pfdataservice.service.transfer.TransferMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionCategorizer categorizer;
    private final CategoryRuleRepository categoryRuleRepository;
    private final TransferMatcher transferMatcher;

    public List<TransferSuggestionDto> findPotentialTransfers(Long userId) {
        LocalDate startDate = LocalDate.now().minusYears(5);
        List<Transaction> transactions = transactionRepository.findRecentNonTransferTransactions(userId, startDate);

        return transferMatcher.findMatches(transactions);
    }

    @Transactional
    public void markAsTransfer(Long userId, List<Long> transactionIds) {
        List<Transaction> transactions = transactionRepository.findAllById(userId, transactionIds);
        for (Transaction t : transactions) {
            if (!t.getAccount().getUserId().equals(userId)) {
                throw new AccessDeniedException("Access denied for transaction " + t.getId());
            }
        }
        
        if (transactions.size() != transactionIds.size()) {
            throw new ResourceNotFoundException("One or more transactions not found");
        }

        List<Transaction> updatedTransactions = new ArrayList<>();
        for (Transaction t : transactions) {
            Account account = accountRepository.findById(t.getAccount().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
            Account accountAfterUndo = account.undoTransaction(t);

            TransactionType newType;
            if (t.getType() == TransactionType.INCOME) {
                newType = TransactionType.TRANSFER_IN;
            } else {
                newType = TransactionType.TRANSFER_OUT;
            }

            Transaction updatedT = t.toBuilder().type(newType).build();
            Account finalAccount = accountAfterUndo.applyTransaction(updatedT);

            updatedTransactions.add(updatedT);
            accountRepository.updateBalance(userId, finalAccount.getId(), finalAccount.getCurrentBalance(), account.getVersion());
        }

        transactionRepository.updateAll(userId, updatedTransactions);
    }

    public Page<TransactionDto> getTransactions(Long userId, TransactionType type, Pageable pageable) {
        TransactionFilter filter = new TransactionFilter(null, type, null, null, null, null, null, null, null);
        return getTransactions(userId, filter, pageable);
    }

    public Page<TransactionDto> getTransactions(Long userId, TransactionFilter filter, Pageable pageable) {
        return transactionRepository.findAll(TransactionSpecification.withFilter(userId, filter), pageable)
                .map(TransactionDtoMapper::toDto);
    }

    @Transactional
    public void deleteTransactions(Long userId, List<Long> transactionIds) {
        if (transactionIds == null || transactionIds.isEmpty()) return;

        List<Transaction> transactions = transactionRepository.findAllById(userId, transactionIds);

        if (transactions.size() != transactionIds.size()) {
            throw new ResourceNotFoundException("One or more transactions not found");
        }

        for (Transaction t : transactions) {
            if (t.getAccount() == null || !userId.equals(t.getAccount().getUserId())) {
                throw new AccessDeniedException("You do not own transaction " + t.getId());
            }
        }

        for (Transaction t : transactions) {
            Account account = accountRepository.findById(t.getAccount().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
            Account accountAfterUndo = account.undoTransaction(t);
            accountRepository.updateBalance(userId, accountAfterUndo.getId(), accountAfterUndo.getCurrentBalance(), account.getVersion());
        }

        transactionRepository.deleteAll(userId, transactions);
    }

    @Transactional
    public int createTransaction(Long userId, TransactionCreateRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this account");
        }

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionDate(request.getTransactionDate())
                .postDate(request.getPostDate())
                .amount(request.getAmount())
                .description(request.getDescription())
                .type(TransactionType.valueOf(request.getType()))
                .merchant(Merchant.builder().id(request.getMerchantId()).build())
                .build();

        transaction = resolveCategory(userId, transaction, request.getCategoryId());

        Account finalAccount = account.applyTransaction(transaction);
        accountRepository.updateBalance(userId, finalAccount.getId(), finalAccount.getCurrentBalance(), account.getVersion());

        return transactionRepository.insert(transaction);
    }

    @Transactional
    public Integer updateTransactionsBulk(Long userId, List<TransactionUpdateRequest> requests) {
        if (requests == null || requests.isEmpty()) return 0;

        return requests.stream()
                .map(request -> updateTransaction(userId, request))
                .toList().stream().mapToInt(Integer::intValue).sum();
    }

    @Transactional
    public int updateTransaction(Long userId, TransactionUpdateRequest request) {
        Transaction transaction = transactionRepository.findById(request.getId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!transaction.getAccount().getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this transaction");
        }

        Account account = transaction.getAccount();
        Account accountAfterUndo = account.undoTransaction(transaction);

        Transaction updatedT = transaction.toBuilder()
                .amount(request.getAmount())
                .transactionDate(request.getTransactionDate())
                .postDate(request.getPostDate())
                .description(request.getDescription())
                .type(TransactionType.valueOf(request.getType()))
                .merchant(request.getMerchantId() != null ? Merchant.builder().id(request.getMerchantId()).build() : null)
                .build();

        updatedT = resolveCategory(userId, updatedT, request.getCategoryId());

        Account finalAccount = accountAfterUndo.applyTransaction(updatedT);
        accountRepository.updateBalance(userId, finalAccount.getId(), finalAccount.getCurrentBalance(), account.getVersion());

        return transactionRepository.update(userId, updatedT);
    }

    private Transaction resolveCategory(Long userId, Transaction transaction, Long requestedCategoryId) {
        if (requestedCategoryId != null) {
            Category category = categoryRepository.findById(requestedCategoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

            if (!category.getUserId().equals(userId)) {
                throw new AccessDeniedException("You do not have access to this category");
            }

            if (!category.isSubCategory()) {
                throw new IllegalArgumentException(
                        "Only subcategories can be assigned to transactions. " +
                                "Please select a specific subcategory under '" + category.getName() + "'.");
            }
            return transaction.toBuilder().category(category).build();
        } else {
            List<CategoryRule> rules = categoryRuleRepository.findByUserId(userId);
            List<Category> userCategories = categoryRepository.findByUserId(userId);
            Long categoryId = categorizer.guessCategory(transaction, rules, userCategories);

            if (categoryId != null && categoryId > 0) {
                Category guessed = userCategories.stream()
                        .filter(c -> c.getId().equals(categoryId))
                        .findFirst()
                        .orElse(null);
                return transaction.toBuilder().category(guessed).build();
            }
        }
        return transaction.toBuilder().category(null).build();
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!transaction.getAccount().getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this transaction");
        }

        Account accountAfterUndo = transaction.getAccount().undoTransaction(transaction);
        accountRepository.updateBalance(userId, accountAfterUndo.getId(), accountAfterUndo.getCurrentBalance(), transaction.getAccount().getVersion());

        transactionRepository.deleteById(transactionId, userId);
    }

    public List<CategoryTransactionsDto> getCountByCategory(Long userId) {
        return transactionRepository.getCountByCategory(userId);
    }

    public List<CategoryDto> getCategoriesWithTransactions(Long userId) {
        List<Category> categories = transactionRepository.getCategoriesWithTransactions(userId);
        return categories.stream().map(CategoryDtoMapper::toDto).toList();
    }

    public List<MerchantDto> getMerchantsWithTransactions(Long userId) {
        return transactionRepository.getMerchantsWithTransactions(userId)
                .stream()
                .map(MerchantDtoMapper::toDto)
                .toList();
    }
}
