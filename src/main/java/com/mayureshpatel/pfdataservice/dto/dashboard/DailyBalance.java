package com.mayureshpatel.pfdataservice.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyBalance(
    LocalDate date,
    BigDecimal balance
) {}
