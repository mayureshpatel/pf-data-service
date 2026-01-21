package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.TransactionDto;
import com.mayureshpatel.pfdataservice.repository.specification.TransactionSpecification;
import com.mayureshpatel.pfdataservice.repository.specification.TransactionSpecification.TransactionFilter;
import com.mayureshpatel.pfdataservice.model.Account;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.repository.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import com.mayureshpatel.pfdataservice.model.VendorRule;
import com.mayureshpatel.pfdataservice.service.categorization.VendorCleaner;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final VendorCleaner vendorCleaner;

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
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this account");
        }

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setDate(dto.date());
        transaction.setAmount(dto.amount());
        transaction.setDescription(dto.description());
        transaction.setType(dto.type());

        // Set original vendor name, default to description if not provided
        String originalVendor = (dto.originalVendorName() != null && !dto.originalVendorName().isBlank())
                ? dto.originalVendorName() : dto.description();
        transaction.setOriginalVendorName(originalVendor);

        if (dto.vendorName() != null && !dto.vendorName().isBlank()) {
            transaction.setVendorName(dto.vendorName());
        } else {
            List<VendorRule> rules = vendorCleaner.loadRulesForUser(userId);
            // Clean based on original vendor name if possible, else description
            transaction.setVendorName(vendorCleaner.cleanVendorName(originalVendor, rules));
        }

        if (dto.categoryName() != null && !dto.categoryName().isBlank()) {
            categoryRepository.findByUserId(userId).stream()
                    .filter(c -> c.getName().equalsIgnoreCase(dto.categoryName()))
                    .findFirst()
                    .ifPresent(transaction::setCategory);
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
            throw new EntityNotFoundException("One or more transactions not found");
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
        transaction.setDate(dto.date());
        transaction.setDescription(dto.description());
        transaction.setType(dto.type());

        // Update original vendor name if provided
        if (dto.originalVendorName() != null && !dto.originalVendorName().isBlank()) {
            transaction.setOriginalVendorName(dto.originalVendorName());
        }

        if (dto.vendorName() != null && !dto.vendorName().isBlank()) {
            transaction.setVendorName(dto.vendorName());
        } else {
            List<VendorRule> rules = vendorCleaner.loadRulesForUser(userId);
            // Clean based on original vendor name
            transaction.setVendorName(vendorCleaner.cleanVendorName(transaction.getOriginalVendorName(), rules));
        }

        if (dto.categoryName() != null && !dto.categoryName().isBlank()) {
            categoryRepository.findByUserId(userId).stream()
                    .filter(c -> c.getName().equalsIgnoreCase(dto.categoryName()))
                    .findFirst()
                    .ifPresent(transaction::setCategory);
        } else {
            transaction.setCategory(null);
        }

        // Apply new amount effect
        account.applyTransaction(transaction);

        return mapToDto(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionDto updateTransaction(Long userId, Long transactionId, TransactionDto dto) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        if (!transaction.getAccount().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this transaction");
        }

        return updateTransactionFromDto(userId, transaction, dto);
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        if (!transaction.getAccount().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this transaction");
        }

        transaction.getAccount().undoTransaction(transaction);

        transactionRepository.delete(transaction);
    }

    private TransactionDto mapToDto(Transaction t) {
        return TransactionDto.builder()
                .id(t.getId())
                .date(t.getDate())
                .amount(t.getAmount())
                .description(t.getDescription())
                .originalVendorName(t.getOriginalVendorName())
                .type(t.getType())
                .vendorName(t.getVendorName())
                .categoryName(t.getCategory() != null ? t.getCategory().getName() : null)
                .accountId(t.getAccount() != null ? t.getAccount().getId() : null)
                .build();
    }
}