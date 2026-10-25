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
        LocalDate next = Frequency.fromCode(item.getFrequency()).advance(newest.date());
        return new Projection(newest.date(), newest.amount(), next, next.isBefore(today),
                findPriceChange(item.getAmount(), chargesNewestFirst));
    }

    /**
     * A price change exists when the newest charge differs from the stored amount; it began at the
     * oldest charge of the unbroken run at the newest amount.
     */
    private static PriceChangeDto findPriceChange(BigDecimal storedAmount, List<RecurringCharge> chargesNewestFirst) {
        BigDecimal newest = chargesNewestFirst.get(0).amount();
        if (storedAmount == null || newest == null || newest.compareTo(storedAmount) == 0) {
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
