package com.mayureshpatel.pfdataservice.dto.account;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
@ToString
public class AccountCreateRequest {

    @NotBlank(message = "Account name cannot be blank.")
    @Size(max = 100, message = "Account name must be less than 100 characters.")
    private final String name;

    @NotBlank(message = "Account type cannot be blank.")
    @Size(max = 20, message = "Account type must be less than 20 characters.")
    private final String type;

    @ToString.Exclude
    @NotNull(message = "Starting balance cannot be null.")
    @DecimalMin(value = "-9999999999.99", message = "Starting balance must be greater than or equal to -9999999999.99")
    @DecimalMax(value = "9999999999.99", message = "Starting balance must be less than or equal to 9999999999.99")
    private BigDecimal startingBalance;

    @NotBlank(message = "Currency code cannot be blank.")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters.")
    private final String currencyCode;

    @Size(max = 50, message = "Bank name must be less than 50 characters.")
    private final String bankName;
}
