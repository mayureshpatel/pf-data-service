package com.mayureshpatel.pfdataservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@code NetWorthBalances}: the month ends of a range, and the balance of an account at a month end from its current balance and later monthly net flows. */
@DisplayName("NetWorthBalances Unit Tests")
class NetWorthBalancesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);
    private static final BigDecimal CURRENT = new BigDecimal("1000");

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    @Test
    @DisplayName("should list every month end from the first to the last month")
    void shouldListEveryMonthEndFromTheFirstToTheLastMonth() {
        // act
        List<LocalDate> ends = NetWorthBalances.monthEnds(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 4, 2));

        // assert & verify
        assertEquals(List.of(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30)), ends);
    }

    @Test
    @DisplayName("should include a leap day month end")
    void shouldIncludeALeapDayMonthEnd() {
        // act
        List<LocalDate> ends = NetWorthBalances.monthEnds(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 20));

        // assert & verify
        assertEquals(List.of(LocalDate.of(2024, 2, 29)), ends);
    }

    @Test
    @DisplayName("should use the current balance for a month end on or after today")
    void shouldUseTheCurrentBalanceForAMonthEndOnOrAfterToday() {
        // arrange
        Map<YearMonth, BigDecimal> flows = Map.of(YearMonth.of(2026, 10), money("50"), YearMonth.of(2026, 11), money("999"));

        // act
        BigDecimal balance = NetWorthBalances.balanceAt(CURRENT, flows, LocalDate.of(2026, 10, 31), TODAY);

        // assert & verify
        assertEquals(0, CURRENT.compareTo(balance));
    }

    @Test
    @DisplayName("should subtract every later month's net flow for a past month end")
    void shouldSubtractEveryLaterMonthsNetFlowForAPastMonthEnd() {
        // arrange
        Map<YearMonth, BigDecimal> flows = Map.of(
                YearMonth.of(2026, 8), money("10"),
                YearMonth.of(2026, 9), money("-30"),
                YearMonth.of(2026, 10), money("100"));

        // act
        BigDecimal atAugust = NetWorthBalances.balanceAt(CURRENT, flows, LocalDate.of(2026, 8, 31), TODAY);
        BigDecimal atSeptember = NetWorthBalances.balanceAt(CURRENT, flows, LocalDate.of(2026, 9, 30), TODAY);

        // assert & verify
        assertEquals(0, money("930").compareTo(atAugust));
        assertEquals(0, money("900").compareTo(atSeptember));
    }

    @Test
    @DisplayName("should treat a transaction on the last day of a month as inside that month")
    void shouldTreatATransactionOnTheLastDayOfAMonthAsInsideThatMonth() {
        // arrange -- flows are keyed by the month they happened in, so a last-day flow is in august, not after it
        Map<YearMonth, BigDecimal> flows = Map.of(YearMonth.of(2026, 8), money("25"), YearMonth.of(2026, 9), money("5"));

        // act
        BigDecimal atAugust = NetWorthBalances.balanceAt(money("100"), flows, LocalDate.of(2026, 8, 31), TODAY);

        // assert & verify
        assertEquals(0, money("95").compareTo(atAugust));
    }

    @Test
    @DisplayName("should not let a future-dated flow move today's balance")
    void shouldNotLetAFutureDatedFlowMoveTodaysBalance() {
        // arrange
        Map<YearMonth, BigDecimal> flows = Map.of(YearMonth.of(2026, 12), money("400"));

        // act
        BigDecimal atToday = NetWorthBalances.balanceAt(CURRENT, flows, LocalDate.of(2026, 10, 31), TODAY);

        // assert & verify
        assertEquals(0, CURRENT.compareTo(atToday));
    }

    @Test
    @DisplayName("should treat an account with no creation date as always existing")
    void shouldTreatAnAccountWithNoCreationDateAsAlwaysExisting() {
        // act & assert & verify
        assertTrue(NetWorthBalances.existedAsOf(null, LocalDate.of(2020, 1, 31)));
    }

    @Test
    @DisplayName("should say an account created mid-month did not exist at the previous month end")
    void shouldSayAnAccountCreatedMidMonthDidNotExistAtThePreviousMonthEnd() {
        // arrange
        OffsetDateTime created = OffsetDateTime.of(2026, 5, 14, 9, 0, 0, 0, ZoneOffset.UTC);

        // act & assert & verify
        assertFalse(NetWorthBalances.existedAsOf(created, LocalDate.of(2026, 4, 30)));
        assertTrue(NetWorthBalances.existedAsOf(created, LocalDate.of(2026, 5, 31)));
    }
}
