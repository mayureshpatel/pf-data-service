package com.mayureshpatel.pfdataservice.domain.transaction;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.domain.vendor.Vendor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringTransaction {

    private Long id;
    private User user;
    private Account account;

    private Vendor vendor;
    private BigDecimal amount;
    private Frequency frequency;

    private LocalDate lastDate;
    private LocalDate nextDate;

    @Builder.Default
    private boolean active = true;

    private TableAudit audit;
}
