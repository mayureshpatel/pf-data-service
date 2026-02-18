package com.mayureshpatel.pfdataservice.dto.account;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AccountSnapshotDto(
        Long id,
        AccountDto account,
        OffsetDateTime snapshotDate,
        BigDecimal balance
) {
}
