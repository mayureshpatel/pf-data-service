package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.transaction.RecurringSuggestionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.RecurringTransactionDto;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecurringTransactionService unit tests")
class RecurringTransactionServiceTest {

    @Mock
    private RecurringTransactionRepository recurringRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RecurringTransactionService recurringTransactionService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long RECURRING_ID = 10L;
    private static final Long ACCOUNT_ID = 20L;

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("testuser");
        return user;
    }

    private Account buildAccount(Long accountId, Long userId) {
        Account account = new Account();
        account.setId(accountId);
        account.setUser(buildUser(userId));
        account.setName("Checking");
        account.setCurrentBalance(new BigDecimal("1000.00"));
        account.setAudit(new TableAudit());
        return account;
    }

    private Merchant buildMerchant(String name) {
        Merchant merchant = new Merchant();
        merchant.setName(name);
        merchant.setOriginalName(name);
        return merchant;
    }

    private RecurringTransaction buildRecurring(Long id, Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        return RecurringTransaction.builder()
                .id(id)
                .user(buildUser(userId))
                .account(buildAccount(ACCOUNT_ID, userId))
                .merchant(buildMerchant("Netflix"))
                .amount(new BigDecimal("15.99"))
                .frequency(Frequency.MONTHLY)
                .lastDate(now.minusMonths(1))
                .nextDate(now.plusDays(5))
                .active(true)
                .build();
    }

    private RecurringTransactionDto buildDto(Long accountId, String merchantName,
                                             BigDecimal amount, Frequency frequency,
                                             OffsetDateTime lastDate, OffsetDateTime nextDate,
                                             boolean active) {
        AccountDto accountDto = accountId != null
                ? new AccountDto(accountId, null, "Checking", null, null, null, null)
                : null;
        MerchantDto merchantDto = merchantName != null
                ? new MerchantDto(null, null, merchantName, merchantName)
                : null;
        return RecurringTransactionDto.builder()
                .account(accountDto)
                .merchant(merchantDto)
                .amount(amount)
                .frequency(frequency)
                .lastDate(lastDate)
                .nextDate(nextDate)
                .active(active)
                .build();
    }

    /**
     * Builds a list of transactions where consecutive transactions are separated
     * by the given intervalDays. The list has 'count' transactions starting from baseDate.
     */
    private List<Transaction> buildTransactionGroup(String merchantName, BigDecimal amount,
                                                     OffsetDateTime baseDate, int intervalDays,
                                                     int count) {
        List<Transaction> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Merchant merchant = buildMerchant(merchantName);
            Transaction tx = new Transaction();
            tx.setId((long) i + 1);
            tx.setMerchant(merchant);
            tx.setAmount(amount);
            tx.setTransactionDate(baseDate.plusDays((long) i * intervalDays));
            tx.setType(TransactionType.EXPENSE);
            list.add(tx);
        }
        return list;
    }

    @Nested
    class GetRecurringTransactionsTest {

        @Test
        @DisplayName("should return mapped DTOs for all active recurring transactions")
        void getRecurringTransactions_happyPath_returnsMappedDtos() {
            // Arrange
            RecurringTransaction rt1 = buildRecurring(10L, USER_ID);
            RecurringTransaction rt2 = buildRecurring(11L, USER_ID);
            rt2.setAmount(new BigDecimal("9.99"));

            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID))
                    .thenReturn(List.of(rt1, rt2));

            // Act
            List<RecurringTransactionDto> result = recurringTransactionService.getRecurringTransactions(USER_ID);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(RecurringTransactionDto::amount)
                    .containsExactlyInAnyOrder(new BigDecimal("15.99"), new BigDecimal("9.99"));
            assertThat(result).allMatch(RecurringTransactionDto::active);
        }

        @Test
        @DisplayName("should return empty list when no active recurring transactions exist")
        void getRecurringTransactions_noRecurring_returnsEmptyList() {
            // Arrange
            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID)).thenReturn(List.of());

            // Act
            List<RecurringTransactionDto> result = recurringTransactionService.getRecurringTransactions(USER_ID);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class CreateRecurringTransactionTest {

        @Test
        @DisplayName("should save and return DTO when account is valid")
        void createRecurringTransaction_withValidAccount_savesAndReturnsDto() {
            // Arrange
            User user = buildUser(USER_ID);
            Account account = buildAccount(ACCOUNT_ID, USER_ID);
            OffsetDateTime lastDate = OffsetDateTime.now().minusMonths(1);
            OffsetDateTime nextDate = OffsetDateTime.now();
            RecurringTransactionDto dto = buildDto(ACCOUNT_ID, "Netflix", new BigDecimal("15.99"),
                    Frequency.MONTHLY, lastDate, nextDate, true);

            RecurringTransaction saved = buildRecurring(RECURRING_ID, USER_ID);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(recurringRepository.save(any(RecurringTransaction.class))).thenReturn(saved);

            // Act
            RecurringTransactionDto result = recurringTransactionService.createRecurringTransaction(USER_ID, dto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(RECURRING_ID);
            assertThat(result.active()).isTrue();

            ArgumentCaptor<RecurringTransaction> captor = ArgumentCaptor.forClass(RecurringTransaction.class);
            verify(recurringRepository).save(captor.capture());
            RecurringTransaction captured = captor.getValue();
            assertThat(captured.getUser().getId()).isEqualTo(USER_ID);
            assertThat(captured.getAccount().getId()).isEqualTo(ACCOUNT_ID);
            assertThat(captured.getAmount()).isEqualByComparingTo(new BigDecimal("15.99"));
            assertThat(captured.getFrequency()).isEqualTo(Frequency.MONTHLY);
            assertThat(captured.isActive()).isTrue();
        }

        @Test
        @DisplayName("should save with null account when account dto is null")
        void createRecurringTransaction_withoutAccount_savesWithNullAccount() {
            // Arrange
            User user = buildUser(USER_ID);
            OffsetDateTime lastDate = OffsetDateTime.now().minusMonths(1);
            OffsetDateTime nextDate = OffsetDateTime.now();
            RecurringTransactionDto dto = buildDto(null, "Netflix", new BigDecimal("15.99"),
                    Frequency.MONTHLY, lastDate, nextDate, true);

            RecurringTransaction saved = RecurringTransaction.builder()
                    .id(RECURRING_ID)
                    .user(user)
                    .account(null)
                    .merchant(buildMerchant("Netflix"))
                    .amount(new BigDecimal("15.99"))
                    .frequency(Frequency.MONTHLY)
                    .active(true)
                    .build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(recurringRepository.save(any(RecurringTransaction.class))).thenReturn(saved);

            // Act
            RecurringTransactionDto result = recurringTransactionService.createRecurringTransaction(USER_ID, dto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.account()).isNull();
            verify(accountRepository, never()).findById(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user is not found")
        void createRecurringTransaction_userNotFound_throwsResourceNotFoundException() {
            // Arrange
            OffsetDateTime now = OffsetDateTime.now();
            RecurringTransactionDto dto = buildDto(null, "Netflix", new BigDecimal("15.99"),
                    Frequency.MONTHLY, now, now, true);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> recurringTransactionService.createRecurringTransaction(USER_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(recurringRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when account is not found")
        void createRecurringTransaction_accountNotFound_throwsResourceNotFoundException() {
            // Arrange
            User user = buildUser(USER_ID);
            OffsetDateTime now = OffsetDateTime.now();
            RecurringTransactionDto dto = buildDto(ACCOUNT_ID, "Netflix", new BigDecimal("15.99"),
                    Frequency.MONTHLY, now, now, true);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> recurringTransactionService.createRecurringTransaction(USER_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found");

            verify(recurringRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when account is owned by a different user")
        void createRecurringTransaction_accountOwnedByDifferentUser_throwsAccessDeniedException() {
            // Arrange
            User user = buildUser(USER_ID);
            Account foreignAccount = buildAccount(ACCOUNT_ID, OTHER_USER_ID);
            OffsetDateTime now = OffsetDateTime.now();
            RecurringTransactionDto dto = buildDto(ACCOUNT_ID, "Netflix", new BigDecimal("15.99"),
                    Frequency.MONTHLY, now, now, true);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(foreignAccount));

            // Act & Assert
            assertThatThrownBy(() -> recurringTransactionService.createRecurringTransaction(USER_ID, dto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied to account");

            verify(recurringRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateRecurringTransactionTest {

        @Test
        @DisplayName("should update all fields and return mapped DTO when valid")
        void updateRecurringTransaction_happyPath_updatesAndReturnsDto() {
            // Arrange
            RecurringTransaction existing = buildRecurring(RECURRING_ID, USER_ID);
            Account account = buildAccount(ACCOUNT_ID, USER_ID);
            OffsetDateTime newLastDate = OffsetDateTime.now().minusDays(14);
            OffsetDateTime newNextDate = OffsetDateTime.now().plusDays(14);
            RecurringTransactionDto dto = buildDto(ACCOUNT_ID, "Spotify", new BigDecimal("9.99"),
                    Frequency.MONTHLY, newLastDate, newNextDate, false);

            RecurringTransaction saved = buildRecurring(RECURRING_ID, USER_ID);
            saved.setAmount(new BigDecimal("9.99"));
            saved.setActive(false);

            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.of(existing));
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(recurringRepository.save(any(RecurringTransaction.class))).thenReturn(saved);

            // Act
            RecurringTransactionDto result = recurringTransactionService.updateRecurringTransaction(USER_ID, RECURRING_ID, dto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("9.99"));
            assertThat(result.active()).isFalse();

            ArgumentCaptor<RecurringTransaction> captor = ArgumentCaptor.forClass(RecurringTransaction.class);
            verify(recurringRepository).save(captor.capture());
            RecurringTransaction captured = captor.getValue();
            assertThat(captured.getAmount()).isEqualByComparingTo(new BigDecimal("9.99"));
            assertThat(captured.getFrequency()).isEqualTo(Frequency.MONTHLY);
            assertThat(captured.isActive()).isFalse();
        }

        @Test
        @DisplayName("should set account to null when dto account is null")
        void updateRecurringTransaction_dtoAccountIsNull_clearsAccount() {
            // Arrange
            RecurringTransaction existing = buildRecurring(RECURRING_ID, USER_ID);
            OffsetDateTime now = OffsetDateTime.now();
            RecurringTransactionDto dto = buildDto(null, "Netflix", new BigDecimal("15.99"),
                    Frequency.MONTHLY, now, now, true);

            RecurringTransaction saved = buildRecurring(RECURRING_ID, USER_ID);
            saved.setAccount(null);

            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.of(existing));
            when(recurringRepository.save(any(RecurringTransaction.class))).thenReturn(saved);

            // Act
            RecurringTransactionDto result = recurringTransactionService.updateRecurringTransaction(USER_ID, RECURRING_ID, dto);

            // Assert
            assertThat(result.account()).isNull();

            ArgumentCaptor<RecurringTransaction> captor = ArgumentCaptor.forClass(RecurringTransaction.class);
            verify(recurringRepository).save(captor.capture());
            assertThat(captor.getValue().getAccount()).isNull();
            verify(accountRepository, never()).findById(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when recurring transaction not found")
        void updateRecurringTransaction_notFound_throwsResourceNotFoundException() {
            // Arrange
            OffsetDateTime now = OffsetDateTime.now();
            RecurringTransactionDto dto = buildDto(null, "Netflix", new BigDecimal("15.99"),
                    Frequency.MONTHLY, now, now, true);
            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> recurringTransactionService.updateRecurringTransaction(USER_ID, RECURRING_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Recurring transaction not found");

            verify(recurringRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when owned by a different user")
        void updateRecurringTransaction_ownedByDifferentUser_throwsAccessDeniedException() {
            // Arrange
            RecurringTransaction existing = buildRecurring(RECURRING_ID, OTHER_USER_ID);
            OffsetDateTime now = OffsetDateTime.now();
            RecurringTransactionDto dto = buildDto(null, "Netflix", new BigDecimal("15.99"),
                    Frequency.MONTHLY, now, now, true);
            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> recurringTransactionService.updateRecurringTransaction(USER_ID, RECURRING_ID, dto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied");

            verify(recurringRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when new account is not found")
        void updateRecurringTransaction_newAccountNotFound_throwsResourceNotFoundException() {
            // Arrange
            RecurringTransaction existing = buildRecurring(RECURRING_ID, USER_ID);
            OffsetDateTime now = OffsetDateTime.now();
            RecurringTransactionDto dto = buildDto(ACCOUNT_ID, "Netflix", new BigDecimal("15.99"),
                    Frequency.MONTHLY, now, now, true);

            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.of(existing));
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> recurringTransactionService.updateRecurringTransaction(USER_ID, RECURRING_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found");

            verify(recurringRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when new account is owned by a different user")
        void updateRecurringTransaction_newAccountOwnedByDifferentUser_throwsAccessDeniedException() {
            // Arrange
            RecurringTransaction existing = buildRecurring(RECURRING_ID, USER_ID);
            Account foreignAccount = buildAccount(ACCOUNT_ID, OTHER_USER_ID);
            OffsetDateTime now = OffsetDateTime.now();
            RecurringTransactionDto dto = buildDto(ACCOUNT_ID, "Netflix", new BigDecimal("15.99"),
                    Frequency.MONTHLY, now, now, true);

            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.of(existing));
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(foreignAccount));

            // Act & Assert
            assertThatThrownBy(() -> recurringTransactionService.updateRecurringTransaction(USER_ID, RECURRING_ID, dto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied to account");

            verify(recurringRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteRecurringTransactionTest {

        @Test
        @DisplayName("should call repository delete with correct id and userId")
        void deleteRecurringTransaction_happyPath_deletesById() {
            // Arrange
            RecurringTransaction existing = buildRecurring(RECURRING_ID, USER_ID);
            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.of(existing));

            // Act
            recurringTransactionService.deleteRecurringTransaction(USER_ID, RECURRING_ID);

            // Assert
            verify(recurringRepository).delete(RECURRING_ID, USER_ID);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void deleteRecurringTransaction_notFound_throwsResourceNotFoundException() {
            // Arrange
            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> recurringTransactionService.deleteRecurringTransaction(USER_ID, RECURRING_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Recurring transaction not found");

            verify(recurringRepository, never()).delete(any(Long.class), any(Long.class));
        }

        @Test
        @DisplayName("should throw AccessDeniedException when owned by a different user")
        void deleteRecurringTransaction_ownedByDifferentUser_throwsAccessDeniedException() {
            // Arrange
            RecurringTransaction existing = buildRecurring(RECURRING_ID, OTHER_USER_ID);
            when(recurringRepository.findById(RECURRING_ID)).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> recurringTransactionService.deleteRecurringTransaction(USER_ID, RECURRING_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied");

            verify(recurringRepository, never()).delete(any(Long.class), any(Long.class));
        }
    }

    @Nested
    class FindSuggestionsTest {

        @Test
        @DisplayName("should detect MONTHLY pattern and include suggestion with correct next date")
        void findSuggestions_monthlyPattern_includesSuggestionWithMonthlyFrequency() {
            // Arrange - 4 transactions ~30 days apart
            OffsetDateTime base = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            List<Transaction> transactions = buildTransactionGroup("Netflix", new BigDecimal("15.99"), base, 30, 4);

            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID)).thenReturn(List.of());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(transactions);

            // Act
            List<RecurringSuggestionDto> suggestions = recurringTransactionService.findSuggestions(USER_ID);

            // Assert
            assertThat(suggestions).hasSize(1);
            RecurringSuggestionDto suggestion = suggestions.get(0);
            assertThat(suggestion.frequency()).isEqualTo(Frequency.MONTHLY);
            assertThat(suggestion.amount()).isEqualByComparingTo(new BigDecimal("15.99"));
            assertThat(suggestion.merchant().cleanName()).isEqualTo("Netflix");
            assertThat(suggestion.occurrenceCount()).isEqualTo(4);
            // next date should be lastDate + 1 month
            OffsetDateTime expectedLastDate = base.plusDays(90); // 3rd interval of 30 days
            LocalDate expectedNextDate = expectedLastDate.toLocalDate().plusMonths(1);
            assertThat(suggestion.nextDate().toLocalDate()).isEqualTo(expectedNextDate);
        }

        @Test
        @DisplayName("should detect WEEKLY pattern with ~7-day intervals")
        void findSuggestions_weeklyPattern_includesSuggestionWithWeeklyFrequency() {
            // Arrange - 4 transactions 7 days apart
            OffsetDateTime base = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            List<Transaction> transactions = buildTransactionGroup("Gym", new BigDecimal("20.00"), base, 7, 4);

            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID)).thenReturn(List.of());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(transactions);

            // Act
            List<RecurringSuggestionDto> suggestions = recurringTransactionService.findSuggestions(USER_ID);

            // Assert
            assertThat(suggestions).hasSize(1);
            assertThat(suggestions.get(0).frequency()).isEqualTo(Frequency.WEEKLY);
            // next date = last date + 1 week
            LocalDate expectedNext = base.plusDays(21).toLocalDate().plusWeeks(1);
            assertThat(suggestions.get(0).nextDate().toLocalDate()).isEqualTo(expectedNext);
        }

        @Test
        @DisplayName("should detect BI_WEEKLY pattern with ~14-day intervals")
        void findSuggestions_biWeeklyPattern_includesSuggestionWithBiWeeklyFrequency() {
            // Arrange - 4 transactions 14 days apart
            OffsetDateTime base = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            List<Transaction> transactions = buildTransactionGroup("ClassPass", new BigDecimal("49.00"), base, 14, 4);

            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID)).thenReturn(List.of());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(transactions);

            // Act
            List<RecurringSuggestionDto> suggestions = recurringTransactionService.findSuggestions(USER_ID);

            // Assert
            assertThat(suggestions).hasSize(1);
            assertThat(suggestions.get(0).frequency()).isEqualTo(Frequency.BI_WEEKLY);
            LocalDate expectedNext = base.plusDays(42).toLocalDate().plusWeeks(2);
            assertThat(suggestions.get(0).nextDate().toLocalDate()).isEqualTo(expectedNext);
        }

        @Test
        @DisplayName("should detect YEARLY pattern with ~365-day intervals")
        void findSuggestions_yearlyPattern_includesSuggestionWithYearlyFrequency() {
            // Arrange - 3 transactions 365 days apart
            OffsetDateTime base = OffsetDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            List<Transaction> transactions = buildTransactionGroup("AnnualSubscription", new BigDecimal("99.00"), base, 365, 3);

            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID)).thenReturn(List.of());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(transactions);

            // Act
            List<RecurringSuggestionDto> suggestions = recurringTransactionService.findSuggestions(USER_ID);

            // Assert
            assertThat(suggestions).hasSize(1);
            assertThat(suggestions.get(0).frequency()).isEqualTo(Frequency.YEARLY);
            LocalDate expectedNext = base.plusDays(730).toLocalDate().plusYears(1);
            assertThat(suggestions.get(0).nextDate().toLocalDate()).isEqualTo(expectedNext);
        }

        @Test
        @DisplayName("should exclude groups with fewer than 3 occurrences")
        void findSuggestions_fewerThanThreeOccurrences_returnsNoSuggestions() {
            // Arrange - only 2 transactions
            OffsetDateTime base = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            List<Transaction> transactions = buildTransactionGroup("Netflix", new BigDecimal("15.99"), base, 30, 2);

            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID)).thenReturn(List.of());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(transactions);

            // Act
            List<RecurringSuggestionDto> suggestions = recurringTransactionService.findSuggestions(USER_ID);

            // Assert
            assertThat(suggestions).isEmpty();
        }

        @Test
        @DisplayName("should exclude groups with unstable intervals (variance >= 5 days)")
        void findSuggestions_unstableIntervals_returnsNoSuggestions() {
            // Arrange - intervals are 30, 30, 45 days: avg=35, one interval deviates by 10 -> unstable
            OffsetDateTime base = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            Merchant merchant = buildMerchant("Erratic");

            Transaction tx1 = new Transaction();
            tx1.setId(1L);
            tx1.setMerchant(merchant);
            tx1.setAmount(new BigDecimal("30.00"));
            tx1.setTransactionDate(base);
            tx1.setType(TransactionType.EXPENSE);

            Transaction tx2 = new Transaction();
            tx2.setId(2L);
            tx2.setMerchant(merchant);
            tx2.setAmount(new BigDecimal("30.00"));
            tx2.setTransactionDate(base.plusDays(30));
            tx2.setType(TransactionType.EXPENSE);

            Transaction tx3 = new Transaction();
            tx3.setId(3L);
            tx3.setMerchant(merchant);
            tx3.setAmount(new BigDecimal("30.00"));
            tx3.setTransactionDate(base.plusDays(60));
            tx3.setType(TransactionType.EXPENSE);

            // 4th transaction placed 45 days after 3rd: interval 45, deviates from avg ~35 by 10 > 5
            Transaction tx4 = new Transaction();
            tx4.setId(4L);
            tx4.setMerchant(merchant);
            tx4.setAmount(new BigDecimal("30.00"));
            tx4.setTransactionDate(base.plusDays(105));
            tx4.setType(TransactionType.EXPENSE);

            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID)).thenReturn(List.of());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(List.of(tx1, tx2, tx3, tx4));

            // Act
            List<RecurringSuggestionDto> suggestions = recurringTransactionService.findSuggestions(USER_ID);

            // Assert
            assertThat(suggestions).isEmpty();
        }

        @Test
        @DisplayName("should exclude merchants that already have an active recurring transaction")
        void findSuggestions_merchantAlreadyInRecurring_excludedFromSuggestions() {
            // Arrange - Netflix has an existing recurring; transactions from Netflix should be skipped
            OffsetDateTime base = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            List<Transaction> transactions = buildTransactionGroup("Netflix", new BigDecimal("15.99"), base, 30, 4);

            RecurringTransaction existingNetflix = buildRecurring(RECURRING_ID, USER_ID);
            existingNetflix.getMerchant().setName("Netflix");

            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID))
                    .thenReturn(List.of(existingNetflix));
            when(transactionRepository.findExpensesSince(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(transactions);

            // Act
            List<RecurringSuggestionDto> suggestions = recurringTransactionService.findSuggestions(USER_ID);

            // Assert
            assertThat(suggestions).isEmpty();
        }

        @Test
        @DisplayName("should return multiple suggestions sorted by confidence score descending")
        void findSuggestions_multipleGroups_sortedByConfidenceScoreDescending() {
            // Arrange - Netflix (5 occurrences, higher confidence) and Gym (3 occurrences, lower confidence)
            OffsetDateTime base = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            List<Transaction> netflixTxs = buildTransactionGroup("Netflix", new BigDecimal("15.99"), base, 30, 5);
            List<Transaction> gymTxs = buildTransactionGroup("Gym", new BigDecimal("50.00"), base, 30, 3);

            List<Transaction> allTxs = new ArrayList<>();
            allTxs.addAll(netflixTxs);
            allTxs.addAll(gymTxs);

            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID)).thenReturn(List.of());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(allTxs);

            // Act
            List<RecurringSuggestionDto> suggestions = recurringTransactionService.findSuggestions(USER_ID);

            // Assert
            assertThat(suggestions).hasSize(2);
            // Sorted descending by confidence: 5 occurrences -> 0.8 + 0.25 = 1.05; 3 occurrences -> 0.8 + 0.15 = 0.95
            assertThat(suggestions.get(0).occurrenceCount()).isGreaterThan(suggestions.get(1).occurrenceCount());
            assertThat(suggestions.get(0).confidenceScore()).isGreaterThan(suggestions.get(1).confidenceScore());
        }

        @Test
        @DisplayName("should calculate confidence score as 0.8 + (occurrenceCount * 0.05)")
        void findSuggestions_confidenceScoreCalculation_isCorrect() {
            // Arrange - 3 occurrences -> confidence = 0.8 + 3*0.05 = 0.95
            OffsetDateTime base = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            List<Transaction> transactions = buildTransactionGroup("Netflix", new BigDecimal("15.99"), base, 30, 3);

            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID)).thenReturn(List.of());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(transactions);

            // Act
            List<RecurringSuggestionDto> suggestions = recurringTransactionService.findSuggestions(USER_ID);

            // Assert
            assertThat(suggestions).hasSize(1);
            assertThat(suggestions.get(0).confidenceScore()).isEqualTo(0.8 + (3 * 0.05));
        }

        // QUARTERLY is excluded because detectFrequency() has no detection range for it;
        // calculateNextDate is unreachable via findSuggestions for QUARTERLY.
        static Stream<Arguments> frequencyNextDateTestCases() {
            return Stream.of(
                    Arguments.of(Frequency.MONTHLY, 30),
                    Arguments.of(Frequency.WEEKLY, 7),
                    Arguments.of(Frequency.BI_WEEKLY, 14),
                    Arguments.of(Frequency.YEARLY, 365)
            );
        }

        @ParameterizedTest(name = "{0}: intervalDays={1}")
        @MethodSource("frequencyNextDateTestCases")
        @DisplayName("should produce correct next date for each detectable Frequency")
        void findSuggestions_calculateNextDate_correctForAllFrequencies(
                Frequency frequency, int intervalDays) {
            // Arrange
            OffsetDateTime baseDateTime = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            List<Transaction> transactions = buildTransactionGroup("TestMerchant", new BigDecimal("10.00"),
                    baseDateTime, intervalDays, 4);

            when(recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(USER_ID)).thenReturn(List.of());
            when(transactionRepository.findExpensesSince(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(transactions);

            // Act
            List<RecurringSuggestionDto> suggestions = recurringTransactionService.findSuggestions(USER_ID);

            // Assert
            assertThat(suggestions).isNotEmpty();
            assertThat(suggestions.get(0).frequency()).isEqualTo(frequency);
            // Verify: the last transaction date is baseDate + 3*interval (4 txs, 0-indexed)
            LocalDate expectedLastDate = baseDateTime.plusDays((long) intervalDays * 3).toLocalDate();
            assertThat(suggestions.get(0).lastDate().toLocalDate()).isEqualTo(expectedLastDate);
            // Verify the next date using the expected offset for the frequency
            LocalDate expectedNextDate = switch (frequency) {
                case MONTHLY -> expectedLastDate.plusMonths(1);
                case WEEKLY -> expectedLastDate.plusWeeks(1);
                case BI_WEEKLY -> expectedLastDate.plusWeeks(2);
                case YEARLY -> expectedLastDate.plusYears(1);
                default -> expectedLastDate;
            };
            assertThat(suggestions.get(0).nextDate().toLocalDate()).isEqualTo(expectedNextDate);
        }
    }
}
