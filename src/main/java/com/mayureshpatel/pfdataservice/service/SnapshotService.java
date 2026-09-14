package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountSnapshot;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.account.AccountSnapshotRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Records historical month-end account balances. Snapshots are computed backward from the
 * account's current balance by subtracting everything that happened after the snapshot date,
 * rather than replaying transactions forward from account creation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotService {

    private final AccountRepository accountRepository;
    private final AccountSnapshotRepository snapshotRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Creates or updates a balance snapshot for the end of the given month.
     * Logic: Balance(EndOfMonth) = CurrentBalance - NetFlow(AfterEndOfMonth)
     */
    @Transactional
    public void createEndOfMonthSnapshot(Long userId, Long accountId, LocalDate dateInMonth) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        if (!account.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied to account");
        }

        LocalDate endOfMonth = dateInMonth.withDayOfMonth(dateInMonth.lengthOfMonth());
        BigDecimal historicBalance = calculateEndOfMonthBalance(account, endOfMonth);

        Optional<AccountSnapshot> existing = snapshotRepository.findByAccountIdAndSnapshotDate(accountId, endOfMonth);

        AccountSnapshot snapshot = existing.orElse(AccountSnapshot.builder().build())
                .toBuilder()
                .account(account)
                .snapshotDate(endOfMonth)
                .balance(historicBalance)
                .build();

        if (existing.isPresent()) {
            snapshotRepository.update(snapshot);
        } else {
            snapshotRepository.insert(snapshot);
        }
        log.info("Saved snapshot for Account {} on {}: {}", accountId, endOfMonth, historicBalance);
    }

    /**
     * Computes (without persisting) an account's balance as of the end of the given month.
     * Logic: Balance(EndOfMonth) = CurrentBalance - NetFlow(AfterEndOfMonth). Extracted from
     * {@link #createEndOfMonthSnapshot} (PF-304) so a read-only caller -- a net-worth-over-time
     * report, for one -- can reuse the same math without the write side effect that method's own
     * snapshot-persistence contract requires.
     *
     * @param account      the account to compute a historic balance for
     * @param endOfMonth   the month-end date to compute the balance as of (not normalized here --
     *                     callers control exactly which date this represents)
     * @return the account's computed balance as of {@code endOfMonth}
     */
    public BigDecimal calculateEndOfMonthBalance(Account account, LocalDate endOfMonth) {
        BigDecimal currentBalance = account.getCurrentBalance();

        BigDecimal changesAfterDate = transactionRepository.getNetFlowAfterDate(account.getId(), endOfMonth);
        if (changesAfterDate == null) {
            changesAfterDate = BigDecimal.ZERO;
        }

        return currentBalance.subtract(changesAfterDate);
    }
}
