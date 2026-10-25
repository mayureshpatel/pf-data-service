package com.mayureshpatel.pfdataservice.domain.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One real charge matched to a tracked recurring item: the day it posted and what it cost.
 *
 * @param date   the charge's date
 * @param amount the charge's amount
 */
public record RecurringCharge(LocalDate date, BigDecimal amount) {
}
