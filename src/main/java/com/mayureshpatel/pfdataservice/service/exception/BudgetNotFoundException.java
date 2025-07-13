package com.mayureshpatel.pfdataservice.service.exception;

import java.time.YearMonth;
import java.util.UUID;

public class BudgetNotFoundException extends RuntimeException {
    public BudgetNotFoundException(UUID userId, YearMonth m) {
        super("No budget for user %s and %s".formatted(userId, m));
    }
}

