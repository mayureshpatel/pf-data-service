package com.mayureshpatel.pfdataservice.dto.budget;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * The request payload for updating an existing {@code Budget}'s amount, identified by {@code id}.
 * Deliberately narrower than {@link BudgetCreateRequest} -- category/month/year aren't editable
 * here; moving a budget to a different category or month is a new budget, not an edit of this one.
 */
@Getter
@Builder(toBuilder = true)
@ToString
public class BudgetUpdateRequest {

    @NotNull(message = "Budget ID cannot be null.")
    @Positive(message = "Budget ID must be a positive number.")
    private final Long id;

    @NotNull(message = "User ID cannot be null.")
    @Positive(message = "User ID must be a positive number.")
    private final Long userId;

    @NotNull(message = "Amount cannot be null.")
    @DecimalMin(value = "-9999999999.99", message = "Amount must be greater than or equal to -9999999999.99")
    @DecimalMax(value = "9999999999.99", message = "Amount must be less than or equal to 9999999999.99")
    private final BigDecimal amount;
}
