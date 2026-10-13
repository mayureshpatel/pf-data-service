package com.mayureshpatel.pfdataservice.dto.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * The request payload for creating a new {@code AccountType} lookup row (e.g. "Checking",
 * "Credit Card"). {@code isAsset} classifies whether the type represents money the user has
 * (checking, savings) versus money the user owes (credit cards) -- a display/grouping
 * distinction; net-worth totals are summed directly from each account's own balance regardless of
 * this flag (see {@code ReportService.getNetWorthOverTime}), so a liability-type account still
 * needs a negative stored balance to net out correctly.
 *
 * <p>a record (PF-849): a plain class with a Lombok-generated {@code isXxx()} getter made
 * springdoc/Jackson's reflection-based property ordering non-deterministic across separate JVM
 * invocations. the {@code @JsonProperty} overrides on the two boolean components pin this
 * endpoint's wire contract to "asset"/"active" (its existing shape, since the old Lombok class
 * stripped the "is" prefix per JavaBean convention) -- a bare record component would otherwise
 * serialize verbatim as "isAsset"/"isActive" instead, a breaking change. unlike the sibling
 * {@code AccountTypeDto} (read side), which has no such pinning and genuinely does emit
 * "isAsset"/"isActive" -- a pre-existing read/write asymmetry, not something this fix touches.
 */
@Builder(toBuilder = true)
public record AccountTypeCreateRequest(
        @NotNull(message = "Account type code cannot be null.")
        @Size(max = 20, message = "Account type code must be less than 20 characters.")
        String code,

        @NotNull(message = "Account type label cannot be null.")
        @Size(max = 50, message = "Account type label must be less than 50 characters.")
        String label,

        @Size(max = 50, message = "Account type icon must be less than 50 characters.")
        String icon,

        @Size(max = 20, message = "Account type color must be less than 20 characters.")
        String color,

        @NotNull(message = "Account type is asset cannot be null.")
        @JsonProperty("asset")
        boolean isAsset,

        @NotNull(message = "Account type sort order cannot be null.")
        @PositiveOrZero(message = "Account type sort order must be a positive number or zero.")
        Integer sortOrder,

        @NotNull(message = "Account type is active cannot be null.")
        @JsonProperty("active")
        boolean isActive) {
}
