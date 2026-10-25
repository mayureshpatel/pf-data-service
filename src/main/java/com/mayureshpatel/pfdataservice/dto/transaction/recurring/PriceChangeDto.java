package com.mayureshpatel.pfdataservice.dto.transaction.recurring;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A tracked item whose real charges no longer match its stored amount.
 *
 * @param previousAmount the amount stored on the tracked item
 * @param newAmount      what the newest matched charge actually cost
 * @param since          the date of the first charge in the unbroken run at the new amount
 */
public record PriceChangeDto(BigDecimal previousAmount, BigDecimal newAmount, LocalDate since) {
}
