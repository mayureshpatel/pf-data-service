package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.CategoryTotal;
import com.mayureshpatel.pfdataservice.dto.DashboardData;
import com.mayureshpatel.pfdataservice.dto.TransactionDto;
import com.mayureshpatel.pfdataservice.model.Account;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.model.User;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void getDashboardData_ShouldCalculateNetSavingsCorrectly() {
        // Arrange
        Long userId = 1L;
        int month = 10;
        int year = 2023;
        LocalDate startDate = LocalDate.of(2023, 10, 1);
        LocalDate endDate = LocalDate.of(2023, 10, 31);

        when(transactionRepository.getSumByDateRange(eq(userId), eq(startDate), eq(endDate), eq(TransactionType.INCOME)))
                .thenReturn(new BigDecimal("5000.00"));

        when(transactionRepository.getSumByDateRange(eq(userId), eq(startDate), eq(endDate), eq(TransactionType.EXPENSE)))
                .thenReturn(new BigDecimal("3000.50"));

        List<CategoryTotal> breakdown = List.of(new CategoryTotal("Groceries", new BigDecimal("500")));
        when(transactionRepository.findCategoryTotals(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(breakdown);

        // Act
        DashboardData result = transactionService.getDashboardData(userId, month, year);

        // Assert
        assertThat(result.getTotalIncome()).isEqualByComparingTo("5000.00");
        assertThat(result.getTotalExpense()).isEqualByComparingTo("3000.50");
        assertThat(result.getNetSavings()).isEqualByComparingTo("1999.50");
        assertThat(result.getCategoryBreakdown()).hasSize(1);
    }

    @Test
    void getDashboardData_ShouldHandleNullSumsAsZero() {
        // Arrange
        Long userId = 1L;
        LocalDate startDate = LocalDate.of(2023, 10, 1);
        LocalDate endDate = LocalDate.of(2023, 10, 31);

        when(transactionRepository.getSumByDateRange(eq(userId), eq(startDate), eq(endDate), any()))
                .thenReturn(null);

        when(transactionRepository.findCategoryTotals(any(), any(), any()))
                .thenReturn(List.of());

        // Act
        DashboardData result = transactionService.getDashboardData(userId, 10, 2023);

        // Assert
        assertThat(result.getTotalIncome()).isEqualByComparingTo("0");
        assertThat(result.getTotalExpense()).isEqualByComparingTo("0");
        assertThat(result.getNetSavings()).isEqualByComparingTo("0");
    }

    @Test
    void updateTransaction_ShouldThrowAccessDenied_IfUserNotOwner() {
        Long userId = 1L;
        Long transactionId = 100L;
        
        User owner = new User();
        owner.setId(99L); // Different user
        Account account = new Account();
        account.setUser(owner);
        
        Transaction t = new Transaction();
        t.setAccount(account);
        
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(t));
        
        TransactionDto dto = new TransactionDto();
        
        assertThatThrownBy(() -> transactionService.updateTransaction(userId, transactionId, dto))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteTransaction_ShouldRevertBalance() {
        Long userId = 1L;
        Long transactionId = 100L;

        User owner = new User();
        owner.setId(userId);
        Account account = new Account();
        account.setUser(owner);
        account.setCurrentBalance(new BigDecimal("1000"));

        Transaction t = new Transaction();
        t.setAccount(account);
        t.setAmount(new BigDecimal("50"));
        t.setType(TransactionType.EXPENSE);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(t));

        transactionService.deleteTransaction(userId, transactionId);

        // Balance should be 1000 + 50 = 1050 (reverting expense)
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("1050");
        verify(transactionRepository).delete(t);
    }
    
    @Test
    void getTransactions_ShouldFilterByType() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        
        Transaction t = new Transaction();
        t.setAmount(BigDecimal.TEN);
        t.setDate(LocalDate.now());
        t.setType(TransactionType.INCOME);
        
        Page<Transaction> page = new PageImpl<>(List.of(t));
        
        when(transactionRepository.findByAccount_User_IdAndType(userId, TransactionType.INCOME, pageable))
                .thenReturn(page);
        
        Page<TransactionDto> result = transactionService.getTransactions(userId, TransactionType.INCOME, pageable);
        
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(TransactionType.INCOME);
    }
}