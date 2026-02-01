package com.mayureshpatel.pfdataservice.dto;

import com.mayureshpatel.pfdataservice.model.AccountType;
import com.mayureshpatel.pfdataservice.model.BankName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AccountDto(
    Long id,

    @NotBlank(message = "Name is required")
    String name,

    @NotNull(message = "Type is required")
    AccountType type,

    @NotNull(message = "Current balance is required")
    BigDecimal currentBalance,

    BankName bankName
) {}
