package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringSuggestionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Verifies {@code RecurringTransactionService}'s CRUD for already-confirmed recurring
 * transactions, one {@code @Nested} class per method below. Pattern-detection
 * ({@code findSuggestions}'s real logic) is tested directly against
 * {@link RecurringSuggestionFinder} in its own test class (PF-809) -- this class only confirms
 * {@code findSuggestions} delegates to it correctly.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecurringTransactionService Unit Tests")
class RecurringTransactionServiceTest {

    @Mock private RecurringTransactionRepository recurringRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;
    @Mock private MerchantRepository merchantRepository;
    @Mock private RecurringSuggestionFinder suggestionFinder;

    @InjectMocks private RecurringTransactionService recurringService;

    private static final Long USER_ID = 1L;
    private static final Long RECURRING_ID = 100L;

    /** {@code getRecurringTransactions} maps every active, next-date-ordered repository row for the user to a {@link RecurringTransactionDto}. */
    @Nested
    @DisplayName("getRecurringTransactions")
    class GetRecurringTransactionsTests {
        @Test
        @DisplayName("should return mapped DTOs for active recurring transactions")
        void shouldReturnList() {
            // arrange
            RecurringTransaction rt = RecurringTransaction.builder()
                    .id(RECURRING_ID)
                    .userId(USER_ID)
                    .frequency("MONTHLY")
                    .active(true)
                    .build();
            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID)).thenReturn(List.of(rt));

            // act
            List<RecurringTransactionDto> result = recurringService.getRecurringTransactions(USER_ID);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(RECURRING_ID, result.get(0).id());
        }
    }

    /** {@code findSuggestions} delegates entirely to {@link RecurringSuggestionFinder} (PF-809) -- its own pattern-detection logic is tested directly against that class, not here. */
    @Nested
    @DisplayName("findSuggestions")
    class FindSuggestionsTests {
        @Test
        @DisplayName("should delegate to RecurringSuggestionFinder and return its result unchanged")
        void shouldDelegateToSuggestionFinder() {
            // arrange
            List<RecurringSuggestionDto> expected = List.of();
            when(suggestionFinder.findSuggestions(USER_ID)).thenReturn(expected);

            // act
            List<RecurringSuggestionDto> result = recurringService.findSuggestions(USER_ID);

            // assert & verify
            assertSame(expected, result);
            verify(suggestionFinder).findSuggestions(USER_ID);
        }
    }

    /**
     * {@code createRecurringTransaction} checks, in order: the user exists, the account exists and
     * is owned by that user, and -- when a merchant id is given -- the merchant exists, each
     * throwing {@link ResourceNotFoundException} or (for the account-ownership case) {@link
     * AccessDeniedException} before ever reaching the repository insert.
     */
    @Nested
    @DisplayName("createRecurringTransaction")
    class CreateRecurringTransactionTests {

        @Test
        @DisplayName("should create successfully when everything is valid")
        void shouldCreate() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            when(accountRepository.findById(10L)).thenReturn(Optional.of(Account.builder().id(10L).userId(USER_ID).build()));
            when(merchantRepository.findById(20L)).thenReturn(Optional.of(Merchant.builder().id(20L).build()));
            when(recurringRepository.insert(any(), eq(USER_ID))).thenReturn(1);

            RecurringTransactionCreateRequest request = RecurringTransactionCreateRequest.builder()
                    .userId(USER_ID).accountId(10L).merchantId(20L).build();

            // act
            int result = recurringService.createRecurringTransaction(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if user not found")
        void shouldThrowOnUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> recurringService.createRecurringTransaction(USER_ID, null));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if account not found")
        void shouldThrowOnAccountNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().build()));
            when(accountRepository.findById(10L)).thenReturn(Optional.empty());
            RecurringTransactionCreateRequest request = RecurringTransactionCreateRequest.builder().accountId(10L).build();
            assertThrows(ResourceNotFoundException.class, () -> recurringService.createRecurringTransaction(USER_ID, request));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if account not owned by user")
        void shouldThrowOnAccountNotOwned() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().build()));
            when(accountRepository.findById(10L)).thenReturn(Optional.of(Account.builder().userId(999L).build()));
            RecurringTransactionCreateRequest request = RecurringTransactionCreateRequest.builder().accountId(10L).build();
            assertThrows(AccessDeniedException.class, () -> recurringService.createRecurringTransaction(USER_ID, request));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if merchant not found")
        void shouldThrowOnMerchantNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().build()));
            when(merchantRepository.findById(20L)).thenReturn(Optional.empty());
            RecurringTransactionCreateRequest request = RecurringTransactionCreateRequest.builder().merchantId(20L).build();
            assertThrows(ResourceNotFoundException.class, () -> recurringService.createRecurringTransaction(USER_ID, request));
        }
    }

    /**
     * {@code updateRecurringTransaction} checks, in order: the recurring transaction exists and is
     * owned by the caller, then -- when present on the request -- the new account exists and is
     * owned by the caller, then the new merchant exists, each throwing {@link
     * ResourceNotFoundException} or {@link AccessDeniedException} as appropriate before delegating
     * to the repository.
     */
    @Nested
    @DisplayName("updateRecurringTransaction")
    class UpdateRecurringTransactionTests {

        @Test
        @DisplayName("should update successfully when ownership and account are valid")
        void shouldUpdate() {
            // arrange
            RecurringTransaction rt = RecurringTransaction.builder().id(RECURRING_ID).userId(USER_ID).build();
            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.of(rt));
            when(accountRepository.findById(10L)).thenReturn(Optional.of(Account.builder().id(10L).userId(USER_ID).build()));
            when(merchantRepository.findById(20L)).thenReturn(Optional.of(Merchant.builder().id(20L).build()));
            when(recurringRepository.update(any(), eq(USER_ID))).thenReturn(1);

            RecurringTransactionUpdateRequest request = RecurringTransactionUpdateRequest.builder()
                    .id(RECURRING_ID).accountId(10L).merchantId(20L).build();

            // act
            int result = recurringService.updateRecurringTransaction(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if recurring not found")
        void shouldThrowOnNotFound() {
            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> recurringService.updateRecurringTransaction(USER_ID, RecurringTransactionUpdateRequest.builder().id(RECURRING_ID).build()));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if not owned")
        void shouldThrowOnrtOwnership() {
            RecurringTransaction rt = RecurringTransaction.builder().id(RECURRING_ID).userId(999L).build();
            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.of(rt));
            assertThrows(AccessDeniedException.class, () -> recurringService.updateRecurringTransaction(USER_ID, RecurringTransactionUpdateRequest.builder().id(RECURRING_ID).build()));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if account not found during update")
        void shouldThrowOnAccountNotFound() {
            RecurringTransaction rt = RecurringTransaction.builder().id(RECURRING_ID).userId(USER_ID).build();
            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.of(rt));
            when(accountRepository.findById(10L)).thenReturn(Optional.empty());
            RecurringTransactionUpdateRequest request = RecurringTransactionUpdateRequest.builder().id(RECURRING_ID).accountId(10L).build();
            assertThrows(ResourceNotFoundException.class, () -> recurringService.updateRecurringTransaction(USER_ID, request));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if account not owned during update")
        void shouldThrowOnAccountNotOwned() {
            RecurringTransaction rt = RecurringTransaction.builder().id(RECURRING_ID).userId(USER_ID).build();
            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.of(rt));
            when(accountRepository.findById(10L)).thenReturn(Optional.of(Account.builder().userId(999L).build()));
            RecurringTransactionUpdateRequest request = RecurringTransactionUpdateRequest.builder().id(RECURRING_ID).accountId(10L).build();
            assertThrows(AccessDeniedException.class, () -> recurringService.updateRecurringTransaction(USER_ID, request));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if merchant not found during update")
        void shouldThrowOnMerchantNotFound() {
            RecurringTransaction rt = RecurringTransaction.builder().id(RECURRING_ID).userId(USER_ID).build();
            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.of(rt));
            when(merchantRepository.findById(20L)).thenReturn(Optional.empty());
            RecurringTransactionUpdateRequest request = RecurringTransactionUpdateRequest.builder().id(RECURRING_ID).merchantId(20L).build();
            assertThrows(ResourceNotFoundException.class, () -> recurringService.updateRecurringTransaction(USER_ID, request));
        }
    }

    /** {@code deleteRecurringTransaction} requires the recurring transaction to be owned by the caller ({@link AccessDeniedException} otherwise) before delegating the delete to the repository. */
    @Nested
    @DisplayName("deleteRecurringTransaction")
    class DeleteRecurringTransactionTests {

        @Test
        @DisplayName("should delete successfully if owned")
        void shouldDelete() {
            // arrange
            RecurringTransaction rt = RecurringTransaction.builder().id(RECURRING_ID).userId(USER_ID).build();
            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.of(rt));
            when(recurringRepository.delete(RECURRING_ID, USER_ID)).thenReturn(1);

            // act
            int result = recurringService.deleteRecurringTransaction(USER_ID, RECURRING_ID);

            // assert & verify
            assertEquals(1, result);
        }

        @Test
        @DisplayName("should throw AccessDeniedException if not owned")
        void shouldThrowOnrtOwnership() {
            RecurringTransaction rt = RecurringTransaction.builder().id(RECURRING_ID).userId(999L).build();
            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.of(rt));
            assertThrows(AccessDeniedException.class, () -> recurringService.deleteRecurringTransaction(USER_ID, RECURRING_ID));
        }
    }
}
