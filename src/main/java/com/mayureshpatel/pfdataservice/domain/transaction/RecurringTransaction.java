package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * A template for a transaction that repeats on a schedule ({@link #frequency}), from which a real
 * {@code Transaction} is generated each time {@link #nextDate} is reached. Extends
 * {@code Transaction} to reuse its account/category/amount/merchant fields for the template's own
 * values -- an inheritance relationship the existing {@code todo} below already flags as worth
 * reconsidering, not a settled design choice.
 */
// todo: maybe make this not extend Transaction?
@Getter
@SuperBuilder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class RecurringTransaction extends Transaction {

    @EqualsAndHashCode.Include
    private Long id;
    private Long userId;
    private String frequency;

    private LocalDate lastDate;
    private LocalDate nextDate;
    private boolean active;

    @ToString.Exclude
    private TableAudit audit;
}
