package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.repository.account.model.Account;
import com.mayureshpatel.pfdataservice.repository.account.model.AccountSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

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
    public void createEndOfMonthSnapshot(Long accountId, LocalDate dateInMonth) {
        LocalDate endOfMonth = dateInMonth.withDayOfMonth(dateInMonth.lengthOfMonth());
        
        // Don't create snapshots for the future (or today if month isn't over? actually we can, it's just a snapshot at that time)
        // But typically "End of Month" implies the month is closed. 
        // For now, we allow calculating it as "Balance on that date".
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        BigDecimal currentBalance = account.getCurrentBalance();
        
        // Calculate transactions that happened AFTER the snapshot date up to NOW
        BigDecimal changesAfterDate = transactionRepository.getNetFlowAfterDate(accountId, endOfMonth);
        
        if (changesAfterDate == null) {
            changesAfterDate = BigDecimal.ZERO;
        }

        // Historic Balance = Current - (Changes that happened later)
        BigDecimal historicBalance = currentBalance.subtract(changesAfterDate);

        Optional<AccountSnapshot> existing = snapshotRepository.findByAccountIdAndSnapshotDate(accountId, endOfMonth);
        
        AccountSnapshot snapshot = existing.orElse(new AccountSnapshot());
        snapshot.setAccount(account);
        snapshot.setSnapshotDate(endOfMonth);
        snapshot.setBalance(historicBalance);

        snapshotRepository.save(snapshot);
        log.info("Saved snapshot for Account {} on {}: {}", accountId, endOfMonth, historicBalance);
    }
}
