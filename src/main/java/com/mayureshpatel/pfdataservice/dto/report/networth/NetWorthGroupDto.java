package com.mayureshpatel.pfdataservice.dto.report.networth;

import java.math.BigDecimal;
import java.util.List;

/**
 * One row of the breakdown: an account type or a single account. Balances stay raw and signed (a card that is
 * owed is negative); the caller decides what is good from {@code kind}.
 *
 * @param key               the type code, or the account id as text
 * @param label             the display name
 * @param kind              {@code ASSET} or {@code DEBT}
 * @param typeCode          the account type's code
 * @param accountCount      how many accounts the row holds
 * @param balance           the signed balance at the breakdown's as-of month end
 * @param previousBalance   the signed balance at the month end before the range
 * @param comparisonBalance the signed balance at the end of the comparison window, or null when not comparing
 * @param series            the signed balance at each point, null where none of the row's accounts existed yet
 * @param story             the account's story over the range (account rows only, otherwise null)
 */
public record NetWorthGroupDto(String key, String label, String kind, String typeCode, int accountCount,
                               BigDecimal balance, BigDecimal previousBalance, BigDecimal comparisonBalance,
                               List<BigDecimal> series, NetWorthStoryDto story) {
}
