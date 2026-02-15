package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.AccountDto;
import com.mayureshpatel.pfdataservice.repository.transaction.model.TransactionType;
import com.mayureshpatel.pfdataservice.repository.account.model.Account;
import com.mayureshpatel.pfdataservice.repository.transaction.model.Transaction;
import com.mayureshpatel.pfdataservice.repository.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<AccountDto> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public AccountDto createAccount(Long userId, AccountDto accountDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Account account = new Account();
        account.setName(accountDto.name());
        account.setType(accountDto.type());
        account.setCurrentBalance(accountDto.currentBalance());
        account.setBankName(accountDto.bankName());
        account.setUser(user);

        return mapToDto(accountRepository.save(account));
    }

    @Transactional
    public AccountDto updateAccount(Long userId, Long accountId, AccountDto dto) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
             throw new RuntimeException("Access denied"); // Better to use AccessDeniedException
        }

        account.setName(dto.name());
        account.setType(dto.type());
        account.setBankName(dto.bankName());
        // currentBalance is NOT updated here to ensure data integrity.
        // Use reconcileAccount for balance adjustments.

        return mapToDto(accountRepository.save(account));
    }

    @Transactional
    public AccountDto reconcileAccount(Long userId, Long accountId, BigDecimal targetBalance) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        BigDecimal currentBalance = account.getCurrentBalance();
        BigDecimal diff = targetBalance.subtract(currentBalance);

        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            return mapToDto(account);
        }

        // Create adjustment transaction
        // If diff is positive (Target > Current), we need to ADD money.
        // If diff is negative (Target < Current), we need to SUBTRACT money.
        // Transaction.getNetChange() for ADJUSTMENT returns the raw signed amount.
        Transaction adjustment = new Transaction();
        adjustment.setAccount(account);
        adjustment.setAmount(diff);
        adjustment.setDate(java.time.LocalDate.now());
        adjustment.setType(TransactionType.ADJUSTMENT);
        adjustment.setDescription("Balance Reconciliation (Manual Adjustment)");
        adjustment.setOriginalVendorName("System");
        
        transactionRepository.save(adjustment);
        
        // Update account balance
        account.applyTransaction(adjustment);
        return mapToDto(accountRepository.save(account));
    }

    @Transactional
    public void deleteAccount(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        // Check if account has transactions
        Long transactionCount = transactionRepository.countByAccountId(accountId);
        if (transactionCount > 0) {
            throw new IllegalStateException(
                "Cannot delete account with existing transactions. " +
                "Please delete or move the " + transactionCount + " transaction(s) first."
            );
        }

        accountRepository.delete(account);
    }

    private AccountDto mapToDto(Account account) {
        return new AccountDto(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getCurrentBalance(),
                account.getBankName()
        );
    }
}
