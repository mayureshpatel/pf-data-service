package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.AccountDto;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private User user;
    private Account account;
    private AccountDto accountDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        account = new Account();
        account.setId(100L);
        account.setName("Test Checking");
        account.setType("CHECKING");
        account.setCurrentBalance(BigDecimal.valueOf(1000.00));
        account.setUser(user);

        accountDto = new AccountDto(
            null,
            "New Account",
            "SAVINGS",
            BigDecimal.valueOf(500.00),
            null
        );
    }

    @Test
    void getAccountsByUserId_ShouldReturnAccountList() {
        // Given
        when(accountRepository.findByUserId(1L)).thenReturn(List.of(account));

        // When
        List<AccountDto> accounts = accountService.getAccountsByUserId(1L);

        // Then
        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).name()).isEqualTo("Test Checking");
        assertThat(accounts.get(0).type()).isEqualTo("CHECKING");
        verify(accountRepository).findByUserId(1L);
    }

    @Test
    void createAccount_ShouldCreateAndReturnAccount() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        // When
        AccountDto created = accountService.createAccount(1L, accountDto);

        // Then
        assertThat(created.id()).isEqualTo(101L);
        assertThat(created.name()).isEqualTo("New Account");
        assertThat(created.type()).isEqualTo("SAVINGS");
        assertThat(created.currentBalance()).isEqualByComparingTo(BigDecimal.valueOf(500.00));
        verify(userRepository).findById(1L);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccount_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.createAccount(1L, accountDto))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("User not found");
    }

    @Test
    void updateAccount_ShouldUpdateAndReturnAccount() {
        // Given
        AccountDto updateDto = new AccountDto(
            100L,
            "Updated Checking",
            "CHECKING",
            BigDecimal.valueOf(1500.00),
            null
        );
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        // When
        AccountDto updated = accountService.updateAccount(1L, 100L, updateDto);

        // Then
        assertThat(updated.name()).isEqualTo("Updated Checking");
        assertThat(updated.currentBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500.00));
        verify(accountRepository).findById(100L);
        verify(accountRepository).save(account);
    }

    @Test
    void updateAccount_AccountNotFound_ShouldThrowException() {
        // Given
        when(accountRepository.findById(100L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.updateAccount(1L, 100L, accountDto))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("Account not found");
    }

    @Test
    void updateAccount_WrongUser_ShouldThrowException() {
        // Given
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));

        // When & Then
        assertThatThrownBy(() -> accountService.updateAccount(999L, 100L, accountDto))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Access denied");
    }

    @Test
    void deleteAccount_WithoutTransactions_ShouldSucceed() {
        // Given
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(transactionRepository.countByAccountId(100L)).thenReturn(0L);

        // When
        accountService.deleteAccount(1L, 100L);

        // Then
        verify(accountRepository).findById(100L);
        verify(transactionRepository).countByAccountId(100L);
        verify(accountRepository).delete(account);
    }

    @Test
    void deleteAccount_WithTransactions_ShouldThrowException() {
        // Given
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(transactionRepository.countByAccountId(100L)).thenReturn(5L);

        // When & Then
        assertThatThrownBy(() -> accountService.deleteAccount(1L, 100L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot delete account with existing transactions")
            .hasMessageContaining("5 transaction(s)");

        verify(accountRepository).findById(100L);
        verify(transactionRepository).countByAccountId(100L);
        verify(accountRepository, never()).delete(any(Account.class));
    }

    @Test
    void deleteAccount_AccountNotFound_ShouldThrowException() {
        // Given
        when(accountRepository.findById(100L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.deleteAccount(1L, 100L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("Account not found");
    }

    @Test
    void deleteAccount_WrongUser_ShouldThrowException() {
        // Given
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));

        // When & Then
        assertThatThrownBy(() -> accountService.deleteAccount(999L, 100L))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Access denied");

        verify(accountRepository, never()).delete(any(Account.class));
    }
}
