package com.mayureshpatel.pfdataservice.dto.account;

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
        String name,
        String accountTypeCode,
        String accountTypeLabel,
        BigDecimal currentBalance,
        String currencyCode,
        String currencySymbol,
        String bankName
) {
}
