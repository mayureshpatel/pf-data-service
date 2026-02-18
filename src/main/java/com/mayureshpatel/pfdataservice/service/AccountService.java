package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Retrieves all accounts for a user.
     *
     * @param userId the user id
     * @return the list of accounts
     */
    @Transactional(readOnly = true)
    public List<AccountDto> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Creates a new account for a user.
     *
     * @param userId     the user id
     * @param accountDto the account dto
     * @return the created account dto
     */
    @Transactional
    public AccountDto createAccount(Long userId, AccountDto accountDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = new Account();
        account.setName(accountDto.name());
        account.setType(accountDto.type());
        account.setCurrentBalance(accountDto.currentBalance());
        account.setBankName(accountDto.bankName());
        account.setUser(user);

        return mapToDto(accountRepository.save(account));
    }

    /**
     * Updates an account owned by the user.
     *
     * @param userId    the user id
     * @param accountId the account id
     * @param dto       the account dto
     * @return the updated account dto
     */
    @Transactional
    public AccountDto updateAccount(Long userId, Long accountId, AccountDto dto) {
        Account account = accountRepository.findByAccountIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        account.setName(dto.name());
        account.setType(dto.type());
        account.setBankName(dto.bankName());

        return mapToDto(accountRepository.save(account));
    }

    /**
     * Reconciles the account balance with the target balance.
     *
     * @param userId        the user id
     * @param accountId     the account id
     * @param targetBalance the target balance
     * @return the updated account
     */
    @Transactional
    public AccountDto reconcileAccount(Long userId, Long accountId, BigDecimal targetBalance) {
        Account account = accountRepository.findByAccountIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // calculate the difference between current and target balance
        BigDecimal currentBalance = account.getCurrentBalance();
        BigDecimal diff = targetBalance.subtract(currentBalance);

        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            return mapToDto(account);
        }

        // create an adjustment transaction
        Transaction adjustment = createAdjustmentTransaction(account, diff);

        // update account balance
        account.applyTransaction(adjustment);
        return mapToDto(accountRepository.save(account));
    }

    private Transaction createAdjustmentTransaction(Account account, BigDecimal diff) {
        Transaction adjustment = new Transaction();
        adjustment.setAccount(account);
        adjustment.setAmount(diff);
        adjustment.setTransactionDate(OffsetDateTime.now());
        adjustment.setType(TransactionType.ADJUSTMENT);
        adjustment.setDescription("Balance Reconciliation");
        adjustment.getMerchant().setOriginalName("Balance Reconciliation");

        return transactionRepository.save(adjustment);
    }

    /**
     * Deletes an account.
     * <br><br>
     * Accounts cannot be deleted if they have any transactions associated with them.
     *
     * @param userId    the user id
     * @param accountId the account id
     */
    @Transactional
    public void deleteAccount(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        // check if an account has any transactions
        long transactionCount = transactionRepository.countByUserId(userId);
        if (transactionCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete account with existing transactions. " +
                            "Please delete or move the " + transactionCount + " transaction(s) first."
            );
        }

        accountRepository.delete(account);
    }

    /**
     * Maps an {@link Account} to a {@link AccountDto}
     *
     * @param account the account to map
     * @return the mapped {@link AccountDto}
     */
    private AccountDto mapToDto(Account account) {
        return new AccountDto(
                account.getId(),
                account.getUser(),
                account.getName(),
                account.getType(),
                account.getCurrentBalance(),
                account.getCurrency(),
                account.getBankName()
        );
    }
}
