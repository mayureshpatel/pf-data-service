package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AccountSnapshot {

    private Long id;
    private Account account;
    private LocalDate snapshotDate;
    private BigDecimal balance;

    private TableAudit audit;
}
