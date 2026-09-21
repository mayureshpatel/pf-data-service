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
    @Mock
    private MerchantService merchantService;

    @InjectMocks
    private TransactionService transactionService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        lenient().when(accountRepository.updateBalance(anyLong(), anyLong(), any(), anyLong())).thenReturn(1);
    }

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long NEW_ACCOUNT_ID = 11L;
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
            // arrange
            when(transactionRepository.findRecentNonTransferTransactions(eq(USER_ID), any())).thenReturn(List.of());
            when(transferMatcher.findMatches(anyList())).thenReturn(List.of(new TransferSuggestionDto(null, null, 0.9)));

            // act
            List<TransferSuggestionDto> result = transactionService.findPotentialTransfers(USER_ID);

            // assert & verify
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
            // arrange
            Account account = createMockAccount(USER_ID);
            Transaction t1 = Transaction.builder().id(1L).type(TransactionType.INCOME).amount(BigDecimal.TEN).account(account).build();
            when(transactionRepository.findAllById(eq(USER_ID), anyList())).thenReturn(List.of(t1));

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            // act
            transactionService.markAsTransfer(USER_ID, List.of(1L));

            // assert & verify
            verify(transactionRepository).updateAll(eq(USER_ID), argThat(list -> list.get(0).getType() == TransactionType.TRANSFER_IN));
            verify(accountRepository).updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong());
        }

        @Test
        @DisplayName("should convert EXPENSE to TRANSFER_OUT")
        void shouldMarkExpenseCorrectly() {
            // arrange
            Account account = createMockAccount(USER_ID);
            Transaction t = Transaction.builder().id(1L).type(TransactionType.EXPENSE).amount(BigDecimal.ONE).account(account).build();
            when(transactionRepository.findAllById(eq(USER_ID), anyList())).thenReturn(List.of(t));

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            // act
            transactionService.markAsTransfer(USER_ID, List.of(1L));

            // assert & verify
            verify(transactionRepository).updateAll(eq(USER_ID), argThat(list -> list.get(0).getType() == TransactionType.TRANSFER_OUT));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user does not own transaction")
        void shouldThrowOnAccessDenied() {
            // arrange
            Account otherAccount = createMockAccount(999L);
            Transaction t = Transaction.builder().id(1L).account(otherAccount).build();
            when(transactionRepository.findAllById(eq(USER_ID), anyList())).thenReturn(List.of(t));

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> transactionService.markAsTransfer(USER_ID, List.of(1L)));
        }
    }

    @Nested
    @DisplayName("backfillTransferTypes (PF-848)")
    class BackfillTransferTypesTests {
        @Test
        @DisplayName("should correct every TRANSFER_IN row on a credit-card account to INCOME")
        void shouldCorrectMisTypedRows() {
            // arrange
            Account account = createMockAccount(USER_ID);
            Transaction misTyped1 = Transaction.builder().id(1L).type(TransactionType.TRANSFER_IN).amount(BigDecimal.TEN).account(account).build();
            Transaction misTyped2 = Transaction.builder().id(2L).type(TransactionType.TRANSFER_IN).amount(BigDecimal.ONE).account(account).build();
            when(transactionRepository.findTransferInOnCreditCardAccounts(USER_ID)).thenReturn(List.of(misTyped1, misTyped2));
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            // act
            int corrected = transactionService.backfillTransferTypes(USER_ID);

            // assert & verify
            assertEquals(2, corrected);
            verify(transactionRepository).updateAll(eq(USER_ID), argThat(list ->
                    list.size() == 2 && list.stream().allMatch(t -> t.getType() == TransactionType.INCOME)));
        }

        @Test
        @DisplayName("should not call updateAll when nothing needs correcting")
        void shouldNoOpWhenNothingToFix() {
            // arrange
            when(transactionRepository.findTransferInOnCreditCardAccounts(USER_ID)).thenReturn(List.of());

            // act
            int corrected = transactionService.backfillTransferTypes(USER_ID);

            // assert & verify
            assertEquals(0, corrected);
            verify(transactionRepository, never()).updateAll(any(), any());
        }
    }

    @Nested
    @DisplayName("getTransactions")
    class GetTransactionsTests {
        @Test
        @DisplayName("should return paginated DTOs")
        void shouldReturnPage() {
            // arrange
            Page<Transaction> page = new PageImpl<>(List.of(Transaction.builder().id(1L).build()));
            when(transactionRepository.findAll(any(), any(Pageable.class))).thenReturn(page);

            // act
            Page<TransactionDto> result = transactionService.getTransactions(USER_ID, TransactionType.INCOME, PageRequest.of(0, 10));

            // assert & verify
            assertEquals(1, result.getContent().size());
            assertEquals(1L, result.getContent().get(0).id());
        }

        @Test
        @DisplayName("should return paginated DTOs with full filter")
        void shouldReturnPageWithFullFilter() {
            // arrange
            Page<Transaction> page = new PageImpl<>(List.of());
            when(transactionRepository.findAll(any(), any(Pageable.class))).thenReturn(page);
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    10L, TransactionType.EXPENSE, "Desc", "Cat", "Vendor", BigDecimal.ONE, BigDecimal.TEN, LocalDate.now(), LocalDate.now(), null
            );

            // act
            Page<TransactionDto> result = transactionService.getTransactions(USER_ID, filter, PageRequest.of(0, 10));

            // assert & verify
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
            // arrange
            Account account = createMockAccount(USER_ID);
            Transaction t = Transaction.builder().id(1L).account(account).amount(BigDecimal.TEN).type(TransactionType.INCOME).build();
            when(transactionRepository.findAllById(eq(USER_ID), anyList())).thenReturn(List.of(t));

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            // act
            transactionService.deleteTransactions(USER_ID, List.of(1L));

            // assert & verify
            verify(transactionRepository).deleteAll(eq(USER_ID), anyList());
            verify(accountRepository).updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if any transaction is not owned")
        void shouldThrowOnMismatchedOwner() {
            // arrange
            Account account = createMockAccount(USER_ID);
            Account other = createMockAccount(999L);
            Transaction t1 = Transaction.builder().id(1L).account(account).build();
            Transaction t2 = Transaction.builder().id(2L).account(other).build();
            when(transactionRepository.findAllById(eq(USER_ID), anyList())).thenReturn(List.of(t1, t2));

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> transactionService.deleteTransactions(USER_ID, List.of(1L, 2L)));
        }
    }

    @Nested
    @DisplayName("createTransaction")
    class CreateTransactionTests {
        @Test
        @DisplayName("should create and apply transaction to account balance")
        void shouldCreate() {
            // arrange
            Account account = createMockAccount(USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .accountId(ACCOUNT_ID)
                    .amount(BigDecimal.TEN)
                    .type("INCOME")
                    .transactionDate(OffsetDateTime.now())
                    .description("Test Description")
                    .categoryId(5L)
                    .build();

            Category subCategory = Category.builder().id(5L).parentId(1L).userId(USER_ID).build();
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(subCategory));
            when(transactionRepository.insert(any(Transaction.class))).thenReturn(1);

            // act
            int result = transactionService.createTransaction(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
            verify(accountRepository).updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong());
            verify(transactionRepository).insert(any(Transaction.class));
        }

        @Test
        @DisplayName("should throw OptimisticLockingFailureException if account balance update fails due to concurrency")
        void shouldThrowOnOptimisticLockingFailure() {
            // arrange
            Account account = createMockAccount(USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(accountRepository.updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong())).thenReturn(0);

            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .accountId(ACCOUNT_ID)
                    .amount(BigDecimal.TEN)
                    .type("INCOME")
                    .description("Test")
                    .build();

            // act & assert & verify
            assertThrows(org.springframework.dao.OptimisticLockingFailureException.class, () -> transactionService.createTransaction(USER_ID, request));
            verify(transactionRepository, never()).insert(any(Transaction.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if account not found")
        void shouldThrowOnAccountNotFound() {
            // arrange
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            TransactionCreateRequest request = TransactionCreateRequest.builder().accountId(ACCOUNT_ID).build();

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> transactionService.createTransaction(USER_ID, request));
        }

        @Test
        @DisplayName("should guess category if none provided")
        void shouldGuessCategory() {
            // arrange
            Account account = createMockAccount(USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            Category cat = Category.builder().id(10L).build();
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(cat));
            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(10L);
            when(transactionRepository.insert(any(Transaction.class))).thenReturn(1);

            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .accountId(ACCOUNT_ID).type("INCOME").description("Guess Me").build();

            // act
            transactionService.createTransaction(USER_ID, request);

            // assert & verify
            verify(transactionRepository).insert((Transaction) argThat(t -> ((Transaction) t).getCategory().getId().equals(10L)));
        }

        @Test
        @DisplayName("bug regression: should use the explicitly provided merchantId instead of "
                + "always auto-deriving from the description (PF-395) -- the frontend's merchant "
                + "picker already resolves a real Merchant before submitting, so silently "
                + "overriding it made every merchant selection in the create form a no-op")
        void shouldUseExplicitMerchantIdWhenProvided() {
            // arrange
            Account account = createMockAccount(USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.insert(any(Transaction.class))).thenReturn(1);

            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .accountId(ACCOUNT_ID)
                    .amount(BigDecimal.TEN)
                    .type("INCOME")
                    .transactionDate(OffsetDateTime.now())
                    .description("Costco Wholesale #123")
                    .merchantId(9999L)
                    .build();

            // act
            transactionService.createTransaction(USER_ID, request);

            // assert & verify
            verify(transactionRepository).insert((Transaction) argThat(t -> ((Transaction) t).getMerchant().getId().equals(9999L)));
        }

        @Test
        @DisplayName("PF-845: should auto-capture a description->merchant link when an explicit "
                + "merchantId and a non-blank description are both present")
        void shouldRecordDescriptionLinkWhenMerchantAssigned() {
            // arrange
            Account account = createMockAccount(USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.insert(any(Transaction.class))).thenReturn(1);

            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .accountId(ACCOUNT_ID)
                    .amount(BigDecimal.TEN)
                    .type("INCOME")
                    .transactionDate(OffsetDateTime.now())
                    .description("Costco Wholesale #123")
                    .merchantId(9999L)
                    .build();

            // act
            transactionService.createTransaction(USER_ID, request);

            // assert & verify
            verify(merchantService).recordDescriptionLink(USER_ID, 9999L, "Costco Wholesale #123");
        }

        @Test
        @DisplayName("PF-845: should never call recordDescriptionLink when no merchant is assigned")
        void shouldNotRecordDescriptionLinkWhenNoMerchant() {
            // arrange
            Account account = createMockAccount(USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.insert(any(Transaction.class))).thenReturn(1);

            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .accountId(ACCOUNT_ID)
                    .amount(BigDecimal.TEN)
                    .type("INCOME")
                    .transactionDate(OffsetDateTime.now())
                    .description("No merchant assigned")
                    .build();

            // act
            transactionService.createTransaction(USER_ID, request);

            // assert & verify
            verify(transactionRepository).insert((Transaction) argThat(t -> ((Transaction) t).getMerchant() == null));
            verify(merchantService, never()).recordDescriptionLink(anyLong(), anyLong(), anyString());
        }

        @Test
        @DisplayName("should handle category guessing when ID is null or zero")
        void shouldHandleGuessedCategoryNullOrZero() {
            // arrange
            Account account = createMockAccount(USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());

            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(null);
            when(transactionRepository.insert(any(Transaction.class))).thenReturn(1);

            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .accountId(ACCOUNT_ID).type("INCOME").description("No Category").build();

            // act
            transactionService.createTransaction(USER_ID, request);

            // assert & verify
            verify(transactionRepository).insert((Transaction) argThat(t -> ((Transaction) t).getCategory() == null));
        }
    }

    @Nested
    @DisplayName("updateTransaction")
    class UpdateTransactionTests {
        @Test
        @DisplayName("should update transaction and update account balance")
        void shouldUpdate() {
            // arrange
            Account account = createMockAccount(USER_ID);
            Transaction original = Transaction.builder().id(TRANSACTION_ID).account(account).amount(BigDecimal.ONE).type(TransactionType.EXPENSE).build();
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.of(original));

            TransactionUpdateRequest request = TransactionUpdateRequest.builder()
                    .id(TRANSACTION_ID)
                    .accountId(ACCOUNT_ID)
                    .amount(BigDecimal.TEN)
                    .type("INCOME")
                    .description("Updated Description")
                    .build();

            when(transactionRepository.update(eq(USER_ID), any(Transaction.class))).thenReturn(1);

            // act
            int result = transactionService.updateTransaction(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
            verify(accountRepository).updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong());
            verify(transactionRepository).update(eq(USER_ID), (Transaction) argThat(t -> ((Transaction) t).getAmount().equals(BigDecimal.TEN)));
        }

        @Test
        @DisplayName("should move a transaction to a different account and update both balances (PF-194)")
        void shouldUpdateAccountWhenChanged() {
            // arrange -- a $10 EXPENSE moving from the old account to a new one
            Account oldAccount = createMockAccount(USER_ID); // id=ACCOUNT_ID, balance=1000.00
            Transaction original = Transaction.builder().id(TRANSACTION_ID).account(oldAccount).amount(BigDecimal.TEN).type(TransactionType.EXPENSE).build();
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.of(original));

            Account newAccount = Account.builder().id(NEW_ACCOUNT_ID).userId(USER_ID).currentBalance(new BigDecimal("500.00")).version(1L).build();
            when(accountRepository.findById(NEW_ACCOUNT_ID)).thenReturn(Optional.of(newAccount));

            TransactionUpdateRequest request = TransactionUpdateRequest.builder()
                    .id(TRANSACTION_ID)
                    .accountId(NEW_ACCOUNT_ID)
                    .amount(BigDecimal.TEN)
                    .type("EXPENSE")
                    .description("Moved transaction")
                    .build();

            when(transactionRepository.update(eq(USER_ID), any(Transaction.class))).thenReturn(1);

            // act
            int result = transactionService.updateTransaction(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
            // old account loses the transaction's effect: undoing a $10 EXPENSE raises its balance (1000 -> 1010)
            verify(accountRepository).updateBalance(eq(USER_ID), eq(ACCOUNT_ID), eq(new BigDecimal("1010.00")), anyLong());
            // new account gains the transaction's effect: applying a $10 EXPENSE lowers its balance (500 -> 490)
            verify(accountRepository).updateBalance(eq(USER_ID), eq(NEW_ACCOUNT_ID), eq(new BigDecimal("490.00")), anyLong());
            verify(transactionRepository).update(eq(USER_ID), (Transaction) argThat(t -> ((Transaction) t).getAccount().getId().equals(NEW_ACCOUNT_ID)));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if the target account doesn't exist (PF-194)")
        void shouldThrowOnTargetAccountNotFound() {
            // arrange
            Account oldAccount = createMockAccount(USER_ID);
            Transaction original = Transaction.builder().id(TRANSACTION_ID).account(oldAccount).amount(BigDecimal.TEN).type(TransactionType.EXPENSE).build();
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.of(original));
            when(accountRepository.findById(NEW_ACCOUNT_ID)).thenReturn(Optional.empty());

            TransactionUpdateRequest request = TransactionUpdateRequest.builder()
                    .id(TRANSACTION_ID)
                    .accountId(NEW_ACCOUNT_ID)
                    .amount(BigDecimal.TEN)
                    .type("EXPENSE")
                    .description("Moved transaction")
                    .build();

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> transactionService.updateTransaction(USER_ID, request));
            verify(transactionRepository, never()).update(anyLong(), any(Transaction.class));
        }

        @Test
        @DisplayName("bug regression: should use the explicitly provided merchantId instead of "
                + "always auto-deriving from the description (PF-395) -- confirmed live via the "
                + "new bulk-edit dialog's Reassign Merchant field: picking a real, different "
                + "merchant had zero effect, since this path always silently re-derived one from "
                + "the (unchanged) description instead of honoring the request's own merchantId")
        void shouldUseExplicitMerchantIdWhenProvided() {
            // arrange
            Account account = createMockAccount(USER_ID);
            Transaction original = Transaction.builder().id(TRANSACTION_ID).account(account).amount(BigDecimal.ONE).type(TransactionType.EXPENSE).build();
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.of(original));
            when(transactionRepository.update(eq(USER_ID), any(Transaction.class))).thenReturn(1);

            TransactionUpdateRequest request = TransactionUpdateRequest.builder()
                    .id(TRANSACTION_ID)
                    .accountId(ACCOUNT_ID)
                    .amount(BigDecimal.TEN)
                    .type("INCOME")
                    .description("Costco Wholesale #123")
                    .merchantId(9999L)
                    .build();

            // act
            transactionService.updateTransaction(USER_ID, request);

            // assert & verify
            verify(transactionRepository).update(eq(USER_ID), (Transaction) argThat(t -> ((Transaction) t).getMerchant().getId().equals(9999L)));
            verify(merchantService).recordDescriptionLink(USER_ID, 9999L, "Costco Wholesale #123");
        }

        @Test
        @DisplayName("should throw AccessDeniedException if the target account belongs to a different user (PF-194)")
        void shouldThrowOnTargetAccountNotOwned() {
            // arrange
            Account oldAccount = createMockAccount(USER_ID);
            Transaction original = Transaction.builder().id(TRANSACTION_ID).account(oldAccount).amount(BigDecimal.TEN).type(TransactionType.EXPENSE).build();
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.of(original));

            Account othersAccount = Account.builder().id(NEW_ACCOUNT_ID).userId(999L).currentBalance(BigDecimal.ZERO).version(1L).build();
            when(accountRepository.findById(NEW_ACCOUNT_ID)).thenReturn(Optional.of(othersAccount));

            TransactionUpdateRequest request = TransactionUpdateRequest.builder()
                    .id(TRANSACTION_ID)
                    .accountId(NEW_ACCOUNT_ID)
                    .amount(BigDecimal.TEN)
                    .type("EXPENSE")
                    .description("Moved transaction")
                    .build();

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> transactionService.updateTransaction(USER_ID, request));
            verify(transactionRepository, never()).update(anyLong(), any(Transaction.class));
        }
    }

    @Nested
    @DisplayName("deleteTransaction")
    class DeleteTransactionTests {
        @Test
        @DisplayName("should delete and reverse transaction from account balance")
        void shouldDelete() {
            // arrange
            Account account = createMockAccount(USER_ID);
            Transaction t = Transaction.builder().id(TRANSACTION_ID).account(account).amount(BigDecimal.TEN).type(TransactionType.INCOME).build();
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.of(t));

            // act
            transactionService.deleteTransaction(USER_ID, TRANSACTION_ID);

            // assert & verify
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
