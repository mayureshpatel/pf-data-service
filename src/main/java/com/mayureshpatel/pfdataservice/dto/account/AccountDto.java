package com.mayureshpatel.pfdataservice.dto.account;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Represents an account object.
 * @param id the account id
 * @param name the account name
 * @param type the account type code (validated against account_types lookup table)
 * @param currentBalance the current balance
 * @param bankName the bank name
 */
public record AccountDto(
    Long id,

    @NotBlank(message = "Name is required")
    String name,

    @NotNull(message = "Type is required")
    @NotBlank(message = "Type cannot be blank")
    String type,

    @NotNull(message = "Current balance is required")
    BigDecimal currentBalance,

    BankName bankName
) {}
