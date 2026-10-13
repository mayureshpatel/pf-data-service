package com.mayureshpatel.pfdataservice.dto.transaction.recurring;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The request payload for updating an existing {@code RecurringTransaction} template, identified
 * by {@code id}. Unlike its sibling
 * {@link com.mayureshpatel.pfdataservice.dto.transaction.TransactionUpdateRequest}, there's no
 * {@code categoryId} field here (nor on {@link RecurringTransactionCreateRequest}) even though
 * {@code RecurringTransaction} inherits a {@code category} field from {@code Transaction} -- the
 * class-level {@code todo} below on that inheritance relationship not matching the database
 * structure is the same open design question this DTO's gap traces back to.
 *
 * <p>a record (PF-849): a plain class with a Lombok-generated {@code isActive()} getter made
 * springdoc/Jackson's reflection-based property ordering non-deterministic across separate JVM
 * invocations. no {@code @JsonProperty} pinning is needed here -- {@code active} has no "is"
 * prefix to begin with, so the record's verbatim component-name serialization already matches
 * this endpoint's existing wire contract.
 */
//todo: recurring transaction should be a subclass of transaction; the database does not reflect this
@Builder(toBuilder = true)
public record RecurringTransactionUpdateRequest(
        @NotNull(message = "Recurring transaction ID cannot be null.")
        @Positive(message = "Recurring transaction ID must be a positive number.")
        Long id,

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

        @NotNull(message = "Next date cannot be null.")
        @Future(message = "Next date must be in the future.")
        LocalDate nextDate,

        boolean active,

        // todo: this does not seem right, as merchant can be null in transactions
        @NotNull(message = "Merchant ID cannot be null.")
        @Positive(message = "Merchant ID must be a positive number.")
        Long merchantId) {
}
