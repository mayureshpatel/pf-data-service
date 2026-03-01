package com.mayureshpatel.pfdataservice.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Represents an account object.
 *
 * @param id               the account id
 * @param userId           the user who owns the account
 * @param name             the account name
 * @param accountTypeCode  the account type code
 * @param accountTypeLabel the account type label
 * @param currentBalance   the current balance
 * @param currencyCode     the currency code (ISO 4217)
 * @param currencySymbol   the currency symbol
 * @param bankName         the bank name
 */
public record AccountDto(
        Long id,
        Long userId,
        
        @NotBlank(message = "Account name is required")
        @Size(max = 100, message = "Account name must be less than 100 characters")
        String name,
        
        @NotBlank(message = "Account type is required")
        String accountTypeCode,
        
        String accountTypeLabel,
        
        @NotNull(message = "Initial balance is required")
        BigDecimal currentBalance,
        
        @NotBlank(message = "Currency code is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be a 3-letter ISO code")
        String currencyCode,
        
        String currencySymbol,
        
        @Size(max = 100, message = "Bank name must be less than 100 characters")
        String bankName
) {
}
