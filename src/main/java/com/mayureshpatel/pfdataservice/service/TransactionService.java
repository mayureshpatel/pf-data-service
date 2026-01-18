package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.CategoryTotal;
import com.mayureshpatel.pfdataservice.dto.DashboardData;
import com.mayureshpatel.pfdataservice.dto.TransactionDto;
import com.mayureshpatel.pfdataservice.model.Account;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public Page<TransactionDto> getTransactions(Long userId, TransactionType type, Pageable pageable) {
        // Note: For advanced filtering, we would use Specification or QueryDSL.
        // For MVP, we'll stick to a simple repo method (needs adding to repo) or filtering in memory if dataset small (bad practice).
        // Let's add findByAccount_User_IdAndType to repo.
        
        Page<Transaction> page;
        if (type != null) {
            page = transactionRepository.findByAccount_User_IdAndType(userId, type, pageable);
        } else {
            page = transactionRepository.findByAccount_User_IdOrderByDateDesc(userId, pageable);
        }
        
        return page.map(this::mapToDto);
    }

    @Transactional
    public TransactionDto updateTransaction(Long userId, Long transactionId, TransactionDto dto) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        transaction.setAmount(dto.amount());
        transaction.setDate(dto.date());
        transaction.setDescription(dto.description());
        transaction.setType(dto.type());
        
        // TODO: Handle category update logic here when categories are fully implemented
        
        return mapToDto(transactionRepository.save(transaction));
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        // Update account balance before deleting?
        // Ideally yes, but for this MVP import-heavy logic, balance is calculated on import.
        // Let's reverse the transaction effect on balance.
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
                .date(t.getDate())
                .amount(t.getAmount())
                .description(t.getDescription())
                .type(t.getType())
                .categoryName(t.getCategory() != null ? t.getCategory().getName() : null)
                .build();
    }
}