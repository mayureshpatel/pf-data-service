package com.mayureshpatel.pfdataservice.dto.budget;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
@ToString
public class BudgetCreateRequest {

    @NotNull(message = "User ID cannot be null.")
    @Positive(message = "User ID must be a positive number.")
    private final Long userId;

    @NotNull(message = "Category ID cannot be null.")
    @Positive(message = "Category ID must be a positive number.")
    private final Long categoryId;

    @NotNull(message = "Amount cannot be null.")
    @DecimalMin(value = "-9999999999.99", message = "Amount must be greater than or equal to -9999999999.99")
    @DecimalMax(value = "9999999999.99", message = "Amount must be less than or equal to 9999999999.99")
    private final BigDecimal amount;

    @NotNull(message = "Month cannot be null.")
    @Positive(message = "Month must be a positive number.")
    @DecimalMin(value = "1", message = "Month must be greater than or equal to 1")
    @DecimalMax(value = "12", message = "Month must be less than or equal to 12")
    private final Integer month;

    @NotNull(message = "Year cannot be null.")
    @Positive(message = "Year must be a positive number.")
    @Min(value = 1900, message = "Year must be greater than or equal to 1")
    @Max(value = 9999, message = "Year must be less than or equal to 9999")
    private final Integer year;
}
