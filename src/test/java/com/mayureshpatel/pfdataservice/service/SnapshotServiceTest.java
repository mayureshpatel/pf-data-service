package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountSnapshot;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.account.AccountSnapshotRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SnapshotService unit tests")
class SnapshotServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountSnapshotRepository snapshotRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private SnapshotService snapshotService;

    private static final Long ACCOUNT_ID = 42L;

    private Account buildAccount(Long id, BigDecimal currentBalance) {
        Account account = new Account();
        account.setId(id);
        account.setCurrentBalance(currentBalance);
        return account;
    }

    @Nested
    @DisplayName("createEndOfMonthSnapshot")
    class CreateEndOfMonthSnapshotTest {

        @Test
        @DisplayName("should throw IllegalArgumentException when account is not found")
        void createEndOfMonthSnapshot_accountNotFound_throwsIllegalArgumentException() {
            LocalDate dateInMonth = LocalDate.of(2025, 3, 15);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> snapshotService.createEndOfMonthSnapshot(ACCOUNT_ID, dateInMonth))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(ACCOUNT_ID));

            verify(snapshotRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create a new snapshot when none exists for that end-of-month date")
        void createEndOfMonthSnapshot_noExistingSnapshot_savesNewSnapshot() {
            LocalDate dateInMonth = LocalDate.of(2025, 3, 10);
            LocalDate expectedEndOfMonth = LocalDate.of(2025, 3, 31);

            BigDecimal currentBalance = new BigDecimal("1500.00");
            BigDecimal netFlowAfter = new BigDecimal("200.00");
            BigDecimal expectedHistoric = new BigDecimal("1300.00");

            Account account = buildAccount(ACCOUNT_ID, currentBalance);

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.getNetFlowAfterDate(ACCOUNT_ID, expectedEndOfMonth)).thenReturn(netFlowAfter);
            when(snapshotRepository.findByAccountIdAndSnapshotDate(ACCOUNT_ID, expectedEndOfMonth)).thenReturn(Optional.empty());
            when(snapshotRepository.save(any(AccountSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

            snapshotService.createEndOfMonthSnapshot(ACCOUNT_ID, dateInMonth);

            ArgumentCaptor<AccountSnapshot> captor = ArgumentCaptor.forClass(AccountSnapshot.class);
            verify(snapshotRepository).save(captor.capture());
            AccountSnapshot saved = captor.getValue();

            assertThat(saved.getId()).isNull();
            assertThat(saved.getAccount()).isSameAs(account);
            assertThat(saved.getBalance()).isEqualByComparingTo(expectedHistoric);
            assertThat(saved.getSnapshotDate())
                    .isEqualTo(expectedEndOfMonth.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime());
        }

        @Test
        @DisplayName("should update the existing snapshot when one already exists for that end-of-month date")
        void createEndOfMonthSnapshot_existingSnapshot_updatesExistingSnapshot() {
            LocalDate dateInMonth = LocalDate.of(2025, 1, 31);
            LocalDate expectedEndOfMonth = LocalDate.of(2025, 1, 31);

            BigDecimal currentBalance = new BigDecimal("2000.00");
            BigDecimal netFlowAfter = new BigDecimal("500.00");
            BigDecimal expectedHistoric = new BigDecimal("1500.00");

            Account account = buildAccount(ACCOUNT_ID, currentBalance);
            AccountSnapshot existing = new AccountSnapshot();
            existing.setId(77L);

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.getNetFlowAfterDate(ACCOUNT_ID, expectedEndOfMonth)).thenReturn(netFlowAfter);
            when(snapshotRepository.findByAccountIdAndSnapshotDate(ACCOUNT_ID, expectedEndOfMonth)).thenReturn(Optional.of(existing));
            when(snapshotRepository.save(any(AccountSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

            snapshotService.createEndOfMonthSnapshot(ACCOUNT_ID, dateInMonth);

            ArgumentCaptor<AccountSnapshot> captor = ArgumentCaptor.forClass(AccountSnapshot.class);
            verify(snapshotRepository).save(captor.capture());
            AccountSnapshot saved = captor.getValue();

            assertThat(saved.getId()).isEqualTo(77L);
            assertThat(saved.getBalance()).isEqualByComparingTo(expectedHistoric);
            assertThat(saved.getAccount()).isSameAs(account);
            assertThat(saved.getSnapshotDate().toLocalDate()).isEqualTo(expectedEndOfMonth);
        }

        @Test
        @DisplayName("should treat null net-flow as zero when computing historic balance")
        void createEndOfMonthSnapshot_nullNetFlow_treatedAsZero() {
            LocalDate dateInMonth = LocalDate.of(2025, 6, 1);
            LocalDate expectedEndOfMonth = LocalDate.of(2025, 6, 30);

            BigDecimal currentBalance = new BigDecimal("800.00");
            Account account = buildAccount(ACCOUNT_ID, currentBalance);

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.getNetFlowAfterDate(ACCOUNT_ID, expectedEndOfMonth)).thenReturn(null);
            when(snapshotRepository.findByAccountIdAndSnapshotDate(ACCOUNT_ID, expectedEndOfMonth)).thenReturn(Optional.empty());
            when(snapshotRepository.save(any(AccountSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

            snapshotService.createEndOfMonthSnapshot(ACCOUNT_ID, dateInMonth);

            ArgumentCaptor<AccountSnapshot> captor = ArgumentCaptor.forClass(AccountSnapshot.class);
            verify(snapshotRepository).save(captor.capture());
            assertThat(captor.getValue().getBalance()).isEqualByComparingTo(new BigDecimal("800.00"));
        }

        @Test
        @DisplayName("should compute correct historic balance when net-flow is negative")
        void createEndOfMonthSnapshot_negativeNetFlow_addsBackToComputeHistoric() {
            LocalDate dateInMonth = LocalDate.of(2025, 4, 15);
            LocalDate expectedEndOfMonth = LocalDate.of(2025, 4, 30);

            BigDecimal currentBalance = new BigDecimal("600.00");
            BigDecimal netFlowAfter = new BigDecimal("-300.00");
            BigDecimal expectedHistoric = new BigDecimal("900.00"); // 600 - (-300) = 900

            Account account = buildAccount(ACCOUNT_ID, currentBalance);

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.getNetFlowAfterDate(ACCOUNT_ID, expectedEndOfMonth)).thenReturn(netFlowAfter);
            when(snapshotRepository.findByAccountIdAndSnapshotDate(ACCOUNT_ID, expectedEndOfMonth)).thenReturn(Optional.empty());
            when(snapshotRepository.save(any(AccountSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

            snapshotService.createEndOfMonthSnapshot(ACCOUNT_ID, dateInMonth);

            ArgumentCaptor<AccountSnapshot> captor = ArgumentCaptor.forClass(AccountSnapshot.class);
            verify(snapshotRepository).save(captor.capture());
            assertThat(captor.getValue().getBalance()).isEqualByComparingTo(expectedHistoric);
        }

        @Test
        @DisplayName("should store the snapshotDate in UTC at midnight")
        void createEndOfMonthSnapshot_snapshotDateStoredInUtcAtMidnight() {
            LocalDate dateInMonth = LocalDate.of(2025, 2, 14);
            LocalDate expectedEndOfMonth = LocalDate.of(2025, 2, 28);
            Account account = buildAccount(ACCOUNT_ID, new BigDecimal("1000.00"));

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.getNetFlowAfterDate(ACCOUNT_ID, expectedEndOfMonth)).thenReturn(BigDecimal.ZERO);
            when(snapshotRepository.findByAccountIdAndSnapshotDate(ACCOUNT_ID, expectedEndOfMonth)).thenReturn(Optional.empty());
            when(snapshotRepository.save(any(AccountSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

            snapshotService.createEndOfMonthSnapshot(ACCOUNT_ID, dateInMonth);

            ArgumentCaptor<AccountSnapshot> captor = ArgumentCaptor.forClass(AccountSnapshot.class);
            verify(snapshotRepository).save(captor.capture());

            assertThat(captor.getValue().getSnapshotDate().getOffset()).isEqualTo(ZoneOffset.UTC);
            assertThat(captor.getValue().getSnapshotDate().toLocalDate()).isEqualTo(expectedEndOfMonth);
            assertThat(captor.getValue().getSnapshotDate().getHour()).isZero();
            assertThat(captor.getValue().getSnapshotDate().getMinute()).isZero();
            assertThat(captor.getValue().getSnapshotDate().getSecond()).isZero();
        }

        @ParameterizedTest(name = "dateInMonth={0} should compute endOfMonth={1}")
        @CsvSource({
                "2025-01-01, 2025-01-31",
                "2025-01-31, 2025-01-31",
                "2025-01-15, 2025-01-31",
                "2025-02-14, 2025-02-28",
                "2024-02-10, 2024-02-29",
                "2025-04-05, 2025-04-30",
                "2025-12-25, 2025-12-31"
        })
        @DisplayName("should correctly compute end-of-month from any day in the month")
        void createEndOfMonthSnapshot_variousDaysInMonth_computesCorrectEndOfMonth(
                String dateInMonthStr, String expectedEndStr) {
            LocalDate dateInMonth = LocalDate.parse(dateInMonthStr);
            LocalDate expectedEndOfMonth = LocalDate.parse(expectedEndStr);
            Account account = buildAccount(ACCOUNT_ID, new BigDecimal("500.00"));

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(transactionRepository.getNetFlowAfterDate(ACCOUNT_ID, expectedEndOfMonth)).thenReturn(BigDecimal.ZERO);
            when(snapshotRepository.findByAccountIdAndSnapshotDate(ACCOUNT_ID, expectedEndOfMonth)).thenReturn(Optional.empty());
            when(snapshotRepository.save(any(AccountSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

            snapshotService.createEndOfMonthSnapshot(ACCOUNT_ID, dateInMonth);

            verify(transactionRepository).getNetFlowAfterDate(ACCOUNT_ID, expectedEndOfMonth);
            verify(snapshotRepository).findByAccountIdAndSnapshotDate(ACCOUNT_ID, expectedEndOfMonth);
        }
    }
}
