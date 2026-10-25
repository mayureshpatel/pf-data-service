package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.transaction.RecurringCharge;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.service.RecurringProjection.Projection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@code RecurringProjection}: how a tracked item's stored schedule and its newest matched real charges become a last charge, a rolled-forward next date, an overdue flag and a price change. */
@DisplayName("RecurringProjection Unit Tests")
class RecurringProjectionTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);
    private static final String STORED_AMOUNT = "10.00";

    private static RecurringTransaction item(String frequency, LocalDate nextDate) {
        return RecurringTransaction.builder()
                .frequency(frequency)
                .nextDate(nextDate)
                .amount(new BigDecimal(STORED_AMOUNT))
                .build();
    }

    private static RecurringCharge charge(LocalDate date, String amount) {
        return new RecurringCharge(date, new BigDecimal(amount));
    }

    /** With no matched charges the stored schedule stands, and a stored date in the past is overdue. */
    @Nested
    @DisplayName("without matched charges")
    class WithoutChargesTests {

        @Test
        @DisplayName("should use the stored next date when nothing matched")
        void shouldUseStoredNextDate() {
            // act
            Projection result = RecurringProjection.project(item("MONTHLY", TODAY.plusDays(5)), List.of(), TODAY);

            // assert & verify
            assertEquals(TODAY.plusDays(5), result.projectedNextDate());
            assertFalse(result.overdue());
            assertNull(result.lastChargeDate());
            assertNull(result.lastChargeAmount());
            assertNull(result.priceChange());
        }

        @Test
        @DisplayName("should flag a stored next date in the past as overdue")
        void shouldFlagPastStoredDate() {
            // act
            Projection result = RecurringProjection.project(item("MONTHLY", TODAY.minusDays(3)), List.of(), TODAY);

            // assert & verify
            assertTrue(result.overdue());
            assertEquals(TODAY.minusDays(3), result.projectedNextDate());
        }
    }

    /** With matched charges the next date rolls forward from the newest one by the item's own frequency. */
    @Nested
    @DisplayName("rolling forward from the last charge")
    class RollForwardTests {

        private final LocalDate last = TODAY.minusDays(2);

        private LocalDate rolled(String frequency) {
            return RecurringProjection.project(item(frequency, TODAY.minusYears(1)), List.of(charge(last, STORED_AMOUNT)), TODAY)
                    .projectedNextDate();
        }

        @Test
        @DisplayName("should roll forward by each frequency")
        void shouldRollByFrequency() {
            // act & assert & verify
            assertEquals(last.plusWeeks(1), rolled("WEEKLY"));
            assertEquals(last.plusWeeks(2), rolled("BI_WEEKLY"));
            assertEquals(last.plusMonths(1), rolled("MONTHLY"));
            assertEquals(last.plusMonths(3), rolled("QUARTERLY"));
            assertEquals(last.plusYears(1), rolled("YEARLY"));
        }

        @Test
        @DisplayName("should report the last charge date and amount")
        void shouldReportLastCharge() {
            // act
            Projection result = RecurringProjection.project(item("MONTHLY", TODAY), List.of(charge(last, STORED_AMOUNT)), TODAY);

            // assert & verify
            assertEquals(last, result.lastChargeDate());
            assertEquals(0, new BigDecimal(STORED_AMOUNT).compareTo(result.lastChargeAmount()));
            assertFalse(result.overdue());
        }

        @Test
        @DisplayName("should flag a rolled date in the past as overdue")
        void shouldFlagOverdueAfterRolling() {
            // act
            Projection result = RecurringProjection.project(
                    item("MONTHLY", TODAY), List.of(charge(TODAY.minusMonths(1).minusDays(4), STORED_AMOUNT)), TODAY);

            // assert & verify
            assertTrue(result.overdue());
            assertEquals(TODAY.minusDays(4), result.projectedNextDate());
        }
    }

    /** A price change is the newest charge differing from the stored amount, dated by the start of the unbroken run at the new price. */
    @Nested
    @DisplayName("price change")
    class PriceChangeTests {

        @Test
        @DisplayName("should report no price change when the newest charge equals the stored amount")
        void shouldReportNoChangeWhenEqual() {
            // act
            Projection result = RecurringProjection.project(
                    item("MONTHLY", TODAY), List.of(charge(TODAY.minusDays(1), "10.0")), TODAY);

            // assert & verify
            assertNull(result.priceChange());
        }

        @Test
        @DisplayName("should report the price change with the first date at the new amount")
        void shouldReportChangeWithSince() {
            // arrange -- newest first: two charges at 12, before them a charge at 10
            List<RecurringCharge> charges = List.of(
                    charge(TODAY.minusDays(2), "12.00"),
                    charge(TODAY.minusDays(32), "12.00"),
                    charge(TODAY.minusDays(62), "10.00"));

            // act
            Projection result = RecurringProjection.project(item("MONTHLY", TODAY), charges, TODAY);

            // assert & verify
            assertNotNull(result.priceChange());
            assertEquals(0, new BigDecimal(STORED_AMOUNT).compareTo(result.priceChange().previousAmount()));
            assertEquals(0, new BigDecimal("12.00").compareTo(result.priceChange().newAmount()));
            assertEquals(TODAY.minusDays(32), result.priceChange().since());
        }

        @Test
        @DisplayName("should find the start of the run when the price changed twice")
        void shouldFindRunStartAfterTwoChanges() {
            // arrange -- stored 10, charges went 10 -> 11 -> 12
            List<RecurringCharge> charges = List.of(
                    charge(TODAY.minusDays(2), "12.00"),
                    charge(TODAY.minusDays(32), "11.00"),
                    charge(TODAY.minusDays(62), "10.00"));

            // act
            Projection result = RecurringProjection.project(item("MONTHLY", TODAY), charges, TODAY);

            // assert & verify
            assertEquals(TODAY.minusDays(2), result.priceChange().since());
            assertEquals(0, new BigDecimal("12.00").compareTo(result.priceChange().newAmount()));
        }
    }
}
