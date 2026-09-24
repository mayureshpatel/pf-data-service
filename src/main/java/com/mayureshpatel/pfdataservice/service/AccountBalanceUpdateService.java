package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.function.UnaryOperator;

/**
 * Applies a delta-shaped change to an account's balance (a transaction being created, undone, or
 * reapplied) with a bounded retry on a concurrent-modification conflict. Extracted from {@code
 * TransactionService}'s original PF-839 fix (the one call site that had this retry) so both {@code
 * TransactionService} and {@code TransactionImportService} can share it (PF-854) rather than
 * duplicating the loop at 8 call sites, or leaving some fixed and others not.
 * <p>
 * Deliberately does not cover {@code AccountService.reconcileAccount}: that method sets an
 * <i>absolute</i> target balance rather than applying a delta, so a conflict-retry there would need
 * to recompute the reconciliation diff against a freshly-fetched balance -- a different problem,
 * left to its own follow-up ticket rather than forced through this delta-shaped abstraction.
 */
@Service
@RequiredArgsConstructor
public class AccountBalanceUpdateService {
    /**
     * How many times {@link #applyWithRetry} retries a concurrent-modification conflict (PF-839,
     * generalized by PF-854) before giving up and letting the exception propagate.
     */
    private static final int MAX_BALANCE_UPDATE_ATTEMPTS = 3;

    private final AccountRepository accountRepository;

    /**
     * Applies {@code transform} to {@code account}'s balance, retrying up to {@value
     * #MAX_BALANCE_UPDATE_ATTEMPTS} times if a concurrent request updates the account first. Each
     * retry re-fetches the account's current state and re-applies {@code transform} against it,
     * rather than blindly repeating the same stale computed balance, which would either fail
     * identically or silently clobber whatever the concurrent request just committed.
     *
     * @param userId    the user id
     * @param account   the account as already fetched by the caller (used as-is for the first
     *                  attempt, to avoid a redundant re-fetch on the common non-conflicting path)
     * @param transform computes the account's new state from its current state -- e.g. {@code acc
     *                  -> acc.applyTransaction(transaction)} or {@code acc ->
     *                  acc.undoTransaction(transaction)}; must be safe to call more than once,
     *                  since a retry re-invokes it against the freshly re-fetched account
     * @return the account's new state, as computed by the attempt that succeeded
     * @throws OptimisticLockingFailureException if every retry attempt still conflicts
     * @throws ResourceNotFoundException         if a retry's re-fetch finds the account gone
     */
    public Account applyWithRetry(Long userId, Account account, UnaryOperator<Account> transform) {
        Account currentAccount = account;
        for (int attempt = 1; attempt <= MAX_BALANCE_UPDATE_ATTEMPTS; attempt++) {
            Account updated = transform.apply(currentAccount);
            try {
                accountRepository.updateBalance(userId, updated.getId(), updated.getCurrentBalance(), currentAccount.getVersion());
                return updated;
            } catch (OptimisticLockingFailureException e) {
                if (attempt == MAX_BALANCE_UPDATE_ATTEMPTS) {
                    throw e;
                }
                currentAccount = accountRepository.findById(currentAccount.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
            }
        }
        // unreachable: the loop above always returns or throws by the final attempt: kept only to
        // satisfy the compiler's definite-return analysis for this non-void method.
        throw new IllegalStateException("unreachable");
    }
}
