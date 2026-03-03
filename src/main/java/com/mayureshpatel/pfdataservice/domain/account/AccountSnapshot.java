package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccountSnapshot {

    @EqualsAndHashCode.Include
    private Long id;
    private Long accountId;
    private Account account;
    private LocalDate snapshotDate;
    @ToString.Exclude
    private BigDecimal balance;

    @ToString.Exclude
    private TableAudit audit;
}
