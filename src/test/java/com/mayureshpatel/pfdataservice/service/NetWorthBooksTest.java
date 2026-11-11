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

import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthGroupBy;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthGroupDto;

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

    @Test
    @DisplayName("should sum a liability stored as +500 and one stored as -500 to the same -500")
    void sum_liabilityStoredEitherWay() {
        BigDecimal positive = books.sum(List.of(account(1, "CREDIT_CARD", false, "500.00")), TODAY);
        BigDecimal negative = books.sum(List.of(account(2, "CREDIT_CARD", false, "-500.00")), TODAY);

        assertEquals(0, new BigDecimal("-500.00").compareTo(positive));
        assertEquals(0, new BigDecimal("-500.00").compareTo(negative));
        assertEquals(0, new BigDecimal("-500.00").compareTo(books.sumOrNull(List.of(account(1, "LOAN", false, "500.00")), TODAY)));
    }

    @Test
    @DisplayName("should leave an asset sum signed as stored")
    void sum_assetUnchanged() {
        assertEquals(0, new BigDecimal("-20.00").compareTo(books.sum(List.of(account(1, "CHECKING", true, "-20.00")), TODAY)));
    }

    @Test
    @DisplayName("should give a +500 and a -500 liability the identical DEBT group balance and series")
    void group_liabilityStoredEitherWay() {
        LocalDate monthEnd = LocalDate.of(2026, 10, 31);
        NetWorthGroupBuilder.Context context = new NetWorthGroupBuilder.Context(List.of(TODAY, monthEnd), TODAY, TODAY, TODAY);

        NetWorthGroupDto positive = NetWorthGroupBuilder.build(NetWorthGroupBy.TYPE, List.of(account(1, "CREDIT_CARD", false, "500.00")), books, context, Map.of()).get(0);
        NetWorthGroupDto negative = NetWorthGroupBuilder.build(NetWorthGroupBy.TYPE, List.of(account(2, "CREDIT_CARD", false, "-500.00")), books, context, Map.of()).get(0);

        assertEquals(0, new BigDecimal("-500.00").compareTo(positive.balance()));
        assertEquals(0, positive.balance().compareTo(negative.balance()));
        assertEquals(0, positive.previousBalance().compareTo(negative.previousBalance()));
        assertEquals(0, positive.comparisonBalance().compareTo(negative.comparisonBalance()));
        assertEquals(positive.series().size(), negative.series().size());
        for (int i = 0; i < positive.series().size(); i++) {
            assertEquals(0, positive.series().get(i).compareTo(negative.series().get(i)));
        }
    }
}
