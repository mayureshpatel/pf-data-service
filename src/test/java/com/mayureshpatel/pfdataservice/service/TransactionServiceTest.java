package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import com.mayureshpatel.pfdataservice.service.categorization.VendorCleaner;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private VendorCleaner vendorCleaner;

    @Mock
    private TransactionCategorizer categorizer;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Account account;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        account = new Account();
        account.setId(1L);
        account.setUser(user);
        account.setCurrentBalance(BigDecimal.valueOf(100));

        transaction = new Transaction();
        transaction.setId(1L);
        transaction.setAccount(account);
        transaction.setAmount(BigDecimal.valueOf(50));
        transaction.setType(TransactionType.EXPENSE);
        transaction.setDate(LocalDate.now());
        transaction.setDescription("Test Transaction");
    }

    @Test
    void getTransactions_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> page = new PageImpl<>(Collections.singletonList(transaction));

        when(transactionRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).thenReturn(page);

        Page<TransactionDto> result = transactionService.getTransactions(1L, (TransactionType) null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(transactionRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    void getTransactions_WithType_ShouldFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> page = new PageImpl<>(Collections.singletonList(transaction));

        when(transactionRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).thenReturn(page);

        Page<TransactionDto> result = transactionService.getTransactions(1L, TransactionType.EXPENSE, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(transactionRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    void updateTransaction_Success() {
        TransactionDto dto = TransactionDto.builder()
                .amount(BigDecimal.valueOf(75))
                .date(LocalDate.now().plusDays(1))
                .description("Updated")
                .type(TransactionType.INCOME)
                .build();

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionDto result = transactionService.updateTransaction(1L, 1L, dto);

        assertThat(result.amount()).isEqualTo(BigDecimal.valueOf(75));
        assertThat(result.description()).isEqualTo("Updated");
        assertThat(result.type()).isEqualTo(TransactionType.INCOME);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void updateTransaction_NotFound_ShouldThrow() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateTransaction(1L, 1L, mock(TransactionDto.class)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateTransaction_WrongUser_ShouldThrow() {
        User otherUser = new User();
        otherUser.setId(2L);
        account.setUser(otherUser); // Transaction now belongs to user 2

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.updateTransaction(1L, 1L, mock(TransactionDto.class)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteTransaction_Success_ShouldUpdateBalance() {
        // Setup: Expense of 50. Balance is 100.
        // If deleted, balance should become 100 + 50 = 150.
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(1L, 1L);

        assertThat(account.getCurrentBalance()).isEqualByComparingTo("150");
        verify(transactionRepository).delete(transaction);
    }

    @Test
    void deleteTransaction_Income_ShouldReduceBalance() {
        transaction.setType(TransactionType.INCOME);
        // Setup: Income of 50. Balance is 100.
        // If deleted, balance should become 100 - 50 = 50.
        
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(1L, 1L);

        assertThat(account.getCurrentBalance()).isEqualByComparingTo("50");
        verify(transactionRepository).delete(transaction);
    }
}