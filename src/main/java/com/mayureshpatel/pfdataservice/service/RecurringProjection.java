package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringCharge;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.PriceChangeDto;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Turns a tracked recurring item's stored schedule and its newest matched real charges into what the
 * page shows: the last real charge, a next date rolled forward from it, whether that date has
 * passed without a charge, and a price change when the newest charge no longer costs what the item
 * says. Pure and stateless: nothing here is stored, so the stored amount and schedule stay exactly
 * what the user set until they choose to update them.
 *
 * <p>A merchant can also be used for unrelated purchases (PF-896), so only charges within an amount band
 * around the stored amount count as the item's own; a new price outside the band is recognised only when
 * the two newest charges at the merchant agree on it one cadence apart.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class RecurringProjection {

    /** the band around the stored amount, as a fraction of it, inside which a charge is the item's own. */
    static final BigDecimal BAND_PERCENT = new BigDecimal("0.20");

    /** the smallest band half-width, so a cheap item still tolerates a small rounding or tax difference. */
    static final BigDecimal BAND_MINIMUM = new BigDecimal("1.00");

    /** how far apart two charges may be and still agree on one new price. */
    private static final BigDecimal PRICE_AGREEMENT = new BigDecimal("0.01");

    /** a new price's two charges must be one period apart, give or take this fraction (1/4) of the period. */
    private static final int CADENCE_TOLERANCE_DIVISOR = 4;

    /**
     * The projected view of one tracked item.
     *
     * @param lastChargeDate    the newest of the item's own charges' date, or null when it has none
     * @param lastChargeAmount  the newest of the item's own charges' amount, or null when it has none
     * @param projectedNextDate when the next charge is expected
     * @param overdue           whether that date has already passed
     * @param priceChange       the price change the newest charge reveals, or null
     */
    public record Projection(
            LocalDate lastChargeDate,
            BigDecimal lastChargeAmount,
            LocalDate projectedNextDate,
            boolean overdue,
            PriceChangeDto priceChange
    ) {
    }

    /**
     * Projects one tracked item.
     *
     * @param item              the tracked item (its stored amount, frequency and next date)
     * @param chargesNewestFirst every charge matched at the item's merchant, newest first (may be empty);
     *                          unrelated purchases among them are filtered out here
     * @param today             the date to judge "overdue" against
     * @return the projection
     */
    public static Projection project(RecurringTransaction item, List<RecurringCharge> chargesNewestFirst, LocalDate today) {
        if (chargesNewestFirst == null || chargesNewestFirst.isEmpty()) {
            return withoutCharges(item, today);
        }
        Frequency frequency = Frequency.fromCode(item.getFrequency());
        BigDecimal stored = item.getAmount();
        List<RecurringCharge> own;
        PriceChangeDto priceChange;
        if (isNewPriceOutsideBand(stored, chargesNewestFirst, frequency)) {
            // the two newest charges agree on a new price: it is the subscription's price from now on
            own = chargesNewestFirst;
            priceChange = new PriceChangeDto(stored, chargesNewestFirst.get(0).amount(),
                    newPriceSince(chargesNewestFirst, frequency));
        } else {
            own = chargesNewestFirst.stream().filter(charge -> isInBand(stored, charge.amount())).toList();
            if (own.isEmpty()) {
                return withoutCharges(item, today);
            }
            priceChange = findPriceChange(stored, own);
        }
        RecurringCharge newest = own.get(0);
        LocalDate rolled = frequency.advance(newest.date());
        // a stored date later than the rolled one is the user pushing it out: honour it; an earlier,
        // stale one has been overtaken by the real charges
        LocalDate storedDate = item.getNextDate();
        LocalDate next = storedDate != null && storedDate.isAfter(rolled) ? storedDate : rolled;
        return new Projection(newest.date(), newest.amount(), next, isOverdue(next, item, today), priceChange);
    }

    /** the projection of an item none of whose matched charges are its own: the stored schedule stands. */
    private static Projection withoutCharges(RecurringTransaction item, LocalDate today) {
        LocalDate stored = item.getNextDate();
        return new Projection(null, null, stored, isOverdue(stored, item, today), null);
    }

    /**
     * Whether a charge belongs to the item: its amount is within the larger of 20 percent of the stored
     * amount and one dollar either side of it, edges included. With no stored amount or no charge amount
     * there is nothing to compare, so the charge counts.
     */
    private static boolean isInBand(BigDecimal storedAmount, BigDecimal amount) {
        if (storedAmount == null || amount == null) {
            return true;
        }
        BigDecimal halfWidth = storedAmount.abs().multiply(BAND_PERCENT).max(BAND_MINIMUM);
        return amount.subtract(storedAmount).abs().compareTo(halfWidth) <= 0;
    }

    /**
     * Whether the two newest charges at the merchant, whatever their amounts, are a new price: both outside
     * the band, within a cent of each other, and one cadence apart. One out-of-band purchase, or two that
     * disagree or sit at the wrong spacing, are unrelated purchases rather than a new price.
     */
    private static boolean isNewPriceOutsideBand(BigDecimal storedAmount, List<RecurringCharge> chargesNewestFirst,
                                                  Frequency frequency) {
        if (storedAmount == null || chargesNewestFirst.size() < 2) {
            return false;
        }
        RecurringCharge newer = chargesNewestFirst.get(0);
        RecurringCharge older = chargesNewestFirst.get(1);
        return newer.amount() != null && older.amount() != null
                && !isInBand(storedAmount, newer.amount()) && !isInBand(storedAmount, older.amount())
                && agree(newer.amount(), older.amount())
                && isOneCadenceApart(older.date(), newer.date(), frequency);
    }

    private static boolean agree(BigDecimal a, BigDecimal b) {
        return a.subtract(b).abs().compareTo(PRICE_AGREEMENT) <= 0;
    }

    /** whether the gap between two dates is the frequency's period, give or take a quarter of it. */
    private static boolean isOneCadenceApart(LocalDate older, LocalDate newer, Frequency frequency) {
        long period = ChronoUnit.DAYS.between(older, frequency.advance(older));
        long gap = ChronoUnit.DAYS.between(older, newer);
        return Math.abs(gap - period) * CADENCE_TOLERANCE_DIVISOR <= period;
    }

    /** the start of the unbroken run of charges, one cadence apart, at the new price (newest first). */
    private static LocalDate newPriceSince(List<RecurringCharge> chargesNewestFirst, Frequency frequency) {
        BigDecimal price = chargesNewestFirst.get(0).amount();
        LocalDate since = chargesNewestFirst.get(0).date();
        for (int i = 1; i < chargesNewestFirst.size(); i++) {
            RecurringCharge charge = chargesNewestFirst.get(i);
            if (charge.amount() == null || !agree(price, charge.amount())
                    || !isOneCadenceApart(charge.date(), since, frequency)) {
                break;
            }
            since = charge.date();
        }
        return since;
    }

    /**
     * A date is overdue once it is further past than the item's grace period: a statement imported
     * a few days late, or a charge that posts a few days late, is not yet worth an amber warning.
     */
    private static boolean isOverdue(LocalDate expected, RecurringTransaction item, LocalDate today) {
        return expected != null && expected.plusDays(graceDays(Frequency.fromCode(item.getFrequency()))).isBefore(today);
    }

    private static int graceDays(Frequency frequency) {
        return switch (frequency) {
            case WEEKLY -> 2;
            case BI_WEEKLY -> 4;
            case MONTHLY -> 7;
            case QUARTERLY -> 10;
            case YEARLY -> 14;
        };
    }

    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        return a != null && b != null && a.compareTo(b) == 0;
    }

    /**
     * A price change exists when the last two charges agree on an amount that differs from the stored
     * one; it began at the oldest charge of the unbroken run at that amount.
     */
    private static PriceChangeDto findPriceChange(BigDecimal storedAmount, List<RecurringCharge> chargesNewestFirst) {
        BigDecimal newest = chargesNewestFirst.get(0).amount();
        if (storedAmount == null || newest == null || newest.compareTo(storedAmount) == 0) {
            return null;
        }
        // one charge at a new amount may be a one-off, and a bill that varies every month never repeats
        // itself: only a new price the last two charges agree on counts as a price change
        if (chargesNewestFirst.size() < 2 || !sameAmount(newest, chargesNewestFirst.get(1).amount())) {
            return null;
        }
        LocalDate since = chargesNewestFirst.get(0).date();
        for (RecurringCharge charge : chargesNewestFirst) {
            if (charge.amount() == null || charge.amount().compareTo(newest) != 0) {
                break;
            }
            since = charge.date();
        }
        return new PriceChangeDto(storedAmount, newest, since);
    }
}
