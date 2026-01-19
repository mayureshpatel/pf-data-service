package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.TransactionDto;
import com.mayureshpatel.pfdataservice.model.Account;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.repository.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public Page<TransactionDto> getTransactions(Long userId, TransactionType type, Pageable pageable) {
        Page<Transaction> page;
        if (type != null) {
            page = transactionRepository.findByAccount_User_IdAndType(userId, type, pageable);
        } else {
            page = transactionRepository.findByAccount_User_IdOrderByDateDesc(userId, pageable);
        }

        return page.map(this::mapToDto);
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

        if (dto.categoryName() != null && !dto.categoryName().isBlank()) {
            categoryRepository.findByUserId(userId).stream()
                    .filter(c -> c.getName().equalsIgnoreCase(dto.categoryName()))
                    .findFirst()
                    .ifPresent(transaction::setCategory);
        }

        // update account balance
        if (transaction.getType() == TransactionType.EXPENSE) {
            account.setCurrentBalance(account.getCurrentBalance().subtract(transaction.getAmount()));
        } else {
            account.setCurrentBalance(account.getCurrentBalance().add(transaction.getAmount()));
        }

        return mapToDto(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionDto updateTransaction(Long userId, Long transactionId, TransactionDto dto) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        if (!transaction.getAccount().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this transaction");
        }

        // Reverse old amount effect
        Account account = transaction.getAccount();
        if (transaction.getType() == TransactionType.EXPENSE) {
            account.setCurrentBalance(account.getCurrentBalance().add(transaction.getAmount()));
        } else {
            account.setCurrentBalance(account.getCurrentBalance().subtract(transaction.getAmount()));
        }

        transaction.setAmount(dto.amount());
        transaction.setDate(dto.date());
        transaction.setDescription(dto.description());
        transaction.setType(dto.type());

        if (dto.categoryName() != null && !dto.categoryName().isBlank()) {
            categoryRepository.findByUserId(userId).stream()
                    .filter(c -> c.getName().equalsIgnoreCase(dto.categoryName()))
                    .findFirst()
                    .ifPresent(transaction::setCategory);
        } else {
            transaction.setCategory(null);
        }

        // Apply new amount effect
        if (transaction.getType() == TransactionType.EXPENSE) {
            account.setCurrentBalance(account.getCurrentBalance().subtract(transaction.getAmount()));
        } else {
            account.setCurrentBalance(account.getCurrentBalance().add(transaction.getAmount()));
        }

        return mapToDto(transactionRepository.save(transaction));
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        if (!transaction.getAccount().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this transaction");
        }

        Account account = transaction.getAccount();
        BigDecimal amount = transaction.getAmount();
        if (transaction.getType() == TransactionType.EXPENSE) {
            account.setCurrentBalance(account.getCurrentBalance().add(amount)); // Add back expense
        } else {
            account.setCurrentBalance(account.getCurrentBalance().subtract(amount)); // Remove income
        }

        transactionRepository.delete(transaction);
    }

    private TransactionDto mapToDto(Transaction t) {
        return TransactionDto.builder()
                .id(t.getId())
                .date(t.getDate())
                .amount(t.getAmount())
                .description(t.getDescription())
                .type(t.getType())
                .categoryName(t.getCategory() != null ? t.getCategory().getName() : null)
                .accountId(t.getAccount() != null ? t.getAccount().getId() : null)
                .build();
    }
}