package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PF-854: the retry logic extracted from PF-839's original {@code TransactionService}-only fix.
 * These tests own the retry-mechanics coverage for every call site that now delegates here --
 * per-site tests (in {@code TransactionServiceTest} and {@code TransactionImportServiceTest})
 * verify each site passes the *correct transform*, not that retrying itself works, matching the
 * whole point of extracting a shared helper in the first place.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountBalanceUpdateService Unit Tests")
class AccountBalanceUpdateServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountBalanceUpdateService accountBalanceUpdateService;

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;

    private Account createAccount(BigDecimal balance, Long version) {
        return Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(balance).version(version).build();
    }

    @Test
    @DisplayName("should apply the transform and update the balance on the first attempt when there's no conflict")
    void shouldSucceedOnFirstAttempt() {
        // arrange
        Account account = createAccount(new BigDecimal("100.00"), 1L);

        // act
        Account result = accountBalanceUpdateService.applyWithRetry(USER_ID, account, acc -> acc.toBuilder().currentBalance(new BigDecimal("150.00")).build());

        // assert & verify
        assertEquals(new BigDecimal("150.00"), result.getCurrentBalance());
        verify(accountRepository).updateBalance(USER_ID, ACCOUNT_ID, new BigDecimal("150.00"), 1L);
        verify(accountRepository, never()).findById(any());
    }

    @Test
    @DisplayName("bug regression: should retry, not fail outright, on a single optimistic-locking "
            + "conflict (PF-839, generalized by PF-854)")
    void shouldRetryAndSucceedAfterOptimisticLockingConflict() {
        // arrange -- first attempt conflicts (as if another request updated the account first);
        // the retry re-fetches and finds the account at a newer version
        Account staleAccount = createAccount(new BigDecimal("100.00"), 1L);
        Account refreshedAccount = createAccount(new BigDecimal("120.00"), 2L);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(refreshedAccount));
        when(accountRepository.updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong()))
                .thenThrow(new OptimisticLockingFailureException("conflict"))
                .thenReturn(1);

        // act -- transform adds a flat $10.00 regardless of starting balance
        Account result = accountBalanceUpdateService.applyWithRetry(USER_ID, staleAccount,
                acc -> acc.toBuilder().currentBalance(acc.getCurrentBalance().add(BigDecimal.TEN)).build());

        // assert & verify -- retry re-applied the transform against the REFRESHED balance (120 +
        // 10 = 130), not the stale first-read one (100 + 10 = 110)
        assertEquals(new BigDecimal("130.00"), result.getCurrentBalance());
        verify(accountRepository, times(1)).findById(ACCOUNT_ID);
        verify(accountRepository).updateBalance(USER_ID, ACCOUNT_ID, new BigDecimal("110.00"), 1L);
        verify(accountRepository).updateBalance(USER_ID, ACCOUNT_ID, new BigDecimal("130.00"), 2L);
    }

    @Test
    @DisplayName("should throw OptimisticLockingFailureException if every retry attempt still conflicts")
    void shouldThrowAfterExhaustingRetries() {
        // arrange
        Account account = createAccount(new BigDecimal("100.00"), 1L);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong()))
                .thenThrow(new OptimisticLockingFailureException("conflict"));

        // act & assert & verify -- gives up after 3 attempts total, not an infinite/unbounded retry
        assertThrows(OptimisticLockingFailureException.class,
                () -> accountBalanceUpdateService.applyWithRetry(USER_ID, account, acc -> acc));
        verify(accountRepository, times(3)).updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong());
        verify(accountRepository, times(2)).findById(ACCOUNT_ID);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException if the account is gone by the time a retry re-fetches it")
    void shouldThrowIfAccountGoneOnRetryRefetch() {
        // arrange -- the first attempt conflicts, and by the time the retry re-fetches, the
        // account has vanished
        Account account = createAccount(new BigDecimal("100.00"), 1L);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
        when(accountRepository.updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong()))
                .thenThrow(new OptimisticLockingFailureException("conflict"));

        // act & assert & verify
        assertThrows(ResourceNotFoundException.class,
                () -> accountBalanceUpdateService.applyWithRetry(USER_ID, account, acc -> acc));
        verify(accountRepository, times(1)).updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong());
    }
}
