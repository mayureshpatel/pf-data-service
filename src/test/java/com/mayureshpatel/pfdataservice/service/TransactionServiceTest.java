package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.transaction.CategoryTransactionsDto;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
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
import java.util.function.UnaryOperator;

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
    @Mock
    private AccountBalanceUpdateService accountBalanceUpdateService;

    @InjectMocks
    private TransactionService transactionService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // PF-854: mimics the real service's success path (apply the transform once, return the
        // result) so every pre-existing test's balance-dependent assertions keep working without
        // each one needing its own stub -- tests that care about retry mechanics specifically now
        // live in AccountBalanceUpdateServiceTest, the shared helper's own dedicated test class.
        lenient().when(accountBalanceUpdateService.applyWithRetry(any(), any(), any()))
                .thenAnswer(invocation -> {
                    Account account = invocation.getArgument(1);
                    UnaryOperator<Account> transform = invocation.getArgument(2);
                    return transform.apply(account);
                });
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
            verify(accountBalanceUpdateService).applyWithRetry(eq(USER_ID), eq(account), any());
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

        @Test
        @DisplayName("should throw ResourceNotFoundException if the transaction's account no longer "
                + "exists (PF-818) -- representative for this exact defensive shape, also shared "
                + "unchanged by unmarkAsTransfer and backfillTransferTypes; see PF-818 resolution")
        void shouldThrowOnAccountNotFound() {
            // arrange
            Account account = createMockAccount(USER_ID);
            Transaction t = Transaction.builder().id(1L).type(TransactionType.INCOME).amount(BigDecimal.TEN).account(account).build();
            when(transactionRepository.findAllById(eq(USER_ID), anyList())).thenReturn(List.of(t));
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> transactionService.markAsTransfer(USER_ID, List.of(1L)));
            verify(transactionRepository, never()).updateAll(any(), any());
        }

        @Test
        @DisplayName("PF-854 AC: a mid-batch conflict-then-retry on one transaction's account "
                + "update doesn't disturb any other transaction in the same batch -- uses the "
                + "REAL AccountBalanceUpdateService (not the class-level mock) so the retry "
                + "genuinely runs inside markAsTransfer's own loop, not simulated. Representative "
                + "for all 4 multi-transaction loop sites (markAsTransfer, unmarkAsTransfer, "
                + "backfillTransferTypes, deleteTransactions): each iteration re-fetches its own "
                + "account fresh from the repository and carries no state forward from prior "
                + "iterations, so an internal retry on iteration N is invisible to iterations "
                + "before or after it by construction, not by coincidence")
        void shouldRetryMidBatchWithoutDisturbingOtherTransactions() {
            // arrange -- two transactions on two DIFFERENT accounts; the first account's balance
            // update conflicts once then succeeds, the second succeeds immediately
            AccountBalanceUpdateService realAccountBalanceUpdateService = new AccountBalanceUpdateService(accountRepository);
            TransactionService serviceUnderTest = new TransactionService(
                    transactionRepository, accountRepository, categoryRepository, categorizer,
                    categoryRuleRepository, transferMatcher, merchantService, realAccountBalanceUpdateService);

            Account account1 = createMockAccount(USER_ID); // id = ACCOUNT_ID
            Account account2 = Account.builder().id(NEW_ACCOUNT_ID).userId(USER_ID).currentBalance(new BigDecimal("500.00")).version(1L).build();
            Transaction t1 = Transaction.builder().id(1L).type(TransactionType.INCOME).amount(BigDecimal.TEN).account(account1).build();
            Transaction t2 = Transaction.builder().id(2L).type(TransactionType.INCOME).amount(BigDecimal.TEN).account(account2).build();
            when(transactionRepository.findAllById(eq(USER_ID), anyList())).thenReturn(List.of(t1, t2));

            Account account1Refreshed = account1.toBuilder().version(2L).build();
            when(accountRepository.findById(ACCOUNT_ID))
                    .thenReturn(Optional.of(account1))
                    .thenReturn(Optional.of(account1Refreshed));
            when(accountRepository.findById(NEW_ACCOUNT_ID)).thenReturn(Optional.of(account2));

            when(accountRepository.updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong()))
                    .thenThrow(new OptimisticLockingFailureException("conflict"))
                    .thenReturn(1);
            when(accountRepository.updateBalance(eq(USER_ID), eq(NEW_ACCOUNT_ID), any(BigDecimal.class), anyLong()))
                    .thenReturn(1);

            // act
            serviceUnderTest.markAsTransfer(USER_ID, List.of(1L, 2L));

            // assert & verify -- both transactions marked despite account1's mid-batch retry;
            // account2's single, non-conflicting update was never touched by account1's retry
            verify(transactionRepository).updateAll(eq(USER_ID), argThat(list -> list.size() == 2
                    && list.stream().allMatch(t -> t.getType() == TransactionType.TRANSFER_IN)));
            verify(accountRepository, times(2)).updateBalance(eq(USER_ID), eq(ACCOUNT_ID), any(BigDecimal.class), anyLong());
            verify(accountRepository, times(1)).updateBalance(eq(USER_ID), eq(NEW_ACCOUNT_ID), any(BigDecimal.class), anyLong());
            verify(accountRepository, times(2)).findById(ACCOUNT_ID);
            verify(accountRepository, times(1)).findById(NEW_ACCOUNT_ID);
        }
    }

    @Nested
    @DisplayName("unmarkAsTransfer (PF-831)")
    class UnmarkAsTransferTests {
        @Test
        @DisplayName("should convert TRANSFER_IN back to INCOME")
        void shouldUnmarkTransferInCorrectly() {
            // arrange
            Account account = createMockAccount(USER_ID);
            Transaction t = Transaction.builder().id(1L).type(TransactionType.TRANSFER_IN).amount(BigDecimal.TEN).account(account).build();
            when(transactionRepository.findAllById(eq(USER_ID), anyList())).thenReturn(List.of(t));
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            // act
            transactionService.unmarkAsTransfer(USER_ID, List.of(1L));

            // assert & verify
            verify(transactionRepository).updateAll(eq(USER_ID), argThat(list -> list.get(0).getType() == TransactionType.INCOME));
            verify(accountBalanceUpdateService).applyWithRetry(eq(USER_ID), eq(account), any());
        }

        @Test
        @DisplayName("should convert TRANSFER_OUT back to EXPENSE")
        void shouldUnmarkTransferOutCorrectly() {
            // arrange
            Account account = createMockAccount(USER_ID);
            Transaction t = Transaction.builder().id(1L).type(TransactionType.TRANSFER_OUT).amount(BigDecimal.ONE).account(account).build();
            when(transactionRepository.findAllById(eq(USER_ID), anyList())).thenReturn(List.of(t));
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            // act
            transactionService.unmarkAsTransfer(USER_ID, List.of(1L));

            // assert & verify
            verify(transactionRepository).updateAll(eq(USER_ID), argThat(list -> list.get(0).getType() == TransactionType.EXPENSE));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user does not own transaction")
        void shouldThrowOnAccessDenied() {
            // arrange
            Account otherAccount = createMockAccount(999L);
            Transaction t = Transaction.builder().id(1L).account(otherAccount).build();
            when(transactionRepository.findAllById(eq(USER_ID), anyList())).thenReturn(List.of(t));

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> transactionService.unmarkAsTransfer(USER_ID, List.of(1L)));
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
            verify(accountBalanceUpdateService).applyWithRetry(eq(USER_ID), eq(account), any());
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

        @Test
        @DisplayName("should throw ResourceNotFoundException if the transaction's account no longer "
                + "exists (PF-818)")
        void shouldThrowOnAccountNotFound() {
            // arrange
            Account account = createMockAccount(USER_ID);
            Transaction t = Transaction.builder().id(1L).account(account).amount(BigDecimal.TEN).type(TransactionType.INCOME).build();
            when(transactionRepository.findAllById(eq(USER_ID), anyList())).thenReturn(List.of(t));
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> transactionService.deleteTransactions(USER_ID, List.of(1L)));
            verify(transactionRepository, never()).deleteAll(any(), any());
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
            verify(accountBalanceUpdateService).applyWithRetry(eq(USER_ID), eq(account), any());
            verify(transactionRepository).insert(any(Transaction.class));
        }

        @Test
        @DisplayName("should propagate OptimisticLockingFailureException if the balance update "
                + "conflicts (PF-854) -- the retry mechanics themselves (retry-then-succeed, "
                + "bounded give-up, account-gone-on-retry-refetch) moved to "
                + "AccountBalanceUpdateServiceTest along with the retry loop itself; this only "
                + "confirms createTransaction doesn't insert the transaction when the balance "
                + "update it depends on ultimately fails")
        void shouldPropagateOptimisticLockingFailure() {
            // arrange
            Account account = createMockAccount(USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            doThrow(new OptimisticLockingFailureException("conflict"))
                    .when(accountBalanceUpdateService).applyWithRetry(eq(USER_ID), eq(account), any());

            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .accountId(ACCOUNT_ID)
                    .amount(BigDecimal.TEN)
                    .type("INCOME")
                    .description("Test")
                    .build();

            // act & assert & verify
            assertThrows(OptimisticLockingFailureException.class, () -> transactionService.createTransaction(USER_ID, request));
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
        @DisplayName("should throw ResourceNotFoundException if the requested category doesn't "
                + "exist (PF-818)")
        void shouldThrowOnCategoryNotFound() {
            // arrange
            Account account = createMockAccount(USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(categoryRepository.findById(5L)).thenReturn(Optional.empty());

            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .accountId(ACCOUNT_ID).amount(BigDecimal.TEN).type("INCOME").categoryId(5L).build();

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> transactionService.createTransaction(USER_ID, request));
            verify(transactionRepository, never()).insert(any(Transaction.class));
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

        @Test
        @DisplayName("PF-856: should treat a guessed category id of exactly 0 the same as -1 "
                + "(leaves the transaction uncategorized), proving the resolveCategory > 0 "
                + "boundary is correct -- TransactionCategorizer's own contract documents 0 (not "
                + "just its real -1 sentinel) as a value callers must handle, even though the "
                + "current implementation never actually produces it. userCategories deliberately "
                + "includes a (synthetic, unrealistic for a real auto-increment id, but necessary "
                + "to prove the boundary) category whose id is literally 0 -- a >= 0 mutant would "
                + "wrongly enter the branch and find it, a correct > 0 check never does")
        void shouldTreatGuessedCategoryZeroSameAsNoMatch() {
            // arrange
            Account account = createMockAccount(USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            Category zeroIdCategory = Category.builder().id(0L).name("Should Never Match").build();
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(zeroIdCategory));
            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(0L);
            when(transactionRepository.insert(any(Transaction.class))).thenReturn(1);

            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .accountId(ACCOUNT_ID).type("INCOME").description("Zero Category").build();

            // act
            transactionService.createTransaction(USER_ID, request);

            // assert & verify
            verify(transactionRepository).insert((Transaction) argThat(t -> ((Transaction) t).getCategory() == null));
        }

        @Test
        @DisplayName("PF-856: should select the guessed category by matching its id, not just "
                + "return the first candidate regardless -- userCategories deliberately has more "
                + "than one entry, and the guessed id is the SECOND one's, so a mutant that always "
                + "picks the first would fail this")
        void shouldSelectGuessedCategoryByIdAmongMultiple() {
            // arrange
            Account account = createMockAccount(USER_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            Category first = Category.builder().id(10L).name("Groceries").build();
            Category second = Category.builder().id(20L).name("Dining").build();
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(first, second));
            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(20L);
            when(transactionRepository.insert(any(Transaction.class))).thenReturn(1);

            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .accountId(ACCOUNT_ID).type("INCOME").description("Guess Second").build();

            // act
            transactionService.createTransaction(USER_ID, request);

            // assert & verify
            verify(transactionRepository).insert((Transaction) argThat(t -> ((Transaction) t).getCategory().getId().equals(20L)));
        }
    }

    @Nested
    @DisplayName("updateTransactionsBulk (PF-818)")
    class UpdateTransactionsBulkTests {
        @Test
        @DisplayName("should return 0 without error for a null or empty request list")
        void shouldReturnZeroForNullOrEmpty() {
            assertEquals(0, transactionService.updateTransactionsBulk(USER_ID, null));
            assertEquals(0, transactionService.updateTransactionsBulk(USER_ID, Collections.emptyList()));
            verify(transactionRepository, never()).findById(anyLong(), anyLong());
        }

        @Test
        @DisplayName("should sum each request's own result rather than just reflecting a single "
                + "element -- two requests each updating exactly 1 row must sum to 2, a total only "
                + "reachable by genuinely aggregating, not e.g. returning the last result or the "
                + "element count")
        void shouldAggregateMultipleRequests() {
            // arrange -- two independent transactions on the same account, each updated successfully
            Account account = createMockAccount(USER_ID);
            Long secondTransactionId = TRANSACTION_ID + 1;
            Transaction first = Transaction.builder().id(TRANSACTION_ID).account(account).amount(BigDecimal.ONE).type(TransactionType.EXPENSE).build();
            Transaction second = Transaction.builder().id(secondTransactionId).account(account).amount(BigDecimal.ONE).type(TransactionType.EXPENSE).build();
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.of(first));
            when(transactionRepository.findById(secondTransactionId, USER_ID)).thenReturn(Optional.of(second));
            when(transactionRepository.update(eq(USER_ID), any(Transaction.class))).thenReturn(1);

            TransactionUpdateRequest req1 = TransactionUpdateRequest.builder().id(TRANSACTION_ID).accountId(ACCOUNT_ID).amount(BigDecimal.TEN).type("INCOME").build();
            TransactionUpdateRequest req2 = TransactionUpdateRequest.builder().id(secondTransactionId).accountId(ACCOUNT_ID).amount(BigDecimal.TEN).type("INCOME").build();

            // act
            Integer result = transactionService.updateTransactionsBulk(USER_ID, List.of(req1, req2));

            // assert & verify
            assertEquals(2, result);
            verify(transactionRepository, times(2)).update(eq(USER_ID), any(Transaction.class));
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
            verify(accountBalanceUpdateService).applyWithRetry(eq(USER_ID), eq(account), any());
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

            // PF-854: the balance is no longer a direct updateBalance() argument -- capture the two
            // applyWithRetry() transforms (invoked in old-account-then-new-account order, per
            // updateTransaction's own source) and apply each against its real starting account.
            @SuppressWarnings("unchecked")
            ArgumentCaptor<UnaryOperator<Account>> transformCaptor = ArgumentCaptor.forClass(UnaryOperator.class);
            verify(accountBalanceUpdateService, times(2)).applyWithRetry(eq(USER_ID), any(Account.class), transformCaptor.capture());
            List<UnaryOperator<Account>> transforms = transformCaptor.getAllValues();

            // old account loses the transaction's effect: undoing a $10 EXPENSE raises its balance (1000 -> 1010)
            assertEquals(new BigDecimal("1010.00"), transforms.get(0).apply(oldAccount).getCurrentBalance());
            // new account gains the transaction's effect: applying a $10 EXPENSE lowers its balance (500 -> 490)
            assertEquals(new BigDecimal("490.00"), transforms.get(1).apply(newAccount).getCurrentBalance());
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
        @DisplayName("should throw ResourceNotFoundException if the transaction itself doesn't "
                + "exist (PF-818) -- distinct from the already-covered target-account-not-found "
                + "case above: this is the earlier lookup, for the transaction being updated")
        void shouldThrowOnTransactionNotFound() {
            // arrange
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.empty());

            TransactionUpdateRequest request = TransactionUpdateRequest.builder()
                    .id(TRANSACTION_ID).accountId(ACCOUNT_ID).amount(BigDecimal.TEN).type("INCOME").build();

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> transactionService.updateTransaction(USER_ID, request));
            verify(accountBalanceUpdateService, never()).applyWithRetry(any(), any(), any());
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
            verify(accountBalanceUpdateService).applyWithRetry(eq(USER_ID), eq(account), any());
            verify(transactionRepository).deleteById(TRANSACTION_ID, USER_ID);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if the transaction doesn't exist "
                + "(PF-818) -- this method's only orElseThrow guards the transaction lookup, not "
                + "an account lookup, unlike this ticket's original account-not-found framing")
        void shouldThrowOnTransactionNotFound() {
            // arrange
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> transactionService.deleteTransaction(USER_ID, TRANSACTION_ID));
            verify(accountBalanceUpdateService, never()).applyWithRetry(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Lookup Methods")
    class LookupTests {
        @Test
        @DisplayName("PF-856: should return the repository's actual content, not just confirm it "
                + "was called -- a mutant that discards the real result and returns empty anyway "
                + "was previously invisible to this test")
        void shouldGetCountByCategory() {
            CategoryTransactionsDto expected = new CategoryTransactionsDto(
                    CategoryDto.builder().id(5L).name("Groceries").build(), 3);
            when(transactionRepository.getCountByCategory(USER_ID)).thenReturn(List.of(expected));

            List<CategoryTransactionsDto> result = transactionService.getCountByCategory(USER_ID);

            assertEquals(1, result.size());
            assertEquals(expected, result.get(0));
            verify(transactionRepository).getCountByCategory(USER_ID);
        }

        @Test
        @DisplayName("PF-856: should return the mapped DTO's real content, not just an empty list")
        void shouldGetCategoriesWithTransactions() {
            Category category = Category.builder().id(5L).userId(USER_ID).name("Groceries").build();
            when(transactionRepository.getCategoriesWithTransactions(USER_ID)).thenReturn(List.of(category));

            List<CategoryDto> result = transactionService.getCategoriesWithTransactions(USER_ID);

            assertEquals(1, result.size());
            assertEquals(5L, result.get(0).id());
            assertEquals("Groceries", result.get(0).name());
            verify(transactionRepository).getCategoriesWithTransactions(USER_ID);
        }

        @Test
        @DisplayName("PF-856: should return the mapped DTO's real content, not just an empty list")
        void shouldGetMerchantsWithTransactions() {
            Merchant merchant = Merchant.builder().id(7L).userId(USER_ID).name("Costco").build();
            when(transactionRepository.getMerchantsWithTransactions(USER_ID)).thenReturn(List.of(merchant));

            List<MerchantDto> result = transactionService.getMerchantsWithTransactions(USER_ID);

            assertEquals(1, result.size());
            assertEquals(7L, result.get(0).id());
            assertEquals("Costco", result.get(0).name());
            verify(transactionRepository).getMerchantsWithTransactions(USER_ID);
        }
    }
}
