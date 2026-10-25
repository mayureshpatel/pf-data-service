package com.mayureshpatel.pfdataservice.dto.transaction.recurring;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The request payload for creating a new {@code RecurringTransaction} template. {@code merchantId}
 * is required here even though a real generated {@code Transaction}'s merchant can be null -- the
 * existing {@code todo} below on this same gap has not been resolved, so this documents the
 * inconsistency rather than a settled design.
 *
 * <p>a record (PF-849): a plain class with a Lombok-generated {@code isActive()} getter made
 * springdoc/Jackson's reflection-based property ordering non-deterministic across separate JVM
 * invocations. no {@code @JsonProperty} pinning is needed here -- {@code active} has no "is"
 * prefix to begin with, so the record's verbatim component-name serialization already matches
 * this endpoint's existing wire contract.
 */
@Builder(toBuilder = true)
public record RecurringTransactionCreateRequest(
        @NotNull(message = "User ID cannot be null.")
        @Positive(message = "User ID must be a positive number.")
        Long userId,

        @NotNull(message = "Account ID cannot be null.")
        @Positive(message = "Account ID must be a positive number.")
        Long accountId,

        @NotNull(message = "Amount cannot be null.")
        @DecimalMin(value = "-9999999999.99", message = "Amount must be greater than or equal to -9999999999.99")
        @DecimalMax(value = "9999999999.99", message = "Amount must be less than or equal to 9999999999.99")
        BigDecimal amount,

        @NotNull(message = "Frequency cannot be null.")
        @Pattern(regexp = "^(WEEKLY|BI_WEEKLY|MONTHLY|YEARLY|QUARTERLY)$", message = "Frequency must be one of WEEKLY, BI_WEEKLY, MONTHLY, YEARLY, or QUARTERLY")
        String frequency,

        @Past(message = "Last date must be in the past.")
        LocalDate lastDate,

        @NotNull(message = "Next date cannot be null.")
        LocalDate nextDate,

        boolean active,

        // todo: this does not seem right, as merchant can be null in transactions
        @NotNull(message = "Merchant ID cannot be null.")
        @Positive(message = "Merchant ID must be a positive number.")
        Long merchantId) {
}
