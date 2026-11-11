package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the one liability sign rule in {@code NetWorthBooks}: a liability owes {@code abs(balance)}, however the
 * balance happens to be stored.
 */
@DisplayName("NetWorthBooks Unit Tests")
class NetWorthBooksTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);
    private static final OffsetDateTime LONG_AGO = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private final NetWorthBooks books = new NetWorthBooks(Map.of(), TODAY);

    private static Account account(long id, String typeCode, boolean asset, String balance) {
        return Account.builder()
                .id(id).userId(1L).name(typeCode + id)
                .type(AccountType.builder().code(typeCode).label(typeCode).asset(asset).build())
                .currentBalance(new BigDecimal(balance))
                .audit(TableAudit.builder().createdAt(LONG_AGO).build())
                .build();
    }

    private NetWorthBooks.Position position(Account... accounts) {
        return books.position(List.of(accounts), TODAY);
    }

    @Test
    @DisplayName("should contribute -500 to net worth for a liability stored as +500")
    void position_liabilityStoredPositive() {
        NetWorthBooks.Position position = position(account(1, "CREDIT_CARD", false, "500.00"));

        assertEquals(0, new BigDecimal("500.00").compareTo(position.debts()));
        assertEquals(0, new BigDecimal("-500.00").compareTo(position.netWorth()));
    }

    @Test
    @DisplayName("should contribute -500 to net worth for a liability stored as -500")
    void position_liabilityStoredNegative() {
        NetWorthBooks.Position position = position(account(1, "CREDIT_CARD", false, "-500.00"));

        assertEquals(0, new BigDecimal("500.00").compareTo(position.debts()));
        assertEquals(0, new BigDecimal("-500.00").compareTo(position.netWorth()));
    }

    @Test
    @DisplayName("should count a LOAN account as debt and not as liquid cash")
    void position_loanIsDebtNotLiquid() {
        NetWorthBooks.Position position = position(account(1, "LOAN", false, "-12000.00"));

        assertEquals(0, new BigDecimal("12000.00").compareTo(position.debts()));
        assertEquals(0, BigDecimal.ZERO.compareTo(position.assets()));
        assertEquals(0, BigDecimal.ZERO.compareTo(position.liquid()));
    }
}
