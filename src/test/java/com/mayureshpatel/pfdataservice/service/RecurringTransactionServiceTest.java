package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringSuggestionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

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
@DisplayName("RecurringTransactionService Unit Tests")
class RecurringTransactionServiceTest {

    @Mock private RecurringTransactionRepository recurringRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;
    @Mock private MerchantRepository merchantRepository;

    @InjectMocks private RecurringTransactionService recurringService;

    private static final Long USER_ID = 1L;
    private static final Long RECURRING_ID = 100L;

    @Nested
    @DisplayName("getRecurringTransactions")
    class GetRecurringTransactionsTests {
        @Test
        @DisplayName("should return mapped DTOs for active recurring transactions")
        void shouldReturnList() {
            // Arrange
            RecurringTransaction rt = RecurringTransaction.builder()
                    .id(RECURRING_ID)
                    .userId(USER_ID)
                    .frequency("MONTHLY")
                    .active(true)
                    .build();
            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID)).thenReturn(List.of(rt));

            // Act
            List<RecurringTransactionDto> result = recurringService.getRecurringTransactions(USER_ID);

            // Assert
            assertEquals(1, result.size());
            assertEquals(RECURRING_ID, result.get(0).id());
        }
    }

    @Nested
    @DisplayName("findSuggestions")
    class FindSuggestionsTests {
        
        @Test
        @DisplayName("should detect various stable intervals (Weekly, Monthly, Bi-Weekly, Yearly)")
        void shouldDetectAllFrequencies() {
            // Arrange
            LocalDate now = LocalDate.now();
            OffsetDateTime zone = OffsetDateTime.now();
            
            // Weekly (avg ~7)
            Transaction w1 = Transaction.builder().transactionDate(now.minusDays(14).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.TEN).description("W").build();
            Transaction w2 = Transaction.builder().transactionDate(now.minusDays(7).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.TEN).description("W").build();
            Transaction w3 = Transaction.builder().transactionDate(now.atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.TEN).description("W").build();
            
            // Monthly (avg ~30)
            Transaction m1 = Transaction.builder().transactionDate(now.minusMonths(2).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.ONE).description("M").build();
            Transaction m2 = Transaction.builder().transactionDate(now.minusMonths(1).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.ONE).description("M").build();
            Transaction m3 = Transaction.builder().transactionDate(now.atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.ONE).description("M").build();

            // Bi-Weekly (avg ~14)
            Transaction b1 = Transaction.builder().transactionDate(now.minusWeeks(4).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.ZERO).description("B").build();
            Transaction b2 = Transaction.builder().transactionDate(now.minusWeeks(2).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.ZERO).description("B").build();
            Transaction b3 = Transaction.builder().transactionDate(now.atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.ZERO).description("B").build();

            // Yearly (avg ~365)
            Transaction y1 = Transaction.builder().transactionDate(now.minusDays(730).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.TEN).description("Y").build();
            Transaction y2 = Transaction.builder().transactionDate(now.minusDays(365).atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.TEN).description("Y").build();
            Transaction y3 = Transaction.builder().transactionDate(now.atStartOfDay().atOffset(zone.getOffset())).amount(BigDecimal.TEN).description("Y").build();

            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any())).thenReturn(List.of(w1, w2, w3, m1, m2, m3, b1, b2, b3, y1, y2, y3));

            // Act
            List<RecurringSuggestionDto> result = recurringService.findSuggestions(USER_ID);

            // Assert
            assertTrue(result.stream().anyMatch(s -> s.frequency() == Frequency.WEEKLY));
            assertTrue(result.stream().anyMatch(s -> s.frequency() == Frequency.MONTHLY));
            assertTrue(result.stream().anyMatch(s -> s.frequency() == Frequency.BI_WEEKLY));
            assertTrue(result.stream().anyMatch(s -> s.frequency() == Frequency.YEARLY));
        }

        @Test
        @DisplayName("should return empty if intervals are unstable")
        void shouldReturnEmptyForUnstableIntervals() {
            // Arrange
            LocalDate now = LocalDate.now();
            Transaction t1 = Transaction.builder().transactionDate(now.minusDays(40).atStartOfDay().atOffset(OffsetDateTime.now().getOffset())).amount(BigDecimal.TEN).description("R").build();
            Transaction t2 = Transaction.builder().transactionDate(now.minusDays(20).atStartOfDay().atOffset(OffsetDateTime.now().getOffset())).amount(BigDecimal.TEN).description("R").build();
            Transaction t3 = Transaction.builder().transactionDate(now.atStartOfDay().atOffset(OffsetDateTime.now().getOffset())).amount(BigDecimal.TEN).description("R").build();
            
            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any())).thenReturn(List.of(t1, t2, t3));

            // Act
            List<RecurringSuggestionDto> result = recurringService.findSuggestions(USER_ID);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should handle null names and empty descriptions and null merchants")
        void shouldHandleNullNames() {
            // Arrange
            Transaction t1 = Transaction.builder().description(null).merchant(null).build();
            Transaction t2 = Transaction.builder().description("  ").merchant(null).build();
            Merchant m = Merchant.builder().cleanName(null).build();
            Transaction t3 = Transaction.builder().description(null).merchant(m).build();
            Transaction t4 = Transaction.builder().description("D").merchant(null).build();
            
            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any())).thenReturn(List.of(t1, t2, t3, t4, t4, t4));

            // Act
            List<RecurringSuggestionDto> result = recurringService.findSuggestions(USER_ID);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return null if intervals match no known frequency")
        void shouldHandleNoMatchedFrequency() {
            // Arrange
            LocalDate now = LocalDate.now();
            // Avg interval ~50 (no frequency for this)
            Transaction t1 = Transaction.builder().transactionDate(now.minusDays(100).atStartOfDay().atOffset(OffsetDateTime.now().getOffset())).amount(BigDecimal.TEN).description("None").build();
            Transaction t2 = Transaction.builder().transactionDate(now.minusDays(50).atStartOfDay().atOffset(OffsetDateTime.now().getOffset())).amount(BigDecimal.TEN).description("None").build();
            Transaction t3 = Transaction.builder().transactionDate(now.atStartOfDay().atOffset(OffsetDateTime.now().getOffset())).amount(BigDecimal.TEN).description("None").build();
            
            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any())).thenReturn(List.of(t1, t2, t3));

            // Act
            List<RecurringSuggestionDto> result = recurringService.findSuggestions(USER_ID);

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("calculateNextDate")
    class CalculateNextDateTests {
        @Test
        @DisplayName("should calculate correct next dates for all frequencies via reflection")
        void shouldCalculateCorrectly() {
            // Arrange
            LocalDate last = LocalDate.of(2026, 3, 1);

            // Act & Assert
            assertEquals(last.plusMonths(1), ReflectionTestUtils.invokeMethod(recurringService, "calculateNextDate", last, Frequency.MONTHLY));
            assertEquals(last.plusWeeks(1), ReflectionTestUtils.invokeMethod(recurringService, "calculateNextDate", last, Frequency.WEEKLY));
            assertEquals(last.plusWeeks(2), ReflectionTestUtils.invokeMethod(recurringService, "calculateNextDate", last, Frequency.BI_WEEKLY));
            assertEquals(last.plusMonths(3), ReflectionTestUtils.invokeMethod(recurringService, "calculateNextDate", last, Frequency.QUARTERLY));
            assertEquals(last.plusYears(1), ReflectionTestUtils.invokeMethod(recurringService, "calculateNextDate", last, Frequency.YEARLY));
        }
    }

    @Nested
    @DisplayName("createRecurringTransaction")
    class CreateRecurringTransactionTests {
        
        @Test
        @DisplayName("should create successfully when everything is valid")
        void shouldCreate() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            when(accountRepository.findById(10L)).thenReturn(Optional.of(Account.builder().id(10L).userId(USER_ID).build()));
            when(merchantRepository.findById(20L)).thenReturn(Optional.of(Merchant.builder().id(20L).build()));
            when(recurringRepository.insert(any(), eq(USER_ID))).thenReturn(1);

            RecurringTransactionCreateRequest request = RecurringTransactionCreateRequest.builder()
                    .userId(USER_ID).accountId(10L).merchantId(20L).build();

            // Act
            int result = recurringService.createRecurringTransaction(USER_ID, request);

            // Assert
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

    @Nested
    @DisplayName("updateRecurringTransaction")
    class UpdateRecurringTransactionTests {
        
        @Test
        @DisplayName("should update successfully when ownership and account are valid")
        void shouldUpdate() {
            // Arrange
            RecurringTransaction rt = RecurringTransaction.builder().id(RECURRING_ID).userId(USER_ID).build();
            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.of(rt));
            when(accountRepository.findById(10L)).thenReturn(Optional.of(Account.builder().id(10L).userId(USER_ID).build()));
            when(merchantRepository.findById(20L)).thenReturn(Optional.of(Merchant.builder().id(20L).build()));
            when(recurringRepository.update(any(), eq(USER_ID))).thenReturn(1);

            RecurringTransactionUpdateRequest request = RecurringTransactionUpdateRequest.builder()
                    .id(RECURRING_ID).accountId(10L).merchantId(20L).build();

            // Act
            int result = recurringService.updateRecurringTransaction(USER_ID, request);

            // Assert
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

    @Nested
    @DisplayName("deleteRecurringTransaction")
    class DeleteRecurringTransactionTests {
        
        @Test
        @DisplayName("should delete successfully if owned")
        void shouldDelete() {
            // Arrange
            RecurringTransaction rt = RecurringTransaction.builder().id(RECURRING_ID).userId(USER_ID).build();
            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.of(rt));
            when(recurringRepository.delete(RECURRING_ID, USER_ID)).thenReturn(1);

            // Act
            int result = recurringService.deleteRecurringTransaction(USER_ID, RECURRING_ID);

            // Assert
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
