package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeDto;
import com.mayureshpatel.pfdataservice.dto.currency.CurrencyDto;
import com.mayureshpatel.pfdataservice.dto.user.UserDto;

/** Converts the {@link Account} domain object into its API-facing {@link AccountDto}. */
public final class AccountDtoMapper {

    private AccountDtoMapper() {
    }

    /**
     * Hand-builds the nested {@code UserDto}/{@code AccountTypeDto}/{@code CurrencyDto} inline
     * rather than delegating to their own dedicated mappers -- the existing {@code todo} below
     * flags this as worth refactoring, not a deliberate inconsistency with the rest of this
     * package's mappers.
     *
     * @param account the domain account to convert, or {@code null}
     * @return the equivalent DTO, or {@code null} if {@code account} was {@code null}
     */
    // todo: refactor by using specific dto mappers
    public static AccountDto toDto(Account account) {
        if (account == null) {
            return null;
        }

        UserDto user = null;
        AccountTypeDto type = null;
        CurrencyDto currency = null;
        BankName bankName = null;
        if (account.getUserId() != null) {
            user = new UserDto(account.getUserId(), null, null);
        }

        if (account.getType() != null) {
            type = new AccountTypeDto(
                    account.getType().getCode(),
                    account.getType().getLabel(),
                    account.getType().isAsset(),
                    account.getType().getSortOrder(),
                    account.getType().isActive(),
                    account.getType().getIcon(),
                    account.getType().getColor()
            );
        }

        if (account.getCurrency() != null) {
            currency = new CurrencyDto(
                    account.getCurrency().getCode(),
                    account.getCurrency().getName(),
                    account.getCurrency().getSymbol(),
                    account.getCurrency().isActive());
        }

        if (account.getBankCode() != null && !account.getBankCode().isBlank()) {
            bankName = BankName.fromString(account.getBankCode());
        }

        return new AccountDto(
                account.getId(),
                user,
                account.getName(),
                type,
                account.getCurrentBalance(),
                currency,
                bankName,
                account.getVersion()
        );
    }
}
