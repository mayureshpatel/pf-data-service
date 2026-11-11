package com.mayureshpatel.pfdataservice.dto.account;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The request payload for entering a hand-recorded balance on a manual-balance account.
 * {@code asOfDate} is the date the balance was true on (entering a second balance for the same
 * date replaces the first), and {@code version} is the optimistic-locking token the caller last
 * read, so an entry made against a stale account fails loudly instead of overwriting a change.
 *
 * @param accountId the manual-balance account the balance belongs to
 * @param balance   the balance as of {@code asOfDate}
 * @param asOfDate  the date the balance was true on
 * @param version   the account version last read by the caller
 */
public record BalanceEntryRequest(
        @NotNull(message = "Account ID is required") Long accountId,
        @NotNull(message = "Balance is required") BigDecimal balance,
        @NotNull(message = "As-of date is required") LocalDate asOfDate,
        @NotNull(message = "Version is required") Long version
) {
}
