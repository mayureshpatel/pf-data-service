package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The month-end balance source for one net worth request: every account's monthly net flows and today's date.
 * It reads balances through {@link NetWorthBalances} and totals them into assets, debts and liquid cash.
 */
final class NetWorthBooks {

    private static final Set<String> LIQUID_TYPES = Set.of("CHECKING", "SAVINGS", "CASH");

    private final Map<Long, Map<YearMonth, BigDecimal>> flows;
    private final LocalDate today;

    NetWorthBooks(Map<Long, Map<YearMonth, BigDecimal>> flows, LocalDate today) {
        this.flows = flows;
        this.today = today;
    }

    /** Assets, debts and liquid cash of some accounts at one month end. */
    record Position(BigDecimal assets, BigDecimal debts, BigDecimal liquid) {

        BigDecimal netWorth() {
            return assets.subtract(debts);
        }
    }

    /**
     * @param account  the account
     * @param monthEnd the month end
     * @return the account's balance at the month end, or null when the account did not exist yet
     */
    BigDecimal balance(Account account, LocalDate monthEnd) {
        if (!NetWorthBalances.existedAsOf(account.getAudit() == null ? null : account.getAudit().getCreatedAt(), monthEnd)) {
            return null;
        }
        return rawBalance(account, monthEnd);
    }

    /**
     * @param account  the account
     * @param monthEnd the month end
     * @return the account's balance at the month end from its flows, even before the account was created
     */
    BigDecimal rawBalance(Account account, LocalDate monthEnd) {
        return NetWorthBalances.balanceAt(account.getCurrentBalance(), flows.getOrDefault(account.getId(), Map.of()), monthEnd, today);
    }

    /**
     * @param accounts the accounts to total
     * @param monthEnd the month end
     * @return assets, debts (positive when owed) and liquid cash at the month end; accounts that did not exist yet add nothing
     */
    Position position(List<Account> accounts, LocalDate monthEnd) {
        BigDecimal assets = BigDecimal.ZERO;
        BigDecimal owed = BigDecimal.ZERO;
        BigDecimal liquid = BigDecimal.ZERO;
        for (Account account : accounts) {
            BigDecimal balance = balance(account, monthEnd);
            if (balance == null) {
                continue;
            }
            if (account.getType().isAsset()) {
                assets = assets.add(balance);
            } else {
                owed = owed.subtract(balance);
            }
            if (LIQUID_TYPES.contains(account.getType().getCode())) {
                liquid = liquid.add(balance);
            }
        }
        return new Position(assets, owed, liquid);
    }

    /**
     * @param accounts the accounts to total
     * @param monthEnd the month end
     * @return the signed sum of their balances at the month end (zero when none existed yet)
     */
    BigDecimal sum(List<Account> accounts, LocalDate monthEnd) {
        BigDecimal total = BigDecimal.ZERO;
        for (Account account : accounts) {
            BigDecimal balance = balance(account, monthEnd);
            total = balance == null ? total : total.add(balance);
        }
        return total;
    }

    /**
     * @param accounts the accounts to total
     * @param monthEnd the month end
     * @return the signed sum of their balances, or null when none of them existed yet
     */
    BigDecimal sumOrNull(List<Account> accounts, LocalDate monthEnd) {
        boolean anyExisted = accounts.stream().anyMatch(a -> balance(a, monthEnd) != null);
        return anyExisted ? sum(accounts, monthEnd) : null;
    }
}
