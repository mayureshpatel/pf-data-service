package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.BalanceMode;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.account.AccountCreateRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.account.AccountReconcileRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountSnapshotDto;
import com.mayureshpatel.pfdataservice.dto.account.AccountUpdateRequest;
import com.mayureshpatel.pfdataservice.dto.account.BalanceEntryRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.AccountDtoMapper;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.account.AccountSnapshotRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Business logic for managing user accounts: creation, updates, balance reconciliation, and
 * deletion. Enforces account ownership on every operation that touches an existing account.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final String ACCOUNT_NOT_FOUND = "Account not found";

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final AccountSnapshotRepository snapshotRepository;

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
     * Creates a new account for a user. A manual-balance account created with a non-zero starting
     * balance also gets a balance-history entry dated today, so the balance and its history agree.
     *
     * @param userId  the user id
     * @param request the create account request
     * @return the created account dto
     */
    @Transactional
    public int createAccount(Long userId, AccountCreateRequest request) {
        this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        int accountId = accountRepository.insert(userId, request);

        // a hand-entered account starts with a balance, so record it in the history too
        BigDecimal startingBalance = request.getStartingBalance();
        if (BalanceMode.MANUAL.name().equals(request.getBalanceMode())
                && startingBalance != null && startingBalance.signum() != 0) {
            snapshotRepository.upsert((long) accountId, LocalDate.now(), startingBalance);
        }
        return accountId;
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

        accountRepository.findByIdAndUserId(request.getId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        return accountRepository.update(userId, request);
    }

    /**
     * Reconciles the account balance with the target balance.
     * <p>
     * The conflict-prone {@code reconcile} update runs before the adjustment transaction is
     * inserted (PF-857) -- reversed from this method's original order. A concurrent modification
     * between the initial read and the reconcile call means {@code diff} was computed against an
     * already-stale balance; committing an adjustment transaction sized for that diff before
     * finding out about the conflict would permanently leave a wrongly-sized transaction behind.
     * With nothing committed until {@code reconcile} itself succeeds, a conflict here leaves no
     * partial state to clean up -- {@link OptimisticLockingFailureException} propagates as-is
     * (handled generically, see {@code GlobalExceptionHandler}) rather than being silently
     * retried: the whole point of this operation is reconciling against one specific snapshot, so
     * a caller whose snapshot just went stale needs to know and decide whether to reconcile again
     * with fresh data, not have that decision made for them.
     *
     * <p>
     * On a manual-balance account there is nothing to adjust, so this delegates to
     * {@link #recordBalance} with today's date and the return value is the snapshot id instead.
     *
     * @param userId        the user id
     * @param request       the reconcile request
     * @return the generated id of the adjustment transaction created to account for the
     *         difference, or {@code 0} if the target balance already matched (no adjustment
     *         needed); on a manual-balance account, the id of the stored snapshot
     * @throws OptimisticLockingFailureException if the account was concurrently modified
     */
    @Transactional
    public int reconcileAccount(Long userId, AccountReconcileRequest request) {
        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(ACCOUNT_NOT_FOUND));

        // a hand-entered balance has no transactions to adjust: record it as today's balance instead
        if (account.isManualBalance()) {
            return recordBalance(userId, new BalanceEntryRequest(
                    request.getAccountId(), request.getNewBalance(), LocalDate.now(), request.getVersion()));
        }

        // calculate the difference between current and target balance
        BigDecimal currentBalance = account.getCurrentBalance();
        BigDecimal diff = request.getNewBalance().subtract(currentBalance);

        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }

        accountRepository.reconcile(userId, request.getAccountId(), request.getNewBalance(), request.getVersion());

        // create an adjustment transaction -- only reached once reconcile has actually succeeded
        TransactionCreateRequest adjustmentTransaction = createAdjustmentTransaction(account, diff);
        return this.transactionRepository.insert(adjustmentTransaction);
    }

    /**
     * Records a hand-entered balance for a manual-balance account as of a date. The snapshot for
     * that date is inserted or replaced; only when that date is the account's newest snapshot is
     * {@code current_balance} also set (with the optimistic-locking version check), so back-filling
     * an older reading never overwrites a newer one. No adjustment transaction is ever created.
     *
     * @param userId  the user id
     * @param request the balance entry
     * @return the id of the stored snapshot
     * @throws IllegalArgumentException          if the account's balance is derived, not hand-entered
     * @throws OptimisticLockingFailureException if the account was modified since {@code request.version()} was read
     */
    @Transactional
    public int recordBalance(Long userId, BalanceEntryRequest request) {
        Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(ACCOUNT_NOT_FOUND));

        if (!account.isManualBalance()) {
            throw new IllegalArgumentException("Balance can only be entered on a manual-balance account.");
        }

        // checked up front so a stale entry fails even when it is a back-dated one that never touches the account row
        if (account.getVersion() != null && !account.getVersion().equals(request.version())) {
            throw new OptimisticLockingFailureException("Account balance entry failed due to concurrent modification");
        }

        int snapshotId = snapshotRepository.upsert(request.accountId(), request.asOfDate(), request.balance());

        boolean isLatest = snapshotRepository.findLatestSnapshotDate(request.accountId())
                .map(latest -> !latest.isAfter(request.asOfDate()))
                .orElse(true);
        if (isLatest) {
            accountRepository.reconcile(userId, request.accountId(), request.balance(), request.version());
        }
        return snapshotId;
    }

    /**
     * Lists an account's recorded balances, newest first.
     *
     * @param userId    the user id
     * @param accountId the account id
     * @return the balance history, empty if none has been recorded
     */
    @Transactional(readOnly = true)
    public List<AccountSnapshotDto> getBalanceHistory(Long userId, Long accountId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ACCOUNT_NOT_FOUND));
        AccountDto accountDto = AccountDtoMapper.toDto(account);

        return snapshotRepository.findAllByAccountIdNewestFirst(accountId).stream()
                .map(snapshot -> new AccountSnapshotDto(
                        snapshot.getId(), accountDto, snapshot.getSnapshotDate(), snapshot.getBalance()))
                .toList();
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
     * Accounts cannot be deleted if they have any transactions or recurring transactions
     * associated with them.
     *
     * @param userId    the user id
     * @param accountId the account id
     * @throws AccessDeniedException if the user does not own the account
     * @throws IllegalStateException if the account has any transactions or recurring transactions
     */
    @Transactional
    public int deleteAccount(Long userId, Long accountId) throws AccessDeniedException, IllegalStateException {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(ACCOUNT_NOT_FOUND));

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

        // check if an account has any recurring transaction templates
        long recurringCount = recurringTransactionRepository.countByAccountId(accountId);
        if (recurringCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete account with existing recurring transactions. " +
                            "Please delete or reassign the " + recurringCount + " recurring transaction(s) first."
            );
        }

        return accountRepository.deleteById(accountId, userId);
    }
}
