package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringCharge;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.PriceChangeDto;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Turns a tracked recurring item's stored schedule and its newest matched real charges into what the
 * page shows: the last real charge, a next date rolled forward from it, whether that date has
 * passed without a charge, and a price change when the newest charge no longer costs what the item
 * says. Pure and stateless: nothing here is stored, so the stored amount and schedule stay exactly
 * what the user set until they choose to update them.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class RecurringProjection {

    /**
     * The projected view of one tracked item.
     *
     * @param lastChargeDate    the newest matched charge's date, or null when nothing matched
     * @param lastChargeAmount  the newest matched charge's amount, or null when nothing matched
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
     * @param chargesNewestFirst the item's matched real charges, newest first (may be empty)
     * @param today             the date to judge "overdue" against
     * @return the projection
     */
    public static Projection project(RecurringTransaction item, List<RecurringCharge> chargesNewestFirst, LocalDate today) {
        if (chargesNewestFirst == null || chargesNewestFirst.isEmpty()) {
            LocalDate stored = item.getNextDate();
            return new Projection(null, null, stored, stored != null && stored.isBefore(today), null);
        }
        RecurringCharge newest = chargesNewestFirst.get(0);
        LocalDate rolled = Frequency.fromCode(item.getFrequency()).advance(newest.date());
        // a stored date later than the rolled one is the user pushing it out: honour it; an earlier,
        // stale one has been overtaken by the real charges
        LocalDate stored = item.getNextDate();
        LocalDate next = stored != null && stored.isAfter(rolled) ? stored : rolled;
        return new Projection(newest.date(), newest.amount(), next, next.isBefore(today),
                findPriceChange(item.getAmount(), chargesNewestFirst));
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
