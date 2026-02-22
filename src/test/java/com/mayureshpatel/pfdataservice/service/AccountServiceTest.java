package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService unit tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long ACCOUNT_ID = 10L;

    private User buildUser(Long id) {
        User user = new User();

        user.setId(id);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        return user;
    }

    private AccountType buildAccountType(String code) {
        AccountType type = new AccountType();

        type.setCode(code);
        type.setLabel(code);

        return type;
    }

    private Account buildAccount(Long accountId, Long userId) {
        Account account = new Account();

        account.setId(accountId);
        account.setUser(buildUser(userId));
        account.setName("Checking Account");
        account.setType(buildAccountType("CHECKING"));
        account.setCurrentBalance(new BigDecimal("1000.00"));
        account.setAudit(new TableAudit());

        return account;
    }

    private AccountDto buildAccountDto(String name, String typeCode, BigDecimal balance, String bankName) {
        AccountType type = buildAccountType(typeCode);

        return new AccountDto(null, null, name, type, balance, null, null);
    }

    @Nested
    class GetAllAccountsByUserIdTest {

        @Test
        @DisplayName("should return mapped DTOs for all accounts belonging to the user")
        void getAllAccountsByUserId_happyPath_returnsMappedDtos() {
            // arrange
            Account account1 = buildAccount(10L, USER_ID);
            account1.setName("Checking");
            Account account2 = buildAccount(11L, USER_ID);
            account2.setName("Savings");

            when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account1, account2));

            // act
            List<AccountDto> result = accountService.getAllAccountsByUserId(USER_ID);

            // assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(AccountDto::name)
                    .containsExactly("Checking", "Savings");

            verify(accountRepository).findByUserId(USER_ID);
        }

        @Test
        @DisplayName("should return empty list when no accounts exist for the user")
        void getAllAccountsByUserId_noAccounts_returnsEmptyList() {
            // arrange
            when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of());

            // act
            List<AccountDto> result = accountService.getAllAccountsByUserId(USER_ID);

            // assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class CreateAccountTest {

        @Test
        @DisplayName("should save new account and return DTO when user exists")
        void createAccount_userExists_savesAndReturnsMappedDto() {
            // arrange
            User user = buildUser(USER_ID);
            AccountDto dto = buildAccountDto("My Checking", "CHECKING", new BigDecimal("500.00"), "Chase");

            Account savedAccount = buildAccount(ACCOUNT_ID, USER_ID);
            savedAccount.setName("My Checking");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

            // act
            AccountDto result = accountService.createAccount(USER_ID, dto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(ACCOUNT_ID);
            assertThat(result.name()).isEqualTo("My Checking");

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            Account captured = captor.getValue();
            assertThat(captured.getName()).isEqualTo(dto.name());
            assertThat(captured.getUser().getId()).isEqualTo(USER_ID);
            assertThat(captured.getCurrentBalance()).isEqualByComparingTo(dto.currentBalance());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void createAccount_userNotFound_throwsResourceNotFoundException() {
            // arrange
            AccountDto dto = buildAccountDto("My Checking", "CHECKING", new BigDecimal("500.00"), "Chase");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> accountService.createAccount(USER_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateAccountTest {

        @Test
        @DisplayName("should update name, type, and bank name when account found for user")
        void updateAccount_accountFound_updatesFieldsAndReturnsDto() {
            // arrange
            Account existing = buildAccount(ACCOUNT_ID, USER_ID);
            existing.setName("Old Name");

            AccountDto updateDto = buildAccountDto("New Name", "SAVINGS", new BigDecimal("999.00"), "BankOfAmerica");
            Account savedAccount = buildAccount(ACCOUNT_ID, USER_ID);
            savedAccount.setName("New Name");

            when(accountRepository.findByAccountIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(existing));
            when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

            // act
            AccountDto result = accountService.updateAccount(USER_ID, ACCOUNT_ID, updateDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("New Name");

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when account not found")
        void updateAccount_accountNotFound_throwsResourceNotFoundException() {
            // arrange
            AccountDto updateDto = buildAccountDto("New Name", "SAVINGS", new BigDecimal("999.00"), "BankOfAmerica");
            when(accountRepository.findByAccountIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> accountService.updateAccount(USER_ID, ACCOUNT_ID, updateDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found");

            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    class ReconcileAccountTest {

        @Test
        @DisplayName("should create adjustment transaction and update balance when positive diff")
        void reconcileAccount_positiveDiff_createsAdjustmentAndUpdatesBalance() {
            // arrange
            BigDecimal currentBalance = new BigDecimal("800.00");
            BigDecimal targetBalance = new BigDecimal("1000.00");
            BigDecimal expectedDiff = new BigDecimal("200.00");

            Account account = buildAccount(ACCOUNT_ID, USER_ID);
            account.setCurrentBalance(currentBalance);

            Account savedAccount = buildAccount(ACCOUNT_ID, USER_ID);
            savedAccount.setCurrentBalance(targetBalance);

            // AccountService.createAdjustmentTransaction() calls new Transaction() then immediately
            // calls adjustment.getMerchant().setOriginalName(...). Because Transaction's no-arg
            // constructor leaves merchant=null, we use mockConstruction to ensure every new
            // Transaction() instance has a pre-initialised Merchant so the call does not NPE.
            try (MockedConstruction<Transaction> txMock = mockConstruction(Transaction.class,
                    (mock, context) -> {
                        Merchant merchant = new Merchant();
                        when(mock.getMerchant()).thenReturn(merchant);
                        when(mock.getNetChange()).thenReturn(expectedDiff);
                    })) {

                when(accountRepository.findByAccountIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
                    Transaction tx = inv.getArgument(0);
                    tx.setId(99L);
                    return tx;
                });
                when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

                // act
                AccountDto result = accountService.reconcileAccount(USER_ID, ACCOUNT_ID, targetBalance);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.currentBalance()).isEqualByComparingTo(targetBalance);

                // Verify the transaction passed to save has the correct amount, type and description
                Transaction constructedTx = txMock.constructed().get(0);
                verify(constructedTx).setAmount(expectedDiff);
                verify(constructedTx).setType(TransactionType.ADJUSTMENT);
                verify(constructedTx).setDescription("Balance Reconciliation");
                verify(transactionRepository).save(constructedTx);
            }
        }

        @Test
        @DisplayName("should create adjustment transaction and update balance when negative diff")
        void reconcileAccount_negativeDiff_createsNegativeAdjustmentAndUpdatesBalance() {
            // arrange
            BigDecimal currentBalance = new BigDecimal("1000.00");
            BigDecimal targetBalance = new BigDecimal("700.00");
            BigDecimal expectedDiff = new BigDecimal("-300.00");

            Account account = buildAccount(ACCOUNT_ID, USER_ID);
            account.setCurrentBalance(currentBalance);

            Account savedAccount = buildAccount(ACCOUNT_ID, USER_ID);
            savedAccount.setCurrentBalance(targetBalance);

            try (MockedConstruction<Transaction> txMock = mockConstruction(Transaction.class,
                    (mock, context) -> {
                        Merchant merchant = new Merchant();
                        when(mock.getMerchant()).thenReturn(merchant);
                        when(mock.getNetChange()).thenReturn(expectedDiff);
                    })) {

                when(accountRepository.findByAccountIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
                    Transaction tx = inv.getArgument(0);
                    tx.setId(100L);
                    return tx;
                });
                when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

                // act
                AccountDto result = accountService.reconcileAccount(USER_ID, ACCOUNT_ID, targetBalance);

                // Assert
                assertThat(result.currentBalance()).isEqualByComparingTo(targetBalance);

                Transaction constructedTx = txMock.constructed().get(0);
                verify(constructedTx).setAmount(expectedDiff);
                verify(constructedTx).setType(TransactionType.ADJUSTMENT);
                verify(transactionRepository).save(constructedTx);
            }
        }

        @Test
        @DisplayName("should return current account DTO without saving when diff is zero")
        void reconcileAccount_zeroDiff_returnsCurrentDtoWithoutSavingTransaction() {
            // arrange
            BigDecimal balance = new BigDecimal("1000.00");

            Account account = buildAccount(ACCOUNT_ID, USER_ID);
            account.setCurrentBalance(balance);

            when(accountRepository.findByAccountIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));

            // act
            AccountDto result = accountService.reconcileAccount(USER_ID, ACCOUNT_ID, balance);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.currentBalance()).isEqualByComparingTo(balance);
            verify(transactionRepository, never()).save(any());
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when account not found")
        void reconcileAccount_accountNotFound_throwsResourceNotFoundException() {
            // arrange
            when(accountRepository.findByAccountIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> accountService.reconcileAccount(USER_ID, ACCOUNT_ID, new BigDecimal("500.00")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found");
        }
    }

    @Nested
    class DeleteAccountTest {

        @Test
        @DisplayName("should delete account when owned by user and has no transactions")
        void deleteAccount_happyPath_deletesAccount() {
            // arrange
            Account account = buildAccount(ACCOUNT_ID, USER_ID);

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.countByAccountId(ACCOUNT_ID)).thenReturn(0L);

            // act
            accountService.deleteAccount(USER_ID, ACCOUNT_ID);

            // Assert
            verify(accountRepository).delete(account);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when account not found")
        void deleteAccount_accountNotFound_throwsResourceNotFoundException() {
            // arrange
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> accountService.deleteAccount(USER_ID, ACCOUNT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found");

            verify(accountRepository, never()).delete(any(Account.class));
        }

        @Test
        @DisplayName("should throw AccessDeniedException when account is owned by a different user")
        void deleteAccount_accountOwnedByDifferentUser_throwsAccessDeniedException() {
            // arrange
            Account account = buildAccount(ACCOUNT_ID, OTHER_USER_ID);

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            // act & assert
            assertThatThrownBy(() -> accountService.deleteAccount(USER_ID, ACCOUNT_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied");

            verify(transactionRepository, never()).countByAccountId(any());
            verify(accountRepository, never()).delete(any(Account.class));
        }

        @Test
        @DisplayName("should throw IllegalStateException when account has existing transactions")
        void deleteAccount_accountHasTransactions_throwsIllegalStateException() {
            // arrange
            final long transactionCount = 3L;
            Account account = buildAccount(ACCOUNT_ID, USER_ID);

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.countByAccountId(ACCOUNT_ID)).thenReturn(transactionCount);

            // act & assert
            assertThatThrownBy(() -> accountService.deleteAccount(USER_ID, ACCOUNT_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("3");

            verify(accountRepository, never()).delete(any(Account.class));
        }

        @Test
        @DisplayName("should throw IllegalStateException when account has existing transactions")
        void deleteAccount_exactlyOneTransaction_throwsIllegalStateException() {
            // arrange
            Account account = buildAccount(ACCOUNT_ID, USER_ID);

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.countByAccountId(ACCOUNT_ID)).thenReturn(1L);

            // act & assert
            assertThatThrownBy(() -> accountService.deleteAccount(USER_ID, ACCOUNT_ID))
                    .isInstanceOf(IllegalStateException.class);

            verify(accountRepository, never()).delete(any(Account.class));
        }
    }
}
