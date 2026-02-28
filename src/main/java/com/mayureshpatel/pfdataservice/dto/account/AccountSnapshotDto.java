package com.mayureshpatel.pfdataservice.dto.account;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountSnapshotDto(
        Long id,
        AccountDto account,
        LocalDate snapshotDate,
        BigDecimal balance
) {
}
