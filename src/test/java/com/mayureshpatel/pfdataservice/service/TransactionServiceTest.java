package com.mayureshpatel.pfdataservice.service;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

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
        assertThat(result.getContent().get(0).type()).isEqualTo(TransactionType.INCOME);
    }
}
