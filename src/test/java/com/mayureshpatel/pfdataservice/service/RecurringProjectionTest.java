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
    private static final String NEW_AMOUNT = "12.00";
    private static final String MONTHLY = "MONTHLY";
    private static final String WEEKLY = "WEEKLY";
    private static final String YEARLY = "YEARLY";
    private static final String UNRELATED = "54.99";
    private static final String MID_AMOUNT = "11.00";
    private static final String HIGH_AMOUNT = "15.00";

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
            Projection result = RecurringProjection.project(item(MONTHLY, TODAY.plusDays(5)), List.of(), TODAY);

            // assert & verify
            assertEquals(TODAY.plusDays(5), result.projectedNextDate());
            assertFalse(result.overdue());
            assertNull(result.lastChargeDate());
            assertNull(result.lastChargeAmount());
            assertNull(result.priceChange());
        }

        @Test
        @DisplayName("should flag a stored next date well in the past as overdue")
        void shouldFlagPastStoredDate() {
            // act
            Projection result = RecurringProjection.project(item(MONTHLY, TODAY.minusDays(10)), List.of(), TODAY);

            // assert & verify
            assertTrue(result.overdue());
            assertEquals(TODAY.minusDays(10), result.projectedNextDate());
        }

        @Test
        @DisplayName("should give a late statement a grace period before flagging it overdue")
        void shouldGiveGracePeriod() {
            // act
            Projection justLate = RecurringProjection.project(item(MONTHLY, TODAY.minusDays(7)), List.of(), TODAY);
            Projection weeklyLate = RecurringProjection.project(item(WEEKLY, TODAY.minusDays(3)), List.of(), TODAY);

            // assert & verify -- seven days for a monthly charge, two for a weekly one
            assertFalse(justLate.overdue());
            assertTrue(weeklyLate.overdue());
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
            assertEquals(last.plusWeeks(1), rolled(WEEKLY));
            assertEquals(last.plusWeeks(2), rolled("BI_WEEKLY"));
            assertEquals(last.plusMonths(1), rolled(MONTHLY));
            assertEquals(last.plusMonths(3), rolled("QUARTERLY"));
            assertEquals(last.plusYears(1), rolled(YEARLY));
        }

        @Test
        @DisplayName("should report the last charge date and amount")
        void shouldReportLastCharge() {
            // act
            Projection result = RecurringProjection.project(item(MONTHLY, TODAY), List.of(charge(last, STORED_AMOUNT)), TODAY);

            // assert & verify
            assertEquals(last, result.lastChargeDate());
            assertEquals(0, new BigDecimal(STORED_AMOUNT).compareTo(result.lastChargeAmount()));
            assertFalse(result.overdue());
        }

        @Test
        @DisplayName("should keep a stored next date that is later than the rolled one: the user pushed it out")
        void shouldKeepALaterStoredDate() {
            // act
            Projection result = RecurringProjection.project(
                    item(MONTHLY, TODAY.plusDays(40)), List.of(charge(last, STORED_AMOUNT)), TODAY);

            // assert & verify
            assertEquals(TODAY.plusDays(40), result.projectedNextDate());
            assertFalse(result.overdue());
        }

        @Test
        @DisplayName("should prefer the rolled date over a stored one the real charges have overtaken")
        void shouldPreferRolledOverStaleStoredDate() {
            // act
            Projection result = RecurringProjection.project(
                    item(MONTHLY, TODAY.minusYears(1)), List.of(charge(last, STORED_AMOUNT)), TODAY);

            // assert & verify
            assertEquals(last.plusMonths(1), result.projectedNextDate());
        }

        @Test
        @DisplayName("should flag a rolled date in the past as overdue")
        void shouldFlagOverdueAfterRolling() {
            // act
            Projection result = RecurringProjection.project(
                    item(MONTHLY, TODAY.minusMonths(3)), List.of(charge(TODAY.minusMonths(1).minusDays(12), STORED_AMOUNT)), TODAY);

            // assert & verify
            assertTrue(result.overdue());
            assertEquals(TODAY.minusDays(12), result.projectedNextDate());
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
                    item(MONTHLY, TODAY), List.of(charge(TODAY.minusDays(1), "10.0")), TODAY);

            // assert & verify
            assertNull(result.priceChange());
        }

        @Test
        @DisplayName("should report the price change with the first date at the new amount")
        void shouldReportChangeWithSince() {
            // arrange -- newest first: two charges at 12, before them a charge at 10
            List<RecurringCharge> charges = List.of(
                    charge(TODAY.minusDays(2), NEW_AMOUNT),
                    charge(TODAY.minusDays(32), NEW_AMOUNT),
                    charge(TODAY.minusDays(62), "10.00"));

            // act
            Projection result = RecurringProjection.project(item(MONTHLY, TODAY), charges, TODAY);

            // assert & verify
            assertNotNull(result.priceChange());
            assertEquals(0, new BigDecimal(STORED_AMOUNT).compareTo(result.priceChange().previousAmount()));
            assertEquals(0, new BigDecimal(NEW_AMOUNT).compareTo(result.priceChange().newAmount()));
            assertEquals(TODAY.minusDays(32), result.priceChange().since());
        }

        @Test
        @DisplayName("should find the start of the run when the price changed twice")
        void shouldFindRunStartAfterTwoChanges() {
            // arrange -- stored 10, charges went 10 -> 11 -> 12 and 12 has now been charged twice
            List<RecurringCharge> charges = List.of(
                    charge(TODAY.minusDays(2), NEW_AMOUNT),
                    charge(TODAY.minusDays(32), NEW_AMOUNT),
                    charge(TODAY.minusDays(62), MID_AMOUNT),
                    charge(TODAY.minusDays(92), "10.00"));

            // act
            Projection result = RecurringProjection.project(item(MONTHLY, TODAY), charges, TODAY);

            // assert & verify
            assertEquals(TODAY.minusDays(32), result.priceChange().since());
            assertEquals(0, new BigDecimal(NEW_AMOUNT).compareTo(result.priceChange().newAmount()));
        }

        @Test
        @DisplayName("should not report a price change from one charge at a new price")
        void shouldWaitForASecondChargeAtTheNewPrice() {
            // arrange -- a single charge may be a one-off, so wait for the new price to repeat
            List<RecurringCharge> charges = List.of(
                    charge(TODAY.minusDays(2), NEW_AMOUNT),
                    charge(TODAY.minusDays(32), STORED_AMOUNT));

            // act & assert & verify
            assertNull(RecurringProjection.project(item(MONTHLY, TODAY), charges, TODAY).priceChange());
        }

        @Test
        @DisplayName("should not report a price change for a bill that varies every month")
        void shouldIgnoreVariableBills() {
            // arrange
            List<RecurringCharge> charges = List.of(
                    charge(TODAY.minusDays(2), "91.40"),
                    charge(TODAY.minusDays(32), "87.12"),
                    charge(TODAY.minusDays(62), "90.00"));

            // act & assert & verify
            assertNull(RecurringProjection.project(item(MONTHLY, TODAY), charges, TODAY).priceChange());
        }
    }

    /** PF-896: a merchant also used for unrelated purchases must not lend them to the subscription as its charges. */
    @Nested
    @DisplayName("unrelated purchases at the same merchant")
    class UnrelatedPurchaseTests {

        private final LocalDate lastMonthly = TODAY.minusDays(12);

        private Projection project(String frequency, List<RecurringCharge> charges) {
            return RecurringProjection.project(item(frequency, TODAY.minusYears(1)), charges, TODAY);
        }

        @Test
        @DisplayName("should take the monthly charge as the last charge among several unrelated purchases")
        void shouldIgnoreUnrelatedPurchasesBesideTheMonthlyCharge() {
            // arrange -- newest first: unrelated purchases of 54.99, 23.10 and 140.00 around one 10.00 charge
            List<RecurringCharge> charges = List.of(
                    charge(TODAY.minusDays(3), UNRELATED),
                    charge(TODAY.minusDays(8), "23.10"),
                    charge(lastMonthly, STORED_AMOUNT),
                    charge(TODAY.minusDays(20), "140.00"));

            // act
            Projection result = project(MONTHLY, charges);

            // assert & verify
            assertEquals(lastMonthly, result.lastChargeDate());
            assertEquals(0, new BigDecimal(STORED_AMOUNT).compareTo(result.lastChargeAmount()));
            assertEquals(lastMonthly.plusMonths(1), result.projectedNextDate());
            assertNull(result.priceChange());
        }

        @Test
        @DisplayName("should ignore an unrelated purchase newer than the real charge")
        void shouldIgnoreANewerUnrelatedPurchase() {
            // arrange
            List<RecurringCharge> charges = List.of(
                    charge(TODAY.minusDays(1), "89.99"),
                    charge(lastMonthly, STORED_AMOUNT),
                    charge(lastMonthly.minusMonths(1), STORED_AMOUNT));

            // act
            Projection result = project(MONTHLY, charges);

            // assert & verify
            assertEquals(lastMonthly, result.lastChargeDate());
            assertEquals(lastMonthly.plusMonths(1), result.projectedNextDate());
            assertNull(result.priceChange());
        }

        @Test
        @DisplayName("should give no last charge when no charge is within the band")
        void shouldGiveNoLastChargeWhenNothingIsInBand() {
            // arrange
            LocalDate stored = TODAY.plusDays(4);

            // act
            Projection result = RecurringProjection.project(item(MONTHLY, stored),
                    List.of(charge(TODAY.minusDays(3), UNRELATED), charge(TODAY.minusDays(9), "23.10")), TODAY);

            // assert & verify
            assertNull(result.lastChargeDate());
            assertNull(result.lastChargeAmount());
            assertNull(result.priceChange());
            assertEquals(stored, result.projectedNextDate());
        }

        @Test
        @DisplayName("should count a charge exactly on the band edge and use the one dollar floor for a small amount")
        void shouldKeepTheBandEdgesInclusive() {
            // arrange -- 20 percent of 10.00 is 2.00 (edge 12.00 counts); a 3.00 item has a 1.00 floor (edge 4.00 counts)
            RecurringTransaction small = RecurringTransaction.builder()
                    .frequency(MONTHLY).nextDate(TODAY).amount(new BigDecimal("3.00")).build();

            // act
            Projection edge = project(MONTHLY, List.of(charge(lastMonthly, "12.00")));
            Projection floor = RecurringProjection.project(small, List.of(charge(lastMonthly, "4.00")), TODAY);
            Projection outside = RecurringProjection.project(small, List.of(charge(lastMonthly, "4.01")), TODAY);

            // assert & verify
            assertEquals(lastMonthly, edge.lastChargeDate());
            assertEquals(lastMonthly, floor.lastChargeDate());
            assertNull(outside.lastChargeDate());
        }

        @Test
        @DisplayName("should detect a real price change beside unrelated purchases and follow it")
        void shouldDetectAPriceChangeNextToUnrelatedPurchases() {
            // arrange -- the two newest charges agree on 15.00, a month apart, outside the 8.00 to 12.00 band
            LocalDate newest = TODAY.minusDays(5);
            List<RecurringCharge> charges = List.of(
                    charge(newest, HIGH_AMOUNT),
                    charge(newest.minusMonths(1), HIGH_AMOUNT),
                    charge(newest.minusMonths(1).minusDays(4), UNRELATED),
                    charge(newest.minusMonths(2), STORED_AMOUNT));

            // act
            Projection result = project(MONTHLY, charges);

            // assert & verify
            assertEquals(newest, result.lastChargeDate());
            assertEquals(0, new BigDecimal(HIGH_AMOUNT).compareTo(result.lastChargeAmount()));
            assertEquals(newest.plusMonths(1), result.projectedNextDate());
            assertNotNull(result.priceChange());
            assertEquals(0, new BigDecimal(STORED_AMOUNT).compareTo(result.priceChange().previousAmount()));
            assertEquals(0, new BigDecimal(HIGH_AMOUNT).compareTo(result.priceChange().newAmount()));
            assertEquals(newest.minusMonths(1), result.priceChange().since());
        }

        @Test
        @DisplayName("should not treat one large out-of-band purchase as a price change")
        void shouldNotTreatASingleLargePurchaseAsAPriceChange() {
            // arrange
            List<RecurringCharge> charges = List.of(
                    charge(TODAY.minusDays(2), "250.00"),
                    charge(lastMonthly, STORED_AMOUNT));

            // act
            Projection result = project(MONTHLY, charges);

            // assert & verify
            assertNull(result.priceChange());
            assertEquals(lastMonthly, result.lastChargeDate());
        }

        @Test
        @DisplayName("should not treat two out-of-band charges that disagree on amount as a price change")
        void shouldNotTreatDisagreeingChargesAsAPriceChange() {
            // arrange
            List<RecurringCharge> charges = List.of(
                    charge(TODAY.minusDays(2), HIGH_AMOUNT),
                    charge(TODAY.minusDays(32), "16.50"),
                    charge(TODAY.minusDays(62), STORED_AMOUNT));

            // act
            Projection result = project(MONTHLY, charges);

            // assert & verify
            assertNull(result.priceChange());
            assertEquals(TODAY.minusDays(62), result.lastChargeDate());
        }

        @Test
        @DisplayName("should not treat two agreeing out-of-band charges that are not one cadence apart as a price change")
        void shouldNotTreatSameDayPurchasesAsAPriceChange() {
            // arrange -- two 15.00 purchases two days apart are not a monthly price
            List<RecurringCharge> charges = List.of(
                    charge(TODAY.minusDays(2), HIGH_AMOUNT),
                    charge(TODAY.minusDays(4), HIGH_AMOUNT),
                    charge(lastMonthly, STORED_AMOUNT));

            // act
            Projection result = project(MONTHLY, charges);

            // assert & verify
            assertNull(result.priceChange());
            assertEquals(lastMonthly, result.lastChargeDate());
        }

        @Test
        @DisplayName("should still detect a small price change between in-band charges")
        void shouldKeepDetectingAPriceChangeInsideTheBand() {
            // arrange
            List<RecurringCharge> charges = List.of(
                    charge(TODAY.minusDays(40), UNRELATED),
                    charge(TODAY.minusDays(2), MID_AMOUNT),
                    charge(TODAY.minusDays(32), MID_AMOUNT),
                    charge(TODAY.minusDays(62), STORED_AMOUNT));

            // act
            Projection result = project(MONTHLY, charges);

            // assert & verify
            assertEquals(TODAY.minusDays(2), result.lastChargeDate());
            assertNotNull(result.priceChange());
            assertEquals(0, new BigDecimal(MID_AMOUNT).compareTo(result.priceChange().newAmount()));
            assertEquals(TODAY.minusDays(32), result.priceChange().since());
        }

        @Test
        @DisplayName("should use a yearly period for the price change cadence")
        void shouldUseTheYearlyPeriod() {
            // arrange -- 15.00 a year apart is one cadence for a yearly item, and a month apart is not
            LocalDate newest = TODAY.minusDays(30);
            List<RecurringCharge> yearly = List.of(charge(newest, HIGH_AMOUNT), charge(newest.minusYears(1).plusDays(20), HIGH_AMOUNT));
            List<RecurringCharge> monthlyApart = List.of(charge(newest, HIGH_AMOUNT), charge(newest.minusMonths(1), HIGH_AMOUNT));

            // act
            Projection agreed = project(YEARLY, yearly);
            Projection tooClose = project(YEARLY, monthlyApart);

            // assert & verify
            assertNotNull(agreed.priceChange());
            assertEquals(newest.plusYears(1), agreed.projectedNextDate());
            assertNull(tooClose.priceChange());
        }

        @Test
        @DisplayName("should use a weekly period for the price change cadence")
        void shouldUseTheWeeklyPeriod() {
            // arrange -- a 9 day gap is outside seven days plus a quarter, an 8 day gap is inside it
            LocalDate newest = TODAY.minusDays(3);
            List<RecurringCharge> inside = List.of(charge(newest, HIGH_AMOUNT), charge(newest.minusDays(8), HIGH_AMOUNT));
            List<RecurringCharge> outside = List.of(charge(newest, HIGH_AMOUNT), charge(newest.minusDays(9), HIGH_AMOUNT));

            // act
            Projection near = project(WEEKLY, inside);
            Projection far = project(WEEKLY, outside);

            // assert & verify
            assertNotNull(near.priceChange());
            assertEquals(newest.plusWeeks(1), near.projectedNextDate());
            assertNull(far.priceChange());
        }
    }
}
