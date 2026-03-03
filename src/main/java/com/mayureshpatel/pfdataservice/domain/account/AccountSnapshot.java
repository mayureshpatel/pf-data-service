package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.CreatedAtAudit;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class AccountSnapshot {

    @EqualsAndHashCode.Include
    private Long id;
    private Long accountId;
    private Account account;
    private LocalDate snapshotDate;
    @ToString.Exclude
    private BigDecimal balance;

    @ToString.Exclude
    private CreatedAtAudit audit;
}
