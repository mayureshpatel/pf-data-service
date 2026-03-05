package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransferSuggestionDto;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import com.mayureshpatel.pfdataservice.service.transfer.TransferMatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionCategorizer categorizer;
    @Mock
    private CategoryRuleRepository categoryRuleRepository;
    @Mock
    private TransferMatcher transferMatcher;

    @InjectMocks
    private TransactionService transactionService;

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long TRANSACTION_ID = 100L;

    private Account createMockAccount(Long uid) {
        return Account.builder().id(ACCOUNT_ID).userId(uid).currentBalance(new BigDecimal("1000.00")).version(1L).build();
    }

    @Nested
    @DisplayName("findPotentialTransfers")
    class FindPotentialTransfersTests {
        @Test
        @DisplayName("should return transfer matches from matcher")
        void shouldReturnMatches() {
            // Arrange
            when(transactionRepository.findRecentNonTransferTransactions(eq(USER_ID), any())).thenReturn(List.of());
            when(transferMatcher.findMatches(anyList())).thenReturn(List.of(new TransferSuggestionDto(null, null, 0.9)));

            // Act
            List<TransferSuggestionDto> result = transactionService.findPotentialTransfers(USER_ID);

            // Assert
            assertEquals(1, result.size());
            verify(transferMatcher).findMatches(anyList());
        }
    }

    @Nested
    @DisplayName("markAsTransfer")
    class MarkAsTransferTests {
        @Test
        @DisplayName("should convert INCOME to TRANSFER_IN and update account balance")
        void shouldMarkCorrectly() {
            // Arrange
            Account account = createMockAccount(USER_ID);
            Transaction t1 = Transaction.builder().id(1L).type(TransactionType.INCOME).amount(BigDecimal.TEN).account(account).build();
            when(transactionRepository.findAllById(eq(USER_ID), anyList())).thenReturn(List.of(t1));

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            
            // Act
            transactionService.markAsTransfer(USER_ID, List.of(1L));

            // Assert
            verify(transactionRepository).updateAll(eq(USER_ID), argThat(list -> list.get(0).getType() == TransactionType.TRANSFER_IN));
            verify(accountRepository).updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong());
        }

        @Test
        @DisplayName("should convert EXPENSE to TRANSFER_OUT")
        void shouldMarkExpenseCorrectly() {
            // Arrange
            Account account = createMockAccount(USER_ID);
            Transaction t = Transaction.builder().id(1L).type(TransactionType.EXPENSE).amount(BigDecimal.ONE).account(account).build();
            when(transactionRepository.findAllById(eq(USER_ID), anyList())).thenReturn(List.of(t));

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            
            // Act
            transactionService.markAsTransfer(USER_ID, List.of(1L));

            // Assert
            verify(transactionRepository).updateAll(eq(USER_ID), argThat(list -> list.get(0).getType() == TransactionType.TRANSFER_OUT));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user does not own transaction")
        void shouldThrowOnAccessDenied() {
            // Arrange
            Account otherAccount = createMockAccount(999L);
            Transaction t = Transaction.builder().id(1L).account(otherAccount).build();
            when(transactionRepository.findAllById(eq(USER_ID), anyList())).thenReturn(List.of(t));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> transactionService.markAsTransfer(USER_ID, List.of(1L)));
        }
    }

    @Nested
    @DisplayName("getTransactions")
    class GetTransactionsTests {
        @Test
        @DisplayName("should return paginated DTOs")
        void shouldReturnPage() {
            // Arrange
            Page<Transaction> page = new PageImpl<>(List.of(Transaction.builder().id(1L).build()));
            when(transactionRepository.findAll(any(), any(Pageable.class))).thenReturn(page);

            // Act
            Page<TransactionDto> result = transactionService.getTransactions(USER_ID, TransactionType.INCOME, PageRequest.of(0, 10));

            // Assert
            assertEquals(1, result.getContent().size());
            assertEquals(1L, result.getContent().get(0).id());
        }

        @Test
        @DisplayName("should return paginated DTOs with full filter")
        void shouldReturnPageWithFullFilter() {
            // Arrange
            Page<Transaction> page = new PageImpl<>(List.of());
            when(transactionRepository.findAll(any(), any(Pageable.class))).thenReturn(page);
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    10L, TransactionType.EXPENSE, "Desc", "Cat", "Vendor", BigDecimal.ONE, BigDecimal.TEN, LocalDate.now(), LocalDate.now()
            );

            // Act
            Page<TransactionDto> result = transactionService.getTransactions(USER_ID, filter, PageRequest.of(0, 10));

            // Assert
            assertNotNull(result);
            verify(transactionRepository).findAll(any(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("deleteTransactions")
    class DeleteTransactionsTests {
        @Test
        @DisplayName("should return early if IDs list is null or empty")
        void shouldReturnEarly() {
            transactionService.deleteTransactions(USER_ID, null);
            transactionService.deleteTransactions(USER_ID, Collections.emptyList());
            verify(transactionRepository, never()).findAllById(eq(USER_ID), any());
        }

        @Test
        @DisplayName("should delete transactions and update balance if owned")
        void shouldDelete() {
            // Arrange
            Account account = createMockAccount(USER_ID);
            Transaction t = Transaction.builder().id(1L).account(account).amount(BigDecimal.TEN).type(TransactionType.INCOME).build();
            when(transactionRepository.findAllById(eq(USER_ID), anyList())).thenReturn(List.of(t));

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            
            // Act
            transactionService.deleteTransactions(USER_ID, List.of(1L));

            // Assert
            verify(transactionRepository).deleteAll(eq(USER_ID), anyList());
            verify(accountRepository).updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if any transaction is not owned")
        void shouldThrowOnMismatchedOwner() {
            // Arrange
            Account account = createMockAccount(USER_ID);
            Account other = createMockAccount(999L);
            Transaction t1 = Transaction.builder().id(1L).account(account).build();
            Transaction t2 = Transaction.builder().id(2L).account(other).build();
            when(transactionRepository.findAllById(eq(USER_ID), anyList())).thenReturn(List.of(t1, t2));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> transactionService.deleteTransactions(USER_ID, List.of(1L, 2L)));
        }
    }

    @Nested
    @DisplayName("createTransaction")
    class CreateTransactionTests {
        @Test
        @DisplayName("should create and apply transaction to account balance")
        void shouldCreate() {
            // Arrange
            Account account = createMockAccount(USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .accountId(ACCOUNT_ID)
                    .amount(BigDecimal.TEN)
                    .type("INCOME")
                    .transactionDate(OffsetDateTime.now())
                    .categoryId(5L)
                    .build();

            Category subCategory = Category.builder().id(5L).parentId(1L).userId(USER_ID).build();
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(subCategory));
            when(transactionRepository.insert(any(Transaction.class))).thenReturn(1);

            // Act
            int result = transactionService.createTransaction(USER_ID, request);

            // Assert
            assertEquals(1, result);
            verify(accountRepository).updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong());
            verify(transactionRepository).insert(any(Transaction.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if account not found")
        void shouldThrowOnAccountNotFound() {
            // Arrange
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            TransactionCreateRequest request = TransactionCreateRequest.builder().accountId(ACCOUNT_ID).build();

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> transactionService.createTransaction(USER_ID, request));
        }

        @Test
        @DisplayName("should guess category if none provided")
        void shouldGuessCategory() {
            // Arrange
            Account account = createMockAccount(USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            Category cat = Category.builder().id(10L).build();
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(cat));
            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(10L);
            when(transactionRepository.insert(any(Transaction.class))).thenReturn(1);

            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .accountId(ACCOUNT_ID).type("INCOME").build();

            // Act
            transactionService.createTransaction(USER_ID, request);

            // Assert
            verify(transactionRepository).insert((Transaction) argThat(t -> ((Transaction) t).getCategory().getId().equals(10L)));
        }

        @Test
        @DisplayName("should handle category guessing when ID is null or zero")
        void shouldHandleGuessedCategoryNullOrZero() {
            // Arrange
            Account account = createMockAccount(USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());

            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(null);
            when(transactionRepository.insert(any(Transaction.class))).thenReturn(1);

            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .accountId(ACCOUNT_ID).type("INCOME").build();

            // Act
            transactionService.createTransaction(USER_ID, request);

            // Assert
            verify(transactionRepository).insert((Transaction) argThat(t -> ((Transaction) t).getCategory() == null));
        }
    }

    @Nested
    @DisplayName("updateTransaction")
    class UpdateTransactionTests {
        @Test
        @DisplayName("should update transaction and update account balance")
        void shouldUpdate() {
            // Arrange
            Account account = createMockAccount(USER_ID);
            Transaction original = Transaction.builder().id(TRANSACTION_ID).account(account).amount(BigDecimal.ONE).type(TransactionType.EXPENSE).build();
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.of(original));

            TransactionUpdateRequest request = TransactionUpdateRequest.builder()
                    .id(TRANSACTION_ID)
                    .amount(BigDecimal.TEN)
                    .type("INCOME")
                    .build();

            when(transactionRepository.update(eq(USER_ID), any(Transaction.class))).thenReturn(1);

            // Act
            int result = transactionService.updateTransaction(USER_ID, request);

            // Assert
            assertEquals(1, result);
            verify(accountRepository).updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong());
            verify(transactionRepository).update(eq(USER_ID), (Transaction) argThat(t -> ((Transaction) t).getAmount().equals(BigDecimal.TEN)));
        }
    }

    @Nested
    @DisplayName("deleteTransaction")
    class DeleteTransactionTests {
        @Test
        @DisplayName("should delete and reverse transaction from account balance")
        void shouldDelete() {
            // Arrange
            Account account = createMockAccount(USER_ID);
            Transaction t = Transaction.builder().id(TRANSACTION_ID).account(account).amount(BigDecimal.TEN).type(TransactionType.INCOME).build();
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.of(t));

            // Act
            transactionService.deleteTransaction(USER_ID, TRANSACTION_ID);

            // Assert
            verify(accountRepository).updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong());
            verify(transactionRepository).deleteById(TRANSACTION_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("Lookup Methods")
    class LookupTests {
        @Test
        void shouldGetCountByCategory() {
            transactionService.getCountByCategory(USER_ID);
            verify(transactionRepository).getCountByCategory(USER_ID);
        }

        @Test
        void shouldGetCategoriesWithTransactions() {
            when(transactionRepository.getCategoriesWithTransactions(USER_ID)).thenReturn(List.of());
            transactionService.getCategoriesWithTransactions(USER_ID);
            verify(transactionRepository).getCategoriesWithTransactions(USER_ID);
        }

        @Test
        void shouldGetMerchantsWithTransactions() {
            when(transactionRepository.getMerchantsWithTransactions(USER_ID)).thenReturn(List.of());
            transactionService.getMerchantsWithTransactions(USER_ID);
            verify(transactionRepository).getMerchantsWithTransactions(USER_ID);
        }
    }
}
