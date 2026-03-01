package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.transaction.CategoryTransactionsDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransferSuggestionDto;
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
        List<Transaction> transactions = transactionRepository.findAllById(transactionIds);

        for (Transaction t : transactions) {
            if (!t.getAccount().getUser().getId().equals(userId)) {
                throw new AccessDeniedException("Access denied for transaction " + t.getId());
            }

            t.getAccount().undoTransaction(t);

            if (t.getType() == TransactionType.INCOME) {
                t.setType(TransactionType.TRANSFER_IN);
            } else if (t.getType() == TransactionType.EXPENSE) {
                t.setType(TransactionType.TRANSFER_OUT);
            } else {
                t.setType(TransactionType.TRANSFER_OUT);
            }

            t.setCategory(null);
            t.getAccount().applyTransaction(t);
        }

        transactionRepository.saveAll(transactions);
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

        List<Transaction> transactions = transactionRepository.findAllById(transactionIds);

        long ownedCount = transactions.stream()
                .filter(t -> t.getAccount() != null
                        && t.getAccount().getUser() != null
                        && userId.equals(t.getAccount().getUser().getId()))
                .count();

        if (ownedCount != transactionIds.size()) {
            throw new AccessDeniedException("You do not own one or more of these transactions");
        }

        for (Transaction t : transactions) {
            t.getAccount().undoTransaction(t);
        }

        transactionRepository.deleteAll(transactions);
    }

    @Transactional
    public TransactionDto createTransaction(Long userId, TransactionDto dto) {
        if (dto.account() == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        Account account = accountRepository.findById(dto.account().id())
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

        Merchant merchant = new Merchant();
        if (dto.merchant() != null && dto.merchant().cleanName() != null) {
            merchant.setCleanName(dto.merchant().cleanName());
            merchant.setOriginalName(dto.merchant().originalName());
        } else {
            merchant.setCleanName(dto.description());
            merchant.setOriginalName(dto.description());
        }
        transaction.setMerchant(merchant);

        if (dto.category() != null) {
            Category category = categoryRepository.findById(dto.category().id())
                    .orElse(null);
            if (category != null) {
                if (!category.isSubCategory()) {
                    throw new IllegalArgumentException(
                            "Only subcategories can be assigned to transactions. " +
                                    "Please select a specific subcategory under '" + category.getName() + "'.");
                }
                transaction.setCategory(category);
            }
        } else {
            List<CategoryRule> rules = categoryRuleRepository.findByUserId(userId);
            List<Category> userCategories = categoryRepository.findByUserId(userId);
            Long categoryId = categorizer.guessCategory(transaction, rules, userCategories);

            if (categoryId > 0) {
                userCategories.stream()
                        .filter(c -> c.getId().equals(categoryId))
                        .findFirst()
                        .ifPresent(transaction::setCategory);
            }
        }

        account.applyTransaction(transaction);

        return TransactionDtoMapper.toDto(transactionRepository.save(transaction));
    }

    @Transactional
    public List<TransactionDto> updateTransactions(Long userId, List<TransactionDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return List.of();

        List<Long> ids = dtos.stream().map(TransactionDto::id).toList();
        List<Transaction> transactions = transactionRepository.findAllById(ids);

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
        Account account = transaction.getAccount();
        account.undoTransaction(transaction);

        transaction.setAmount(dto.amount());
        transaction.setTransactionDate(dto.date());
        transaction.setDescription(dto.description());
        transaction.setType(dto.type());

        if (dto.merchant() != null) {
            Merchant merchant = transaction.getMerchant() != null ? transaction.getMerchant() : new Merchant();
            if (dto.merchant().cleanName() != null) {
                merchant.setCleanName(dto.merchant().cleanName());
            }
            if (dto.merchant().originalName() != null) {
                merchant.setOriginalName(dto.merchant().originalName());
            }
            transaction.setMerchant(merchant);
        }

        if (dto.category() != null) {
            Category category = categoryRepository.findById(dto.category().id())
                    .orElse(null);
            if (category != null) {
                if (!category.isSubCategory()) {
                    throw new IllegalArgumentException(
                            "Only subcategories can be assigned to transactions. " +
                                    "Please select a specific subcategory under '" + category.getName() + "'.");
                }
                transaction.setCategory(category);
            }
        } else {
            List<CategoryRule> rules = categoryRuleRepository.findByUserId(userId);
            List<Category> userCategories = categoryRepository.findByUserId(userId);
            Long categoryId = categorizer.guessCategory(transaction, rules, userCategories);

            if (categoryId > 0) {
                userCategories.stream()
                        .filter(c -> c.getId().equals(categoryId))
                        .findFirst()
                        .ifPresent(transaction::setCategory);
            } else {
                transaction.setCategory(null);
            }
        }

        account.applyTransaction(transaction);

        return TransactionDtoMapper.toDto(transactionRepository.save(transaction));
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
