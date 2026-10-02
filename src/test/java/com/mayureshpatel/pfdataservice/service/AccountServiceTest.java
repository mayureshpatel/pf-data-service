package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.account.AccountCreateRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.account.AccountReconcileRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountUpdateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Verifies {@code AccountService}'s CRUD and reconciliation methods, one {@code @Nested} class per method below. */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Unit Tests")
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private RecurringTransactionRepository recurringTransactionRepository;

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
            BigDecimal target = new BigDecimal("1000.00");
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
            BigDecimal target = new BigDecimal("1000.00");
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
            BigDecimal target = new BigDecimal("1000.00");
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
