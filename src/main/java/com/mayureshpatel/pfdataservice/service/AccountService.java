package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.account.AccountCreateRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.account.AccountReconcileRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountUpdateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.AccountDtoMapper;
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
    public List<AccountDto> getAllAccountsByUserId(Long userId) {
        return accountRepository.findAllByUserId(userId).stream()
                .map(AccountDtoMapper::toDto)
                .toList();
    }

    /**
     * Creates a new account for a user.
     *
     * @param userId  the user id
     * @param request the create account request
     * @return the created account dto
     */
    @Transactional
    public int createAccount(Long userId, AccountCreateRequest request) {
        this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        return accountRepository.insert(userId, request);
    }

    /**
     * Updates an account owned by the user.
     *
     * @param userId  the user id
     * @param request the account update request
     * @return the updated account dto
     */
    @Transactional
    public int updateAccount(Long userId, AccountUpdateRequest request) {
        this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Account account = accountRepository.findByIdAndUserId(request.getId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        if (!account.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        return accountRepository.update(userId, request);
    }

    /**
     * Reconciles the account balance with the target balance.
     *
     * @param userId        the user id
     * @param request       the reconcile request
     * @return the updated account
     */
    @Transactional
    public int reconcileAccount(Long userId, AccountReconcileRequest request) {
        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // calculate the difference between current and target balance
        BigDecimal currentBalance = account.getCurrentBalance();
        BigDecimal diff = request.getNewBalance().subtract(currentBalance);

        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }

        // create an adjustment transaction
        TransactionCreateRequest adjustmentTransaction = createAdjustmentTransaction(account, diff);
        this.transactionRepository.insert(adjustmentTransaction);

        // update account balance
        account.applyTransaction(adjustmentTransaction);
        return accountRepository.reconcile(userId, request.getAccountId(), request.getNewBalance(), request.getVersion());
    }

    /**
     * Create an adjustment transaction to reconcile account balance.
     *
     * @param account the account to adjust
     * @param diff    the difference between current and target balance
     * @return the created adjustment transaction
     */
    private TransactionCreateRequest createAdjustmentTransaction(Account account, BigDecimal diff) {
        return TransactionCreateRequest.builder()
                .accountId(account.getId())
                .amount(diff)
                .transactionDate(OffsetDateTime.now())
                .description("Balance Reconciliation")
                .type(TransactionType.ADJUSTMENT.name())
                .build();
    }

    /**
     * Deletes an account.
     * <br><br>
     * Accounts cannot be deleted if they have any transactions associated with them.
     *
     * @param userId    the user id
     * @param accountId the account id
     * @throws AccessDeniedException if the user does not own the account
     * @throws IllegalStateException if the account has any transactions
     */
    @Transactional
    public int deleteAccount(Long userId, Long accountId) throws AccessDeniedException, IllegalStateException {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        // check if an account has any transactions
        long transactionCount = transactionRepository.countByAccountId(accountId);
        if (transactionCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete account with existing transactions. " +
                            "Please delete or move the " + transactionCount + " transaction(s) first."
            );
        }

        return accountRepository.deleteById(accountId, userId);
    }
}
