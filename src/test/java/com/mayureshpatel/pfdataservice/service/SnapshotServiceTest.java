package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.model.Account;
import com.mayureshpatel.pfdataservice.model.AccountSnapshot;
import com.mayureshpatel.pfdataservice.repository.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.AccountSnapshotRepository;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private AccountSnapshotRepository snapshotRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private SnapshotService snapshotService;

    @Test
    void createEndOfMonthSnapshot_ShouldCalculateCorrectly() {
        Long accountId = 1L;
        LocalDate date = LocalDate.of(2026, 1, 15);
        LocalDate endOfMonth = LocalDate.of(2026, 1, 31);

        Account account = new Account();
        account.setId(accountId);
        account.setCurrentBalance(BigDecimal.valueOf(1000)); // Balance NOW (Feb 2026 or later potentially)

        // Net Flow AFTER Jan 31st = +200.
        // So Balance on Jan 31st = 1000 - 200 = 800.
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.getNetFlowAfterDate(accountId, endOfMonth)).thenReturn(BigDecimal.valueOf(200));
        when(snapshotRepository.findByAccountIdAndSnapshotDate(accountId, endOfMonth)).thenReturn(Optional.empty());

        snapshotService.createEndOfMonthSnapshot(accountId, date);

        ArgumentCaptor<AccountSnapshot> captor = ArgumentCaptor.forClass(AccountSnapshot.class);
        verify(snapshotRepository).save(captor.capture());

        AccountSnapshot saved = captor.getValue();
        assertThat(saved.getSnapshotDate()).isEqualTo(endOfMonth);
        assertThat(saved.getBalance()).isEqualByComparingTo("800");
    }
}
