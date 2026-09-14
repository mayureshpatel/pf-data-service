package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.dto.report.NetWorthDataPointDto;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService Unit Tests")
class ReportServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SnapshotService snapshotService;

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
}
