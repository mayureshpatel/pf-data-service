package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.transaction.RecurringSuggestionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.RecurringTransactionDto;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceTest {

    @Mock
    private RecurringTransactionRepository recurringRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RecurringTransactionService recurringTransactionService;

    private User user;
    private Account account;
    private RecurringTransaction recurring;
    private RecurringTransactionDto recurringDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        account = new Account();
        account.setId(10L);
        account.setName("Checking");
        account.setUser(user);

        recurring = RecurringTransaction.builder()
                .id(100L)
                .user(user)
                .account(account)
                .merchantName("Netflix")
                .amount(new BigDecimal("15.99"))
                .frequency(Frequency.MONTHLY)
                .lastDate(LocalDate.now().minusMonths(1))
                .nextDate(LocalDate.now())
                .active(true)
                .build();

        recurringDto = RecurringTransactionDto.builder()
                .accountId(10L)
                .merchantName("Netflix")
                .amount(new BigDecimal("15.99"))
                .frequency(Frequency.MONTHLY)
                .lastDate(LocalDate.now().minusMonths(1))
                .nextDate(LocalDate.now())
                .active(true)
                .build();
    }

    @Test
    void getRecurringTransactions_ShouldReturnList() {
        // Given
        when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(1L)).thenReturn(List.of(recurring));

        // When
        List<RecurringTransactionDto> result = recurringTransactionService.getRecurringTransactions(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).merchantName()).isEqualTo("Netflix");
        verify(recurringRepository).findByUserIdAndActiveTrueOrderByNextDate(1L);
    }

    @Test
    void createRecurringTransaction_ShouldSaveAndReturnDto() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(recurringRepository.save(any(RecurringTransaction.class))).thenReturn(recurring);

        // When
        RecurringTransactionDto result = recurringTransactionService.createRecurringTransaction(1L, recurringDto);

        // Then
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.merchantName()).isEqualTo("Netflix");
        verify(userRepository).findById(1L);
        verify(accountRepository).findById(10L);
        verify(recurringRepository).save(any(RecurringTransaction.class));
    }

    @Test
    void updateRecurringTransaction_ShouldUpdateAndReturnDto() {
        // Given
        when(recurringRepository.findById(100L)).thenReturn(Optional.of(recurring));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(recurringRepository.save(any(RecurringTransaction.class))).thenReturn(recurring);

        // When
        RecurringTransactionDto result = recurringTransactionService.updateRecurringTransaction(1L, 100L, recurringDto);

        // Then
        assertThat(result.id()).isEqualTo(100L);
        verify(recurringRepository).findById(100L);
        verify(recurringRepository).save(recurring);
    }

    @Test
    void deleteRecurringTransaction_ShouldSoftDelete() {
        // Given
        when(recurringRepository.findById(100L)).thenReturn(Optional.of(recurring));

        // When
        recurringTransactionService.deleteRecurringTransaction(1L, 100L);

        // Then
        assertThat(recurring.isActive()).isFalse();
        assertThat(recurring.getDeletedAt()).isNotNull();
        verify(recurringRepository).save(recurring);
    }

    @Test
    void deleteRecurringTransaction_WrongUser_ShouldThrowException() {
        // Given
        when(recurringRepository.findById(100L)).thenReturn(Optional.of(recurring));

        // When & Then
        assertThatThrownBy(() -> recurringTransactionService.deleteRecurringTransaction(99L, 100L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findSuggestions_ShouldReturnListOfSuggestions() {
        // Given
        LocalDate now = LocalDate.now();
        Transaction t1 = new Transaction();
        t1.setVendorName("Spotify");
        t1.setAmount(new BigDecimal("9.99"));
        t1.setDate(now.minusMonths(3));

        Transaction t2 = new Transaction();
        t2.setVendorName("Spotify");
        t2.setAmount(new BigDecimal("9.99"));
        t2.setDate(now.minusMonths(2));

        Transaction t3 = new Transaction();
        t3.setVendorName("Spotify");
        t3.setAmount(new BigDecimal("9.99"));
        t3.setDate(now.minusMonths(1));

        when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(1L)).thenReturn(Collections.emptyList());
        when(transactionRepository.findExpensesSince(eq(1L), any(LocalDate.class))).thenReturn(List.of(t1, t2, t3));

        // When
        List<RecurringSuggestionDto> suggestions = recurringTransactionService.findSuggestions(1L);

        // Then
        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).merchantName()).isEqualTo("Spotify");
        assertThat(suggestions.get(0).frequency()).isEqualTo(Frequency.MONTHLY);
    }
}
