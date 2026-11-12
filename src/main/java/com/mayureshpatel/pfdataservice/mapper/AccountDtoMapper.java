package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeDto;
import com.mayureshpatel.pfdataservice.dto.currency.CurrencyDto;
import com.mayureshpatel.pfdataservice.dto.user.UserDto;

import java.math.BigDecimal;

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
        BankName bankName = null;
        if (account.getUserId() != null) {
            user = new UserDto(account.getUserId(), null, null);
        }

        if (account.getBankCode() != null && !account.getBankCode().isBlank()) {
            bankName = BankName.fromString(account.getBankCode());
        }

        // money in and gain only mean something on a hand-entered balance
        BigDecimal moneyIn = account.isManualBalance() ? account.getMoneyIn() : null;
        BigDecimal gain = account.isManualBalance() ? gainOver(moneyIn, account.getLatestRecordedBalance()) : null;

        return new AccountDto(
                account.getId(),
                user,
                account.getName(),
                toTypeDto(account),
                account.getCurrentBalance(),
                toCurrencyDto(account),
                bankName,
                account.getVersion(),
                account.getBalanceMode(),
                moneyIn,
                gain
        );
    }

    /** Converts the account's type, or returns {@code null} when it has none. */
    private static AccountTypeDto toTypeDto(Account account) {
        if (account.getType() == null) {
            return null;
        }
        return new AccountTypeDto(
                account.getType().getCode(),
                account.getType().getLabel(),
                account.getType().isAsset(),
                account.getType().getSortOrder(),
                account.getType().isActive(),
                account.getType().getIcon(),
                account.getType().getColor()
        );
    }

    /** Converts the account's currency, or returns {@code null} when it has none. */
    private static CurrencyDto toCurrencyDto(Account account) {
        if (account.getCurrency() == null) {
            return null;
        }
        return new CurrencyDto(
                account.getCurrency().getCode(),
                account.getCurrency().getName(),
                account.getCurrency().getSymbol(),
                account.getCurrency().isActive());
    }

    /** The gain of the latest recorded balance over money in, or {@code null} when either is unknown. */
    private static BigDecimal gainOver(BigDecimal moneyIn, BigDecimal latestRecordedBalance) {
        if (moneyIn == null || latestRecordedBalance == null) {
            return null;
        }
        return latestRecordedBalance.subtract(moneyIn);
    }
}
