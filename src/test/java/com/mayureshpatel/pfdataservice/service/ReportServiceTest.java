package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.dto.report.CategoryReportDataDto;
import com.mayureshpatel.pfdataservice.dto.report.MerchantReportDataDto;
import com.mayureshpatel.pfdataservice.dto.report.MonthlyReportDataDto;
import com.mayureshpatel.pfdataservice.dto.report.NetWorthDataPointDto;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService Unit Tests")
class ReportServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SnapshotService snapshotService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private ReportService reportService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("getNetWorthOverTime")
    class GetNetWorthOverTimeTests {

        @Test
        @DisplayName("should sum every account's computed balance for a single-month range")
        void shouldSumAcrossAccountsForOneMonth() {
            // arrange
            Account checking = Account.builder().id(1L).userId(USER_ID).build();
            Account savings = Account.builder().id(2L).userId(USER_ID).build();
            when(accountRepository.findAllByUserId(USER_ID)).thenReturn(List.of(checking, savings));

            LocalDate monthEnd = LocalDate.of(2026, 3, 31);
            when(snapshotService.calculateEndOfMonthBalance(eq(checking), eq(monthEnd))).thenReturn(new BigDecimal("500.00"));
            when(snapshotService.calculateEndOfMonthBalance(eq(savings), eq(monthEnd))).thenReturn(new BigDecimal("1500.00"));

            // act
            List<NetWorthDataPointDto> result = reportService.getNetWorthOverTime(
                    USER_ID, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15));

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(monthEnd, result.get(0).date());
            assertEquals(0, result.get(0).netWorth().compareTo(new BigDecimal("2000.00")));
        }

        @Test
        @DisplayName("should return one data point per month, oldest first, across a multi-month range spanning a year boundary")
        void shouldReturnOnePointPerMonthAcrossYearBoundary() {
            // arrange
            Account checking = Account.builder().id(1L).userId(USER_ID).build();
            when(accountRepository.findAllByUserId(USER_ID)).thenReturn(List.of(checking));
            when(snapshotService.calculateEndOfMonthBalance(any(), any())).thenReturn(BigDecimal.TEN);

            // act -- Nov 2025 through Jan 2026
            List<NetWorthDataPointDto> result = reportService.getNetWorthOverTime(
                    USER_ID, LocalDate.of(2025, 11, 5), LocalDate.of(2026, 1, 20));

            // assert & verify
            assertEquals(3, result.size());
            assertEquals(LocalDate.of(2025, 11, 30), result.get(0).date());
            assertEquals(LocalDate.of(2025, 12, 31), result.get(1).date());
            assertEquals(LocalDate.of(2026, 1, 31), result.get(2).date());
        }

        @Test
        @DisplayName("should still return a zero-net-worth point for a user with no accounts")
        void shouldReturnZeroPointsWhenNoAccounts() {
            // arrange
            when(accountRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

            // act
            List<NetWorthDataPointDto> result = reportService.getNetWorthOverTime(
                    USER_ID, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1));

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(0, result.get(0).netWorth().compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("should exclude an account from month-ends before it was created")
        void shouldExcludeAccountBeforeItExisted() {
            // arrange -- account created mid-December 2025
            Account lateAccount = Account.builder()
                    .id(1L).userId(USER_ID)
                    .audit(TableAudit.builder().createdAt(OffsetDateTime.parse("2025-12-15T00:00:00Z")).build())
                    .build();
            when(accountRepository.findAllByUserId(USER_ID)).thenReturn(List.of(lateAccount));
            when(snapshotService.calculateEndOfMonthBalance(eq(lateAccount), eq(LocalDate.of(2025, 12, 31))))
                    .thenReturn(new BigDecimal("300.00"));

            // act -- November (before creation) through December (after creation)
            List<NetWorthDataPointDto> result = reportService.getNetWorthOverTime(
                    USER_ID, LocalDate.of(2025, 11, 1), LocalDate.of(2025, 12, 1));

            // assert & verify
            assertEquals(2, result.size());
            assertEquals(0, result.get(0).netWorth().compareTo(BigDecimal.ZERO)); // November: account didn't exist yet
            assertEquals(0, result.get(1).netWorth().compareTo(new BigDecimal("300.00"))); // December: it did
        }

        @Test
        @DisplayName("should include an account created exactly on the month-end date itself")
        void shouldIncludeAccountCreatedOnMonthEndItself() {
            // arrange
            Account account = Account.builder()
                    .id(1L).userId(USER_ID)
                    .audit(TableAudit.builder().createdAt(OffsetDateTime.parse("2026-03-31T23:59:00Z")).build())
                    .build();
            when(accountRepository.findAllByUserId(USER_ID)).thenReturn(List.of(account));
            when(snapshotService.calculateEndOfMonthBalance(eq(account), eq(LocalDate.of(2026, 3, 31))))
                    .thenReturn(new BigDecimal("50.00"));

            // act
            List<NetWorthDataPointDto> result = reportService.getNetWorthOverTime(
                    USER_ID, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1));

            // assert & verify
            assertEquals(0, result.get(0).netWorth().compareTo(new BigDecimal("50.00")));
        }
    }

    @Nested
    @DisplayName("getCategoryReportData / getMerchantReportData / getMonthlyReportData (PF-823)")
    class ReportDataDateBoundaryTests {

        private final ArgumentCaptor<OffsetDateTime> startCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        private final ArgumentCaptor<OffsetDateTime> endCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);

        @Test
        @DisplayName("getCategoryReportData anchors the caller-inclusive end date to UTC midnight of the *next* day")
        void shouldAnchorCategoryReportDateBoundsHalfOpen() {
            // arrange
            when(transactionRepository.findCategoryReportData(eq(USER_ID), any(), any())).thenReturn(List.of());

            // act
            reportService.getCategoryReportData(USER_ID, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

            // assert & verify
            verify(transactionRepository).findCategoryReportData(eq(USER_ID), startCaptor.capture(), endCaptor.capture());
            assertEquals(OffsetDateTime.parse("2026-03-01T00:00:00Z"), startCaptor.getValue());
            assertEquals(OffsetDateTime.parse("2026-04-01T00:00:00Z"), endCaptor.getValue(),
                    "the caller's inclusive endDate (3/31) must become the exclusive bound 4/1, "
                            + "not 3/31 itself -- otherwise same-day activity on 3/31 would be dropped");
        }

        @Test
        @DisplayName("getMerchantReportData anchors the caller-inclusive end date to UTC midnight of the *next* day")
        void shouldAnchorMerchantReportDateBoundsHalfOpen() {
            // arrange
            when(merchantRepository.findMerchantReportData(eq(USER_ID), any(), any())).thenReturn(List.of());

            // act
            reportService.getMerchantReportData(USER_ID, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

            // assert & verify
            verify(merchantRepository).findMerchantReportData(eq(USER_ID), startCaptor.capture(), endCaptor.capture());
            assertEquals(OffsetDateTime.parse("2026-03-01T00:00:00Z"), startCaptor.getValue());
            assertEquals(OffsetDateTime.parse("2026-04-01T00:00:00Z"), endCaptor.getValue());
        }

        @Test
        @DisplayName("getMonthlyReportData anchors the caller-inclusive end date to UTC midnight of the *next* day")
        void shouldAnchorMonthlyReportDateBoundsHalfOpen() {
            // arrange
            when(transactionRepository.findMonthlyIncomeExpense(eq(USER_ID), any(), any())).thenReturn(List.of());

            // act
            reportService.getMonthlyReportData(USER_ID, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

            // assert & verify
            verify(transactionRepository).findMonthlyIncomeExpense(eq(USER_ID), startCaptor.capture(), endCaptor.capture());
            assertEquals(OffsetDateTime.parse("2026-03-01T00:00:00Z"), startCaptor.getValue());
            assertEquals(OffsetDateTime.parse("2026-04-01T00:00:00Z"), endCaptor.getValue());
        }

        @Test
        @DisplayName("getCategoryReportData passes each repository row straight through unmodified")
        void shouldPassThroughCategoryReportRows() {
            // arrange
            List<CategoryReportDataDto> rows = List.of(new CategoryReportDataDto(null, new BigDecimal("42.00"), 3L));
            when(transactionRepository.findCategoryReportData(eq(USER_ID), any(), any())).thenReturn(rows);

            // act
            List<CategoryReportDataDto> result = reportService.getCategoryReportData(
                    USER_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

            // assert & verify
            assertEquals(rows, result);
        }

        @Test
        @DisplayName("getMerchantReportData passes each repository row straight through unmodified")
        void shouldPassThroughMerchantReportRows() {
            // arrange
            List<MerchantReportDataDto> rows = List.of(
                    new MerchantReportDataDto(1L, "Whole Foods", new BigDecimal("42.00"), 3L, List.of("Groceries")));
            when(merchantRepository.findMerchantReportData(eq(USER_ID), any(), any())).thenReturn(rows);

            // act
            List<MerchantReportDataDto> result = reportService.getMerchantReportData(
                    USER_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

            // assert & verify
            assertEquals(rows, result);
        }

        @Test
        @DisplayName("getMonthlyReportData passes each repository row straight through unmodified")
        void shouldPassThroughMonthlyReportRows() {
            // arrange
            List<MonthlyReportDataDto> rows = List.of(
                    new MonthlyReportDataDto(2026, 1, new BigDecimal("100.00"), new BigDecimal("60.00")));
            when(transactionRepository.findMonthlyIncomeExpense(eq(USER_ID), any(), any())).thenReturn(rows);

            // act
            List<MonthlyReportDataDto> result = reportService.getMonthlyReportData(
                    USER_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

            // assert & verify
            assertEquals(rows, result);
        }
    }
}
