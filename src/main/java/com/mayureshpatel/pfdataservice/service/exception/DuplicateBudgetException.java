package com.mayureshpatel.pfdataservice.service.exception;

import java.time.YearMonth;
import java.util.UUID;

public class DuplicateBudgetException extends RuntimeException {
    public DuplicateBudgetException(UUID userId, YearMonth m) {
        super("Budget already exists for user %s and %s".formatted(userId, m));
    }
    public DuplicateBudgetException(UUID userId, YearMonth m, Throwable cause) {
        super("Budget already exists for user %s and %s".formatted(userId, m), cause);
    }
}

