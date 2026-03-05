package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Gold Standard examples for Service layer unit testing.
 * Demonstrates isolation via Mockito, @Nested organization, and exhaustive branch testing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Service Layer Gold Standard Tests")
class ServiceGoldStandardTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @InjectMocks private TransactionService transactionService;

    @Nested
    @DisplayName("Method: createTransaction")
    class CreateTransactionTests {

        @Test
        @DisplayName("should create transaction when inputs are valid and owned")
        void shouldCreateSuccessfully() {
            // Arrange
            Long userId = 1L;
            Account account = Account.builder().id(10L).userId(userId).build();
            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
            
            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .accountId(10L).amount(BigDecimal.TEN).type("INCOME").build();

            // Act
            transactionService.createTransaction(userId, request);

            // Assert
            verify(transactionRepository).insert(any(Transaction.class));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user doesn't own the account")
        void shouldFailOwnershipCheck() {
            // Arrange
            Long userId = 1L;
            Account otherAccount = Account.builder().id(10L).userId(999L).build();
            when(accountRepository.findById(10L)).thenReturn(Optional.of(otherAccount));
            TransactionCreateRequest request = TransactionCreateRequest.builder().accountId(10L).build();

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> transactionService.createTransaction(userId, request));
        }
    }
}
