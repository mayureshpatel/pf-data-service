package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountSnapshot;
import com.mayureshpatel.pfdataservice.domain.account.BalanceMode;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.account.AccountCreateRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.account.AccountReconcileRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountSnapshotDto;
import com.mayureshpatel.pfdataservice.dto.account.BalanceEntryRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountUpdateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.account.AccountSnapshotRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Verifies {@code AccountService}'s CRUD and reconciliation methods, one {@code @Nested} class per method below. */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Unit Tests")
class AccountServiceTest {
    private static final String MONEY_IN = "800.00";
    private static final String RECORDED_BALANCE = "1000.00";

    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private RecurringTransactionRepository recurringTransactionRepository;
    @Mock private AccountSnapshotRepository snapshotRepository;

    @InjectMocks private AccountService accountService;

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;

    /** {@code getAllAccountsByUserId} maps every repository row for the user into an {@link AccountDto}. */
    @Nested
    @DisplayName("getAllAccountsByUserId")
    class GetAllAccountsByUserIdTests {
        @Test
        @DisplayName("should return mapped account DTOs")
        void shouldReturnAccounts() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).name("Checking").build();
            when(accountRepository.findAllByUserId(USER_ID)).thenReturn(List.of(account));

            // act
            List<AccountDto> result = accountService.getAllAccountsByUserId(USER_ID);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals("Checking", result.get(0).name());
            assertEquals(BalanceMode.DERIVED, result.get(0).balanceMode());
            assertNull(result.get(0).moneyIn());
            assertNull(result.get(0).gain());
        }

        @Test
        @DisplayName("should expose moneyIn and a gain of latest balance minus moneyIn for a manual account")
        void shouldExposeMoneyInAndGain() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).name("Brokerage")
                    .balanceMode(BalanceMode.MANUAL)
                    .moneyIn(new BigDecimal(MONEY_IN))
                    .latestRecordedBalance(new BigDecimal(RECORDED_BALANCE))
                    .build();
            when(accountRepository.findAllByUserId(USER_ID)).thenReturn(List.of(account));

            // act
            AccountDto result = accountService.getAllAccountsByUserId(USER_ID).get(0);

            // assert & verify
            assertEquals(BalanceMode.MANUAL, result.balanceMode());
            assertEquals(0, new BigDecimal(MONEY_IN).compareTo(result.moneyIn()));
            assertEquals(0, new BigDecimal("200.00").compareTo(result.gain()));
        }

        @Test
        @DisplayName("should have a null gain when a manual account has no balance entry yet")
        void shouldHaveNullGainWithoutBalanceEntry() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).name("Brokerage")
                    .balanceMode(BalanceMode.MANUAL)
                    .moneyIn(new BigDecimal(MONEY_IN))
                    .build();
            when(accountRepository.findAllByUserId(USER_ID)).thenReturn(List.of(account));

            // act
            AccountDto result = accountService.getAllAccountsByUserId(USER_ID).get(0);

            // assert & verify
            assertEquals(0, new BigDecimal(MONEY_IN).compareTo(result.moneyIn()));
            assertNull(result.gain());
        }

        @Test
        @DisplayName("should ignore money-in figures on a derived account")
        void shouldNullMoneyInForDerived() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).name("Checking")
                    .moneyIn(new BigDecimal(MONEY_IN))
                    .latestRecordedBalance(new BigDecimal(RECORDED_BALANCE))
                    .build();
            when(accountRepository.findAllByUserId(USER_ID)).thenReturn(List.of(account));

            // act
            AccountDto result = accountService.getAllAccountsByUserId(USER_ID).get(0);

            // assert & verify
            assertNull(result.moneyIn());
            assertNull(result.gain());
        }
    }

    /** {@code createAccount} requires the owning user to exist first, throwing {@link ResourceNotFoundException} otherwise, before delegating the insert to the repository. */
    @Nested
    @DisplayName("createAccount")
    class CreateAccountTests {
        @Test
        @DisplayName("should create account successfully")
        void shouldCreate() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            when(accountRepository.insert(eq(USER_ID), any())).thenReturn(1);

            AccountCreateRequest request = AccountCreateRequest.builder().name("New").build();

            // act
            int result = accountService.createAccount(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
            verify(accountRepository).insert(USER_ID, request);
        }

        @Test
        @DisplayName("should pass a MANUAL balanceMode to the repository and write a balance entry dated today")
        void shouldWriteHistoryRowForManualStartingBalance() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            when(accountRepository.insert(eq(USER_ID), any())).thenReturn(5);
            AccountCreateRequest request = AccountCreateRequest.builder().name("Brokerage")
                    .balanceMode("MANUAL").startingBalance(new BigDecimal("500.00")).build();

            // act
            int result = accountService.createAccount(USER_ID, request);

            // assert & verify
            assertEquals(5, result);
            verify(accountRepository).insert(USER_ID, request);
            verify(snapshotRepository).upsert(5L, LocalDate.now(), new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("should not write a balance entry for a MANUAL account with a zero or missing starting balance")
        void shouldNotWriteHistoryRowForZeroManualBalance() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            when(accountRepository.insert(eq(USER_ID), any())).thenReturn(5);

            // act
            accountService.createAccount(USER_ID, AccountCreateRequest.builder().name("A")
                    .balanceMode("MANUAL").startingBalance(BigDecimal.ZERO).build());
            accountService.createAccount(USER_ID, AccountCreateRequest.builder().name("B")
                    .balanceMode("MANUAL").build());

            // assert & verify
            verifyNoInteractions(snapshotRepository);
        }

        @Test
        @DisplayName("should not write a balance entry for a DERIVED account")
        void shouldNotWriteHistoryRowForDerived() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            when(accountRepository.insert(eq(USER_ID), any())).thenReturn(5);

            // act
            accountService.createAccount(USER_ID, AccountCreateRequest.builder().name("C")
                    .startingBalance(new BigDecimal("500.00")).build());

            // assert & verify
            verifyNoInteractions(snapshotRepository);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if user not found")
        void shouldThrowOnUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> accountService.createAccount(USER_ID, null));
        }
    }

    /** {@code updateAccount} requires both the user and, scoped to that user, the account to exist -- throwing {@link ResourceNotFoundException} for whichever is missing -- before delegating the update to the repository. */
    @Nested
    @DisplayName("updateAccount")
    class UpdateAccountTests {
        @Test
        @DisplayName("should update account successfully if owned")
        void shouldUpdate() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
            when(accountRepository.update(eq(USER_ID), any())).thenReturn(1);

            AccountUpdateRequest request = AccountUpdateRequest.builder().id(ACCOUNT_ID).name("Updated").build();

            // act
            int result = accountService.updateAccount(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
            verify(accountRepository).update(eq(USER_ID), any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if user not found during update")
        void shouldThrowOnUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> accountService.updateAccount(USER_ID, AccountUpdateRequest.builder().build()));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if account not found during update")
        void shouldThrowOnAccountNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().build()));
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> accountService.updateAccount(USER_ID, AccountUpdateRequest.builder().id(ACCOUNT_ID).build()));
        }

    }

    /**
     * {@code reconcileAccount} inserts an adjustment transaction sized for the gap between the
     * account's current balance and the requested target (returning that adjustment transaction's
     * own id, not the repository's rows-affected count from {@code reconcile()} -- a PF-837 bug
     * this suite's first test deliberately stubs two different return values for so it can't pass
     * by coincidence), does nothing and returns 0 when the balances already match, and -- a PF-857
     * fix -- only inserts the adjustment transaction *after* {@code reconcile()} itself succeeds,
     * so a concurrent modification that makes {@code reconcile()} throw {@link
     * OptimisticLockingFailureException} never leaves a stale, already-committed adjustment
     * transaction behind.
     */
    @Nested
    @DisplayName("reconcileAccount")
    class ReconcileAccountTests {
        @Test
        @DisplayName("should return the real generated adjustment-transaction id, not "
                + "accountRepository.reconcile()'s rows-affected count (PF-837) -- insert() and "
                + "reconcile() are stubbed to return two DIFFERENT values specifically so this "
                + "test can't pass by coincidence the way the pre-fix version did (both happened "
                + "to be 1)")
        void shouldReconcile() {
            // arrange
            BigDecimal target = new BigDecimal(RECORDED_BALANCE);
            Long version = 1L;
            Long realAdjustmentTransactionId = 555L;
            AccountReconcileRequest request = new AccountReconcileRequest(ACCOUNT_ID, target, version);
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(new BigDecimal("900.00")).version(version).build();
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.insert(any(TransactionCreateRequest.class))).thenReturn(realAdjustmentTransactionId.intValue());
            when(accountRepository.reconcile(USER_ID, ACCOUNT_ID, target, version)).thenReturn(1);

            // act
            int result = accountService.reconcileAccount(USER_ID, request);

            // assert & verify
            assertEquals(realAdjustmentTransactionId.intValue(), result);
            verify(transactionRepository).insert((TransactionCreateRequest) argThat(req -> ((TransactionCreateRequest) req).getAmount().compareTo(new BigDecimal("100.00")) == 0));
            verify(accountRepository).reconcile(USER_ID, ACCOUNT_ID, target, version);
        }

        @Test
        @DisplayName("should return 0 if target balance matches current balance")
        void shouldReturnZeroIfBalancesMatch() {
            // arrange
            BigDecimal target = new BigDecimal(RECORDED_BALANCE);
            AccountReconcileRequest request = new AccountReconcileRequest(ACCOUNT_ID, target, 1L);
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(target).build();
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));

            // act
            int result = accountService.reconcileAccount(USER_ID, request);

            // assert & verify
            assertEquals(0, result);
            verify(transactionRepository, never()).insert(any(TransactionCreateRequest.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if account missing during reconcile")
        void shouldThrowOnAccountNotFound() {
            AccountReconcileRequest request = new AccountReconcileRequest(ACCOUNT_ID, BigDecimal.TEN, 1L);
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> accountService.reconcileAccount(USER_ID, request));
        }

        @Test
        @DisplayName("bug regression (PF-857): should not commit an adjustment transaction if "
                + "reconcile() itself conflicts -- pre-fix, the transaction was inserted BEFORE "
                + "the conflict-prone reconcile() call, so a concurrent modification left a "
                + "permanently-committed adjustment transaction sized for a diff computed against "
                + "a balance that was already stale by the time reconcile() even ran. Proven via "
                + "revert-and-confirm-fail: reverting the production fix makes this test fail with "
                + "transactionRepository.insert(...) actually having been called")
        void shouldNotCommitAdjustmentTransactionWhenReconcileConflicts() {
            // arrange
            BigDecimal target = new BigDecimal(RECORDED_BALANCE);
            Long version = 1L;
            AccountReconcileRequest request = new AccountReconcileRequest(ACCOUNT_ID, target, version);
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(new BigDecimal("900.00")).version(version).build();
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
            doThrow(new OptimisticLockingFailureException("conflict"))
                    .when(accountRepository).reconcile(USER_ID, ACCOUNT_ID, target, version);

            // act & assert & verify
            assertThrows(OptimisticLockingFailureException.class, () -> accountService.reconcileAccount(USER_ID, request));
            verify(transactionRepository, never()).insert(any(TransactionCreateRequest.class));
        }
    }

    /**
     * {@code recordBalance} upserts the snapshot for the entered date, only moves {@code
     * current_balance} when that date is the newest snapshot, rejects derived accounts and stale
     * versions, and never creates an adjustment transaction. {@code reconcileAccount} on a manual
     * account delegates to it with today's date.
     */
    @Nested
    @DisplayName("recordBalance")
    class RecordBalanceTests {
        private final LocalDate today = LocalDate.of(2026, 11, 11);

        private Account manualAccount() {
            return Account.builder().id(ACCOUNT_ID).userId(USER_ID).balanceMode(BalanceMode.MANUAL)
                    .currentBalance(new BigDecimal("12000.00")).version(3L).build();
        }

        @Test
        @DisplayName("should upsert the snapshot and set the current balance when the date is the latest")
        void shouldSetCurrentBalanceWhenLatest() {
            // arrange
            LocalDate date = LocalDate.of(2026, 11, 10);
            BigDecimal balance = new BigDecimal("12500.00");
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(manualAccount()));
            when(snapshotRepository.upsert(ACCOUNT_ID, date, balance)).thenReturn(77);
            when(snapshotRepository.findLatestSnapshotDate(ACCOUNT_ID)).thenReturn(Optional.of(date));

            // act
            int result = accountService.recordBalance(USER_ID, new BalanceEntryRequest(ACCOUNT_ID, balance, date, 3L));

            // assert & verify
            assertEquals(77, result);
            verify(snapshotRepository).upsert(ACCOUNT_ID, date, balance);
            verify(accountRepository).reconcile(USER_ID, ACCOUNT_ID, balance, 3L);
            verify(transactionRepository, never()).insert(any(TransactionCreateRequest.class));
        }

        @Test
        @DisplayName("should leave the current balance alone when an earlier date than the latest is entered")
        void shouldNotTouchCurrentBalanceWhenEarlier() {
            // arrange
            LocalDate date = LocalDate.of(2026, 10, 1);
            BigDecimal balance = new BigDecimal("11000.00");
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(manualAccount()));
            when(snapshotRepository.upsert(ACCOUNT_ID, date, balance)).thenReturn(5);
            when(snapshotRepository.findLatestSnapshotDate(ACCOUNT_ID)).thenReturn(Optional.of(LocalDate.of(2026, 11, 10)));

            // act
            accountService.recordBalance(USER_ID, new BalanceEntryRequest(ACCOUNT_ID, balance, date, 3L));

            // assert & verify
            verify(snapshotRepository).upsert(ACCOUNT_ID, date, balance);
            verify(accountRepository, never()).reconcile(any(), any(), any(), any());
            verify(transactionRepository, never()).insert(any(TransactionCreateRequest.class));
        }

        @Test
        @DisplayName("should reject a derived account")
        void shouldRejectDerivedAccount() {
            // arrange
            Account derived = Account.builder().id(ACCOUNT_ID).userId(USER_ID).balanceMode(BalanceMode.DERIVED).version(3L).build();
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(derived));

            // act & assert & verify
            assertThrows(IllegalArgumentException.class, () -> accountService.recordBalance(USER_ID,
                    new BalanceEntryRequest(ACCOUNT_ID, BigDecimal.TEN, today, 3L)));
            verify(snapshotRepository, never()).upsert(any(), any(), any());
        }

        @Test
        @DisplayName("should throw the optimistic-locking conflict when the version is stale")
        void shouldThrowOnStaleVersion() {
            // arrange
            LocalDate date = LocalDate.of(2026, 11, 10);
            BigDecimal balance = new BigDecimal("12500.00");
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(manualAccount()));
            lenient().when(snapshotRepository.findLatestSnapshotDate(ACCOUNT_ID)).thenReturn(Optional.of(date));
            lenient().doThrow(new OptimisticLockingFailureException("conflict"))
                    .when(accountRepository).reconcile(USER_ID, ACCOUNT_ID, balance, 2L);

            // act & assert & verify
            assertThrows(OptimisticLockingFailureException.class, () -> accountService.recordBalance(USER_ID,
                    new BalanceEntryRequest(ACCOUNT_ID, balance, date, 2L)));
            verify(snapshotRepository, never()).upsert(any(), any(), any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when the account is not the user's")
        void shouldThrowWhenAccountMissing() {
            // arrange
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> accountService.recordBalance(USER_ID,
                    new BalanceEntryRequest(ACCOUNT_ID, BigDecimal.TEN, today, 3L)));
        }

        @Test
        @DisplayName("reconcileAccount on a manual account records a balance for today and creates no adjustment")
        void reconcileOnManualAccountDelegatesToRecordBalance() {
            // arrange
            BigDecimal target = new BigDecimal("13000.00");
            LocalDate date = LocalDate.now();
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(manualAccount()));
            when(snapshotRepository.upsert(ACCOUNT_ID, date, target)).thenReturn(42);
            when(snapshotRepository.findLatestSnapshotDate(ACCOUNT_ID)).thenReturn(Optional.of(date));

            // act
            int result = accountService.reconcileAccount(USER_ID, new AccountReconcileRequest(ACCOUNT_ID, target, 3L));

            // assert & verify
            assertEquals(42, result);
            verify(snapshotRepository).upsert(ACCOUNT_ID, date, target);
            verify(accountRepository).reconcile(USER_ID, ACCOUNT_ID, target, 3L);
            verify(transactionRepository, never()).insert(any(TransactionCreateRequest.class));
        }
    }

    /** {@code getBalanceHistory} returns the account's snapshots newest first, and an empty list (not an error) when there are none. */
    @Nested
    @DisplayName("getBalanceHistory")
    class GetBalanceHistoryTests {
        @Test
        @DisplayName("should return snapshots newest first as dtos")
        void shouldReturnHistory() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).balanceMode(BalanceMode.MANUAL).build();
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
            when(snapshotRepository.findAllByAccountIdNewestFirst(ACCOUNT_ID)).thenReturn(List.of(
                    AccountSnapshot.builder().id(2L).accountId(ACCOUNT_ID).snapshotDate(LocalDate.of(2026, 11, 10)).balance(new BigDecimal("12500.00")).build(),
                    AccountSnapshot.builder().id(1L).accountId(ACCOUNT_ID).snapshotDate(LocalDate.of(2026, 10, 10)).balance(new BigDecimal("12000.00")).build()));

            // act
            List<AccountSnapshotDto> result = accountService.getBalanceHistory(USER_ID, ACCOUNT_ID);

            // assert & verify
            assertEquals(2, result.size());
            assertEquals(LocalDate.of(2026, 11, 10), result.get(0).snapshotDate());
            assertEquals(new BigDecimal("12000.00"), result.get(1).balance());
        }

        @Test
        @DisplayName("should return an empty list for an account with no history")
        void shouldReturnEmptyList() {
            // arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
            when(snapshotRepository.findAllByAccountIdNewestFirst(ACCOUNT_ID)).thenReturn(List.of());

            // act
            List<AccountSnapshotDto> result = accountService.getBalanceHistory(USER_ID, ACCOUNT_ID);

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when the account is not the user's")
        void shouldThrowWhenMissing() {
            // arrange
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> accountService.getBalanceHistory(USER_ID, ACCOUNT_ID));
        }
    }

    /**
     * {@code deleteAccount} enforces, in order: the user exists ({@link
     * ResourceNotFoundException}), the account exists ({@link ResourceNotFoundException}), the
     * user owns it ({@link AccessDeniedException}), and the account has neither transactions nor
     * dependent recurring transactions (PF-192) -- either one throws {@link
     * IllegalStateException} naming the actual blocking count, and only a fully clean account
     * reaches the repository delete.
     */
    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccountTests {
        @Test
        @DisplayName("should delete account if owned and has no transactions")
        void shouldDelete() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.countByAccountId(ACCOUNT_ID)).thenReturn(0L);
            when(recurringTransactionRepository.countByAccountId(ACCOUNT_ID)).thenReturn(0L);
            when(accountRepository.deleteById(ACCOUNT_ID, USER_ID)).thenReturn(1);

            // act
            int result = accountService.deleteAccount(USER_ID, ACCOUNT_ID);

            // assert & verify
            assertEquals(1, result);
            verify(accountRepository).deleteById(ACCOUNT_ID, USER_ID);
        }

        @Test
        @DisplayName("should throw AccessDeniedException if not owned during delete")
        void shouldThrowOnAccessDenied() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            Account otherAccount = Account.builder().id(ACCOUNT_ID).userId(999L).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(otherAccount));

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> accountService.deleteAccount(USER_ID, ACCOUNT_ID));
        }

        @Test
        @DisplayName("should throw IllegalStateException if account has transactions")
        void shouldThrowOnExistingTransactions() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.countByAccountId(ACCOUNT_ID)).thenReturn(5L);

            // act & assert & verify
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> accountService.deleteAccount(USER_ID, ACCOUNT_ID));
            assertTrue(ex.getMessage().contains("5 transaction(s)"));
        }

        @Test
        @DisplayName("should throw IllegalStateException if account has dependent recurring transactions (PF-192)")
        void shouldThrowOnExistingRecurringTransactions() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.countByAccountId(ACCOUNT_ID)).thenReturn(0L);
            when(recurringTransactionRepository.countByAccountId(ACCOUNT_ID)).thenReturn(2L);

            // act & assert & verify
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> accountService.deleteAccount(USER_ID, ACCOUNT_ID));
            assertTrue(ex.getMessage().contains("2 recurring transaction(s)"));
            verify(accountRepository, never()).deleteById(any(), any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if user not found during delete")
        void shouldThrowOnUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> accountService.deleteAccount(USER_ID, ACCOUNT_ID));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if account not found during delete")
        void shouldThrowOnAccountNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().build()));
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> accountService.deleteAccount(USER_ID, ACCOUNT_ID));
        }
    }
}
