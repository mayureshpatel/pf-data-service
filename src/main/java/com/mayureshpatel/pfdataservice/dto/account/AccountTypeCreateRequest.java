package com.mayureshpatel.pfdataservice.dto.account;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
public class AccountTypeCreateRequest {

    @NotNull(message = "Account type code cannot be null.")
    @Size(max = 20, message = "Account type code must be less than 20 characters.")
    private final String code;

    @NotNull(message = "Account type label cannot be null.")
    @Size(max = 50, message = "Account type label must be less than 50 characters.")
    private final String label;

    @Size(max = 50, message = "Account type icon must be less than 50 characters.")
    private final String icon;

    @Size(max = 20, message = "Account type color must be less than 20 characters.")
    private final String color;

    @NotNull(message = "Account type is asset cannot be null.")
    private final boolean isAsset;

    @NotNull(message = "Account type sort order cannot be null.")
    @PositiveOrZero(message = "Account type sort order must be a positive number or zero.")
    private final Integer sortOrder;

    @NotNull(message = "Account type is active cannot be null.")
    private final boolean isActive;
}
