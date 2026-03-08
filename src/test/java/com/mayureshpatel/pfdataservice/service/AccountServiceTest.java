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
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Unit Tests")
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private AccountService accountService;

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;

    @Nested
    @DisplayName("getAllAccountsByUserId")
    class GetAllAccountsByUserIdTests {
        @Test
        @DisplayName("should return mapped account DTOs")
        void shouldReturnAccounts() {
            // Arrange
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).name("Checking").build();
            when(accountRepository.findAllByUserId(USER_ID)).thenReturn(List.of(account));

            // Act
            List<AccountDto> result = accountService.getAllAccountsByUserId(USER_ID);

            // Assert
            assertEquals(1, result.size());
            assertEquals("Checking", result.get(0).name());
        }
    }

    @Nested
    @DisplayName("createAccount")
    class CreateAccountTests {
        @Test
        @DisplayName("should create account successfully")
        void shouldCreate() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            when(accountRepository.insert(eq(USER_ID), any())).thenReturn(1);

            AccountCreateRequest request = AccountCreateRequest.builder().name("New").build();

            // Act
            int result = accountService.createAccount(USER_ID, request);

            // Assert
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

    @Nested
    @DisplayName("updateAccount")
    class UpdateAccountTests {
        @Test
        @DisplayName("should update account successfully if owned")
        void shouldUpdate() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
            when(accountRepository.update(eq(USER_ID), any())).thenReturn(1);

            AccountUpdateRequest request = AccountUpdateRequest.builder().id(ACCOUNT_ID).name("Updated").build();

            // Act
            int result = accountService.updateAccount(USER_ID, request);

            // Assert
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

        @Test
        @DisplayName("should throw AccessDeniedException if user doesn't own account during update")
        void shouldThrowOnAccessDenied() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().build()));
            // Account repo returns account but somehow userId doesn't match (extra safety check in service)
            Account otherAccount = Account.builder().id(ACCOUNT_ID).userId(999L).build();
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(otherAccount));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> accountService.updateAccount(USER_ID, AccountUpdateRequest.builder().id(ACCOUNT_ID).build()));
        }
    }

    @Nested
    @DisplayName("reconcileAccount")
    class ReconcileAccountTests {
        @Test
        @DisplayName("should create adjustment transaction and reconcile balance")
        void shouldReconcile() {
            // Arrange
            BigDecimal target = new BigDecimal("1000.00");
            Long version = 1L;
            AccountReconcileRequest request = new AccountReconcileRequest(ACCOUNT_ID, target, version);
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(new BigDecimal("900.00")).version(version).build();
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
            when(accountRepository.reconcile(USER_ID, ACCOUNT_ID, target, version)).thenReturn(1);

            // Act
            int result = accountService.reconcileAccount(USER_ID, request);

            // Assert
            assertEquals(1, result);
            verify(transactionRepository).insert((TransactionCreateRequest) argThat(req -> ((TransactionCreateRequest) req).getAmount().compareTo(new BigDecimal("100.00")) == 0));
            verify(accountRepository).reconcile(USER_ID, ACCOUNT_ID, target, version);
        }

        @Test
        @DisplayName("should return 0 if target balance matches current balance")
        void shouldReturnZeroIfBalancesMatch() {
            // Arrange
            BigDecimal target = new BigDecimal("1000.00");
            AccountReconcileRequest request = new AccountReconcileRequest(ACCOUNT_ID, target, 1L);
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).currentBalance(target).build();
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));

            // Act
            int result = accountService.reconcileAccount(USER_ID, request);

            // Assert
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
    }

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccountTests {
        @Test
        @DisplayName("should delete account if owned and has no transactions")
        void shouldDelete() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.countByAccountId(ACCOUNT_ID)).thenReturn(0L);
            when(accountRepository.deleteById(ACCOUNT_ID, USER_ID)).thenReturn(1);

            // Act
            int result = accountService.deleteAccount(USER_ID, ACCOUNT_ID);

            // Assert
            assertEquals(1, result);
            verify(accountRepository).deleteById(ACCOUNT_ID, USER_ID);
        }

        @Test
        @DisplayName("should throw AccessDeniedException if not owned during delete")
        void shouldThrowOnAccessDenied() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            Account otherAccount = Account.builder().id(ACCOUNT_ID).userId(999L).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(otherAccount));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> accountService.deleteAccount(USER_ID, ACCOUNT_ID));
        }

        @Test
        @DisplayName("should throw IllegalStateException if account has transactions")
        void shouldThrowOnExistingTransactions() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.countByAccountId(ACCOUNT_ID)).thenReturn(5L);

            // Act & Assert
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> accountService.deleteAccount(USER_ID, ACCOUNT_ID));
            assertTrue(ex.getMessage().contains("5 transaction(s)"));
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
