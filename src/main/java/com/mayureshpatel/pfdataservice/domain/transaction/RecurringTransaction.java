package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

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
