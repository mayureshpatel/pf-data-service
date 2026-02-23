package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransferSuggestionDto;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService unit tests")
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

    @InjectMocks
    private TransactionService transactionService;

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Account buildAccount(Long id, User user, BigDecimal balance) {
        Account account = new Account();
        account.setId(id);
        account.setUser(user);
        account.setCurrentBalance(balance);
        return account;
    }

    private Transaction buildTransaction(Long id, Account account, BigDecimal amount,
                                         TransactionType type, OffsetDateTime date) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setAccount(account);
        t.setAmount(amount);
        t.setType(type);
        t.setTransactionDate(date);
        t.setMerchant(new Merchant()); // Required for TransactionDto.mapToDto
        return t;
    }

    @Nested
    @DisplayName("findPotentialTransfers()")
    class FindPotentialTransfersTests {

        @Test
        @DisplayName("should return empty list when no transactions exist")
        void findPotentialTransfers_noTransactions_returnsEmptyList() {
            when(transactionRepository.findRecentNonTransferTransactions(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(List.of());

            List<TransferSuggestionDto> result = transactionService.findPotentialTransfers(USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should detect a transfer pair when amounts match, types differ, accounts differ, and within 3 days")
        void findPotentialTransfers_matchingPair_returnsSuggestion() {
            User user = buildUser(USER_ID);
            Account accountA = buildAccount(1L, user, BigDecimal.ZERO);
            Account accountB = buildAccount(2L, user, BigDecimal.ZERO);
            OffsetDateTime now = OffsetDateTime.now();

            Transaction t1 = buildTransaction(1L, accountA, new BigDecimal("100"), TransactionType.INCOME, now);
            Transaction t2 = buildTransaction(2L, accountB, new BigDecimal("100"), TransactionType.EXPENSE,
                    now.plusDays(1));

            when(transactionRepository.findRecentNonTransferTransactions(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(List.of(t1, t2));

            List<TransferSuggestionDto> result = transactionService.findPotentialTransfers(USER_ID);

            assertThat(result).hasSize(1);
            // confidence = 0.9 - (daysDiff * 0.1) = 0.9 - (1 * 0.1) = 0.8
            assertThat(result.get(0).confidenceScore()).isCloseTo(0.8, org.assertj.core.data.Offset.offset(1e-9));
        }

        @Test
        @DisplayName("should not suggest transfer when transactions are in the same account")
        void findPotentialTransfers_sameAccount_noSuggestion() {
            User user = buildUser(USER_ID);
            Account account = buildAccount(1L, user, BigDecimal.ZERO);
            OffsetDateTime now = OffsetDateTime.now();

            Transaction t1 = buildTransaction(1L, account, new BigDecimal("100"), TransactionType.INCOME, now);
            Transaction t2 = buildTransaction(2L, account, new BigDecimal("100"), TransactionType.EXPENSE, now);

            when(transactionRepository.findRecentNonTransferTransactions(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(List.of(t1, t2));

            List<TransferSuggestionDto> result = transactionService.findPotentialTransfers(USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should not suggest transfer when transaction types are the same")
        void findPotentialTransfers_sameType_noSuggestion() {
            User user = buildUser(USER_ID);
            Account accountA = buildAccount(1L, user, BigDecimal.ZERO);
            Account accountB = buildAccount(2L, user, BigDecimal.ZERO);
            OffsetDateTime now = OffsetDateTime.now();

            Transaction t1 = buildTransaction(1L, accountA, new BigDecimal("100"), TransactionType.EXPENSE, now);
            Transaction t2 = buildTransaction(2L, accountB, new BigDecimal("100"), TransactionType.EXPENSE, now);

            when(transactionRepository.findRecentNonTransferTransactions(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(List.of(t1, t2));

            List<TransferSuggestionDto> result = transactionService.findPotentialTransfers(USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should not suggest transfer when amounts differ")
        void findPotentialTransfers_differentAmounts_noSuggestion() {
            User user = buildUser(USER_ID);
            Account accountA = buildAccount(1L, user, BigDecimal.ZERO);
            Account accountB = buildAccount(2L, user, BigDecimal.ZERO);
            OffsetDateTime now = OffsetDateTime.now();

            Transaction t1 = buildTransaction(1L, accountA, new BigDecimal("100"), TransactionType.INCOME, now);
            Transaction t2 = buildTransaction(2L, accountB, new BigDecimal("200"), TransactionType.EXPENSE, now);

            when(transactionRepository.findRecentNonTransferTransactions(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(List.of(t1, t2));

            List<TransferSuggestionDto> result = transactionService.findPotentialTransfers(USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should use confidence score of 0.9 when transactions occur on the same day")
        void findPotentialTransfers_sameDayTransactions_confidenceIsMax() {
            User user = buildUser(USER_ID);
            Account accountA = buildAccount(1L, user, BigDecimal.ZERO);
            Account accountB = buildAccount(2L, user, BigDecimal.ZERO);
            OffsetDateTime now = OffsetDateTime.now();

            Transaction t1 = buildTransaction(1L, accountA, new BigDecimal("50"), TransactionType.INCOME, now);
            Transaction t2 = buildTransaction(2L, accountB, new BigDecimal("50"), TransactionType.EXPENSE, now);

            when(transactionRepository.findRecentNonTransferTransactions(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(List.of(t1, t2));

            List<TransferSuggestionDto> result = transactionService.findPotentialTransfers(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).confidenceScore()).isEqualTo(0.9); // 0.9 - 0 * 0.1
        }
    }

    @Nested
    @DisplayName("markAsTransfer()")
    class MarkAsTransferTests {

        @Test
        @DisplayName("should change INCOME transaction to TRANSFER_IN")
        void markAsTransfer_incomeTransaction_changesTypeToTransferIn() {
            User user = buildUser(USER_ID);
            Account account = buildAccount(1L, user, new BigDecimal("500"));
            Transaction t = buildTransaction(1L, account, new BigDecimal("100"), TransactionType.INCOME,
                    OffsetDateTime.now());

            when(transactionRepository.findAllById(List.of(1L))).thenReturn(List.of(t));
            when(transactionRepository.saveAll(anyList())).thenReturn(List.of(t));

            transactionService.markAsTransfer(USER_ID, List.of(1L));

            assertThat(t.getType()).isEqualTo(TransactionType.TRANSFER_IN);
            verify(transactionRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("should change EXPENSE transaction to TRANSFER_OUT")
        void markAsTransfer_expenseTransaction_changesTypeToTransferOut() {
            User user = buildUser(USER_ID);
            Account account = buildAccount(1L, user, new BigDecimal("500"));
            Transaction t = buildTransaction(1L, account, new BigDecimal("100"), TransactionType.EXPENSE,
                    OffsetDateTime.now());

            when(transactionRepository.findAllById(List.of(1L))).thenReturn(List.of(t));
            when(transactionRepository.saveAll(anyList())).thenReturn(List.of(t));

            transactionService.markAsTransfer(USER_ID, List.of(1L));

            assertThat(t.getType()).isEqualTo(TransactionType.TRANSFER_OUT);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when user does not own the transaction")
        void markAsTransfer_notOwnedTransaction_throwsAccessDeniedException() {
            Long anotherUserId = 99L;
            User owner = buildUser(anotherUserId); // different user owns it
            Account account = buildAccount(1L, owner, BigDecimal.ZERO);
            Transaction t = buildTransaction(1L, account, BigDecimal.TEN, TransactionType.EXPENSE,
                    OffsetDateTime.now());

            when(transactionRepository.findAllById(List.of(1L))).thenReturn(List.of(t));

            assertThatThrownBy(() -> transactionService.markAsTransfer(USER_ID, List.of(1L)))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("should clear the category after marking as transfer")
        void markAsTransfer_transaction_clearsCategoryField() {
            User user = buildUser(USER_ID);
            Account account = buildAccount(1L, user, new BigDecimal("200"));
            Transaction t = buildTransaction(1L, account, new BigDecimal("50"), TransactionType.EXPENSE,
                    OffsetDateTime.now());
            Category cat = new Category();
            cat.setId(5L);
            t.setCategory(cat);

            when(transactionRepository.findAllById(List.of(1L))).thenReturn(List.of(t));
            when(transactionRepository.saveAll(anyList())).thenReturn(List.of(t));

            transactionService.markAsTransfer(USER_ID, List.of(1L));

            assertThat(t.getCategory()).isNull();
        }
    }

    @Nested
    @DisplayName("deleteTransaction()")
    class DeleteTransactionTests {

        @Test
        @DisplayName("should throw ResourceNotFoundException when transaction is not found")
        void deleteTransaction_notFound_throwsResourceNotFoundException() {
            when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.deleteTransaction(USER_ID, 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Transaction not found");
        }

        @Test
        @DisplayName("should throw AccessDeniedException when user does not own the transaction")
        void deleteTransaction_notOwned_throwsAccessDeniedException() {
            User owner = buildUser(99L); // another user
            Account account = buildAccount(1L, owner, BigDecimal.ZERO);
            Transaction t = buildTransaction(1L, account, BigDecimal.TEN, TransactionType.EXPENSE,
                    OffsetDateTime.now());

            when(transactionRepository.findById(1L)).thenReturn(Optional.of(t));

            assertThatThrownBy(() -> transactionService.deleteTransaction(USER_ID, 1L))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("should undo transaction and call delete when successful")
        void deleteTransaction_owned_undoesTransactionAndDeletes() {
            User user = buildUser(USER_ID);
            Account account = buildAccount(1L, user, new BigDecimal("900"));
            Transaction t = buildTransaction(1L, account, new BigDecimal("100"), TransactionType.INCOME,
                    OffsetDateTime.now());

            when(transactionRepository.findById(1L)).thenReturn(Optional.of(t));

            transactionService.deleteTransaction(USER_ID, 1L);

            // INCOME getNetChange = +100, undoTransaction subtracts net: 900 - 100 = 800
            assertThat(account.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("800.00"));
            verify(transactionRepository).delete(t);
        }
    }

    @Nested
    @DisplayName("deleteTransactions()")
    class DeleteTransactionsTests {

        @Test
        @DisplayName("should return immediately when list is null")
        void deleteTransactions_nullList_returnsWithoutRepositoryCall() {
            transactionService.deleteTransactions(USER_ID, null);

            verify(transactionRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("should return immediately when list is empty")
        void deleteTransactions_emptyList_returnsWithoutRepositoryCall() {
            transactionService.deleteTransactions(USER_ID, List.of());

            verify(transactionRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when not all transactions are owned by the user")
        void deleteTransactions_notAllOwned_throwsAccessDeniedException() {
            User user = buildUser(USER_ID);
            User anotherUser = buildUser(99L);
            Account ownedAccount = buildAccount(1L, user, BigDecimal.ZERO);
            Account otherAccount = buildAccount(2L, anotherUser, BigDecimal.ZERO);

            Transaction t1 = buildTransaction(1L, ownedAccount, BigDecimal.TEN, TransactionType.EXPENSE,
                    OffsetDateTime.now());
            Transaction t2 = buildTransaction(2L, otherAccount, BigDecimal.TEN, TransactionType.EXPENSE,
                    OffsetDateTime.now());

            when(transactionRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(t1, t2));

            assertThatThrownBy(() -> transactionService.deleteTransactions(USER_ID, List.of(1L, 2L)))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("one or more");
        }

        @Test
        @DisplayName("should undo transactions and call deleteAll when all are owned")
        void deleteTransactions_allOwned_deletesAll() {
            User user = buildUser(USER_ID);
            Account account = buildAccount(1L, user, new BigDecimal("500"));
            Transaction t = buildTransaction(1L, account, new BigDecimal("100"), TransactionType.EXPENSE,
                    OffsetDateTime.now());

            when(transactionRepository.findAllById(List.of(1L))).thenReturn(List.of(t));

            transactionService.deleteTransactions(USER_ID, List.of(1L));

            // EXPENSE getNetChange() = -100; undoTransaction subtracts net: 500 - (-100) = 600
            assertThat(account.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("600.00"));
            verify(transactionRepository).deleteAll(anyList());
        }
    }

    @Nested
    @DisplayName("createTransaction()")
    class CreateTransactionTests {

        @Test
        @DisplayName("should throw ResourceNotFoundException when dto.account() is null")
        void createTransaction_nullAccount_throwsResourceNotFoundException() {
            TransactionDto dto = TransactionDto.builder()
                    .date(OffsetDateTime.now())
                    .description("Test")
                    .amount(BigDecimal.TEN)
                    .type(TransactionType.EXPENSE)
                    .build();

            assertThatThrownBy(() -> transactionService.createTransaction(USER_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when account is not found in repository")
        void createTransaction_accountNotFound_throwsResourceNotFoundException() {
            AccountDto accountDto = new AccountDto(ACCOUNT_ID, null, null, null, null, null, null);
            TransactionDto dto = TransactionDto.builder()
                    .account(accountDto)
                    .date(OffsetDateTime.now())
                    .description("Test")
                    .amount(BigDecimal.TEN)
                    .type(TransactionType.EXPENSE)
                    .build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.createTransaction(USER_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found");
        }

        @Test
        @DisplayName("should throw AccessDeniedException when user does not own the account")
        void createTransaction_notOwnedAccount_throwsAccessDeniedException() {
            User anotherUser = buildUser(99L);
            Account account = buildAccount(ACCOUNT_ID, anotherUser, new BigDecimal("500"));
            AccountDto accountDto = new AccountDto(ACCOUNT_ID, null, null, null, null, null, null);
            TransactionDto dto = TransactionDto.builder()
                    .account(accountDto)
                    .date(OffsetDateTime.now())
                    .description("Test")
                    .amount(BigDecimal.TEN)
                    .type(TransactionType.EXPENSE)
                    .build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            assertThatThrownBy(() -> transactionService.createTransaction(USER_ID, dto))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when category provided is a parent category")
        void createTransaction_parentCategory_throwsIllegalArgumentException() {
            User user = buildUser(USER_ID);
            Account account = buildAccount(ACCOUNT_ID, user, new BigDecimal("500"));
            AccountDto accountDto = new AccountDto(ACCOUNT_ID, null, null, null, null, null, null);

            Category parentCategory = Category.builder().id(1L).name("Food").parent(null).build();

            TransactionDto dto = TransactionDto.builder()
                    .account(accountDto)
                    .date(OffsetDateTime.now())
                    .description("Test")
                    .amount(BigDecimal.TEN)
                    .type(TransactionType.EXPENSE)
                    .category(parentCategory)
                    .build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));

            assertThatThrownBy(() -> transactionService.createTransaction(USER_ID, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subcategor");
        }

        @Test
        @DisplayName("should save transaction and return DTO when no category is specified (uses categorizer)")
        void createTransaction_noCategorySpecified_usesCategorizerAndSaves() {
            User user = buildUser(USER_ID);
            Account account = buildAccount(ACCOUNT_ID, user, new BigDecimal("1000"));
            AccountDto accountDto = new AccountDto(ACCOUNT_ID, null, null, null, null, null, null);
            TransactionDto dto = TransactionDto.builder()
                    .account(accountDto)
                    .date(OffsetDateTime.now())
                    .description("Coffee")
                    .amount(new BigDecimal("5.00"))
                    .type(TransactionType.EXPENSE)
                    .build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(categorizer.guessCategory(any(), any(), any())).thenReturn(-1L);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
                Transaction t = inv.getArgument(0);
                t.setId(100L);
                return t;
            });

            TransactionDto result = transactionService.createTransaction(USER_ID, dto);

            assertThat(result).isNotNull();
            verify(transactionRepository).save(any(Transaction.class));
            // Account balance should be updated: 1000 - 5 = 995
            assertThat(account.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("995.00"));
        }
    }

    @Nested
    @DisplayName("updateTransaction()")
    class UpdateTransactionTests {

        @Test
        @DisplayName("should throw ResourceNotFoundException when transaction is not found")
        void updateTransaction_notFound_throwsResourceNotFoundException() {
            TransactionDto dto = TransactionDto.builder()
                    .date(OffsetDateTime.now())
                    .description("Update")
                    .amount(BigDecimal.TEN)
                    .type(TransactionType.EXPENSE)
                    .build();

            when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.updateTransaction(USER_ID, 99L, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Transaction not found");
        }

        @Test
        @DisplayName("should throw AccessDeniedException when user does not own the transaction")
        void updateTransaction_notOwned_throwsAccessDeniedException() {
            User owner = buildUser(99L);
            Account account = buildAccount(1L, owner, BigDecimal.ZERO);
            Transaction t = buildTransaction(1L, account, BigDecimal.TEN, TransactionType.EXPENSE,
                    OffsetDateTime.now());

            TransactionDto dto = TransactionDto.builder()
                    .date(OffsetDateTime.now())
                    .description("Update")
                    .amount(BigDecimal.TEN)
                    .type(TransactionType.EXPENSE)
                    .build();

            when(transactionRepository.findById(1L)).thenReturn(Optional.of(t));

            assertThatThrownBy(() -> transactionService.updateTransaction(USER_ID, 1L, dto))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("should update and save transaction when user owns it and no category specified")
        void updateTransaction_ownedAndValid_savesAndReturnsDto() {
            User user = buildUser(USER_ID);
            Account account = buildAccount(1L, user, new BigDecimal("400"));
            Transaction t = buildTransaction(1L, account, new BigDecimal("100"), TransactionType.EXPENSE,
                    OffsetDateTime.now());

            TransactionDto dto = TransactionDto.builder()
                    .date(OffsetDateTime.now())
                    .description("Updated description")
                    .amount(new BigDecimal("80"))
                    .type(TransactionType.EXPENSE)
                    .build();

            when(transactionRepository.findById(1L)).thenReturn(Optional.of(t));
            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(categorizer.guessCategory(any(), any(), any())).thenReturn(-1L);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            TransactionDto result = transactionService.updateTransaction(USER_ID, 1L, dto);

            assertThat(result).isNotNull();
            assertThat(t.getDescription()).isEqualTo("Updated description");
            verify(transactionRepository).save(t);
        }
    }

    @Nested
    @DisplayName("updateTransactions()")
    class UpdateTransactionsTests {

        @Test
        @DisplayName("should return empty list when input is null")
        void updateTransactions_nullInput_returnsEmptyList() {
            List<TransactionDto> result = transactionService.updateTransactions(USER_ID, null);

            assertThat(result).isEmpty();
            verify(transactionRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("should return empty list when input is empty")
        void updateTransactions_emptyInput_returnsEmptyList() {
            List<TransactionDto> result = transactionService.updateTransactions(USER_ID, List.of());

            assertThat(result).isEmpty();
            verify(transactionRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when some transactions are not found")
        void updateTransactions_missingTransactions_throwsResourceNotFoundException() {
            TransactionDto dto = TransactionDto.builder()
                    .id(999L)
                    .date(OffsetDateTime.now())
                    .description("Test")
                    .amount(BigDecimal.TEN)
                    .type(TransactionType.EXPENSE)
                    .build();

            when(transactionRepository.findAllById(List.of(999L))).thenReturn(List.of());

            assertThatThrownBy(() -> transactionService.updateTransactions(USER_ID, List.of(dto)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("not found");
        }
    }
}
