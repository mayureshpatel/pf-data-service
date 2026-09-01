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

/**
 * CRUD and querying for transactions. Every mutation that changes a transaction's amount or type
 * keeps the owning account's running balance in sync: it undoes the transaction's old effect (if
 * any), applies the new one, and persists the balance with an optimistic-locking check so a
 * concurrent update can't silently overwrite another one.
 */
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
    private final MerchantService merchantService;

    /**
     * Finds transaction pairs in the last 5 years that look like transfers between the user's
     * own accounts.
     *
     * @param userId the user id
     * @return candidate transfer pairs, empty if none found
     */
    public List<TransferSuggestionDto> findPotentialTransfers(Long userId) {
        LocalDate startDate = LocalDate.now().minusYears(5);
        List<Transaction> transactions = transactionRepository.findRecentNonTransferTransactions(userId, startDate);

        return transferMatcher.findMatches(transactions);
    }

    /**
     * Confirms a list of transactions as transfers: each one's type is switched to
     * {@code TRANSFER_IN} (if it was income) or {@code TRANSFER_OUT} (otherwise), so it's
     * excluded from income/expense aggregates going forward.
     *
     * @param userId         the user id
     * @param transactionIds the transaction ids to mark
     * @throws AccessDeniedException     if any transaction belongs to a different user
     * @throws ResourceNotFoundException if any transaction id doesn't exist
     */
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
            int updatedRows = accountRepository.updateBalance(userId, finalAccount.getId(), finalAccount.getCurrentBalance(), account.getVersion());
            if (updatedRows == 0) {
                throw new org.springframework.dao.OptimisticLockingFailureException("Account balance update failed due to concurrent modification");
            }
        }

        transactionRepository.updateAll(userId, updatedTransactions);
    }

    /**
     * Returns a paginated page of transactions filtered only by type.
     *
     * @param userId   the user id
     * @param type     the transaction type to filter by
     * @param pageable page number/size/sort
     * @return the matching page of transactions
     */
    public Page<TransactionDto> getTransactions(Long userId, TransactionType type, Pageable pageable) {
        TransactionFilter filter = new TransactionFilter(null, type, null, null, null, null, null, null, null);
        return getTransactions(userId, filter, pageable);
    }

    /**
     * Returns a paginated page of transactions matching an arbitrary filter.
     *
     * @param userId   the user id
     * @param filter   the filter criteria to apply
     * @param pageable page number/size/sort
     * @return the matching page of transactions
     */
    public Page<TransactionDto> getTransactions(Long userId, TransactionFilter filter, Pageable pageable) {
        return transactionRepository.findAll(TransactionSpecification.withFilter(userId, filter), pageable)
                .map(TransactionDtoMapper::toDto);
    }

    /**
     * Deletes multiple transactions, reverting each one's effect on its account's balance first.
     * A no-op if {@code transactionIds} is null or empty.
     *
     * @param userId         the user id
     * @param transactionIds the transaction ids to delete
     * @throws ResourceNotFoundException if any transaction id doesn't exist
     * @throws AccessDeniedException     if any transaction belongs to a different user
     */
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
            int updatedRows = accountRepository.updateBalance(userId, accountAfterUndo.getId(), accountAfterUndo.getCurrentBalance(), account.getVersion());
            if (updatedRows == 0) {
                throw new org.springframework.dao.OptimisticLockingFailureException("Account balance update failed due to concurrent modification");
            }
        }

        transactionRepository.deleteAll(userId, transactions);
    }

    /**
     * Creates a new transaction, resolving its merchant and category (explicit if given,
     * otherwise auto-guessed from the user's rules) and applying it to the account's balance.
     *
     * @param userId  the user id
     * @param request the transaction to create
     * @return the new transaction's generated id
     * @throws ResourceNotFoundException if the account doesn't exist
     * @throws AccessDeniedException     if the account belongs to a different user
     */
    @Transactional
    public int createTransaction(Long userId, TransactionCreateRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this account");
        }

        Long merchantId = merchantService.findOrCreateMerchant(userId, request.getDescription());

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionDate(request.getTransactionDate())
                .postDate(request.getPostDate())
                .amount(request.getAmount())
                .description(request.getDescription())
                .type(TransactionType.valueOf(request.getType()))
                .merchant(Merchant.builder().id(merchantId).build())
                .build();

        transaction = resolveCategory(userId, transaction, request.getCategoryId());

        Account finalAccount = account.applyTransaction(transaction);
        int updatedRows = accountRepository.updateBalance(userId, finalAccount.getId(), finalAccount.getCurrentBalance(), account.getVersion());
        if (updatedRows == 0) {
            throw new org.springframework.dao.OptimisticLockingFailureException("Account balance update failed due to concurrent modification");
        }

        return transactionRepository.insert(transaction);
    }

    /**
     * Updates multiple transactions by calling {@link #updateTransaction} once per request.
     *
     * @param userId   the user id
     * @param requests the transactions to update, each including its id
     * @return the total number of transactions updated
     */
    @Transactional
    public Integer updateTransactionsBulk(Long userId, List<TransactionUpdateRequest> requests) {
        if (requests == null || requests.isEmpty()) return 0;

        return requests.stream()
                .map(request -> updateTransaction(userId, request))
                .toList().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Updates a single transaction: reverts its old effect on the account balance, re-resolves
     * its merchant and category, then reapplies the updated transaction to the balance.
     *
     * @param userId  the user id
     * @param request the transaction to update, including its id
     * @return the number of rows updated (0 or 1)
     * @throws ResourceNotFoundException if the transaction doesn't exist
     * @throws AccessDeniedException     if the transaction belongs to a different user
     */
    @Transactional
    public int updateTransaction(Long userId, TransactionUpdateRequest request) {
        Transaction transaction = transactionRepository.findById(request.getId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!transaction.getAccount().getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this transaction");
        }

        Account account = transaction.getAccount();
        Account accountAfterUndo = account.undoTransaction(transaction);

        Long merchantId = merchantService.findOrCreateMerchant(userId, request.getDescription());

        Transaction updatedT = transaction.toBuilder()
                .amount(request.getAmount())
                .transactionDate(request.getTransactionDate())
                .postDate(request.getPostDate())
                .description(request.getDescription())
                .type(TransactionType.valueOf(request.getType()))
                .merchant(Merchant.builder().id(merchantId).build())
                .build();

        updatedT = resolveCategory(userId, updatedT, request.getCategoryId());

        Account finalAccount = accountAfterUndo.applyTransaction(updatedT);
        int updatedRows = accountRepository.updateBalance(userId, finalAccount.getId(), finalAccount.getCurrentBalance(), account.getVersion());
        if (updatedRows == 0) {
            throw new org.springframework.dao.OptimisticLockingFailureException("Account balance update failed due to concurrent modification");
        }

        return transactionRepository.update(userId, updatedT);
    }

    /**
     * Resolves a transaction's category: uses the explicit category if one was requested
     * (verifying it belongs to the user and is a subcategory, since only subcategories can be
     * assigned directly), otherwise auto-guesses one from the user's category rules, otherwise
     * leaves it uncategorized.
     *
     * @param userId              the user id
     * @param transaction         the transaction to categorize
     * @param requestedCategoryId an explicit category id, or {@code null} to auto-guess
     * @return the transaction with its category set (possibly to {@code null})
     * @throws ResourceNotFoundException if the requested category doesn't exist
     * @throws AccessDeniedException     if the requested category belongs to a different user
     * @throws IllegalArgumentException  if the requested category isn't a subcategory
     */
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

    /**
     * Deletes a single transaction, reverting its effect on the account's balance.
     *
     * @param userId        the user id
     * @param transactionId the transaction id to delete
     * @throws ResourceNotFoundException if the transaction doesn't exist
     * @throws AccessDeniedException     if the transaction belongs to a different user
     */
    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!transaction.getAccount().getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this transaction");
        }

        Account accountAfterUndo = transaction.getAccount().undoTransaction(transaction);
        int updatedRows = accountRepository.updateBalance(userId, accountAfterUndo.getId(), accountAfterUndo.getCurrentBalance(), transaction.getAccount().getVersion());
        if (updatedRows == 0) {
            throw new org.springframework.dao.OptimisticLockingFailureException("Account balance update failed due to concurrent modification");
        }

        transactionRepository.deleteById(transactionId, userId);
    }

    /**
     * Returns the user's transaction count grouped by category.
     *
     * @param userId the user id
     * @return per-category transaction counts
     */
    public List<CategoryTransactionsDto> getCountByCategory(Long userId) {
        return transactionRepository.getCountByCategory(userId);
    }

    /**
     * Returns only the categories that have at least one of the user's transactions assigned.
     *
     * @param userId the user id
     * @return categories with at least one transaction
     */
    public List<CategoryDto> getCategoriesWithTransactions(Long userId) {
        List<Category> categories = transactionRepository.getCategoriesWithTransactions(userId);
        return categories.stream().map(CategoryDtoMapper::toDto).toList();
    }

    /**
     * Returns only the merchants that have at least one of the user's transactions assigned.
     *
     * @param userId the user id
     * @return merchants with at least one transaction
     */
    public List<MerchantDto> getMerchantsWithTransactions(Long userId) {
        return transactionRepository.getMerchantsWithTransactions(userId)
                .stream()
                .map(MerchantDtoMapper::toDto)
                .toList();
    }
}
