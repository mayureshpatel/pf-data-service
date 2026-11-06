package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import com.mayureshpatel.pfdataservice.service.NetWorthBalances;
import com.mayureshpatel.pfdataservice.service.SnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pins the month-end boundary shared by the snapshot math and the Reports net worth view (PF-895) against a
 * real PostgreSQL instance. Every transaction is inserted in 2033, far from the baseline fixture's dates, so
 * the expected balances are exact: anything dated on the last day of a month, even 23:59:59 UTC, belongs to
 * that month, and an instant at 00:00 UTC on the first of the next month does not.
 */
@Import({TransactionRepository.class, NetWorthRepository.class})
@DisplayName("Snapshot month-end boundary Integration Tests (PostgreSQL)")
class SnapshotMonthEndBoundaryTest extends BaseRepositoryTest {

    private static final long CHECKING = 1L;
    private static final Long USER_1 = 1L;
    private static final BigDecimal CURRENT_BALANCE = new BigDecimal("1000.00");
    private static final LocalDate JANUARY_END = LocalDate.of(2033, 1, 31);
    private static final LocalDate TODAY = LocalDate.of(2033, 6, 1);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private NetWorthRepository netWorthRepository;

    @Autowired
    private JdbcClient jdbcClient;

    private void insert(String type, String amount, String instant) {
        jdbcClient.sql("""
                        insert into transactions (account_id, amount, date, description, type)
                        values (:accountId, :amount, :date, 'month end boundary test', :type)
                        """)
                .param("accountId", CHECKING)
                .param("amount", new BigDecimal(amount))
                .param("date", OffsetDateTime.parse(instant))
                .param("type", type)
                .update();
    }

    /** January: income 20 at 00:00 on the 31st and an expense of 10 at 23:59:59 on the 31st; February: an expense of 50 at 00:00 on the 1st. */
    @BeforeEach
    void seed() {
        insert("INCOME", "20.00", "2033-01-31T00:00:00Z");
        insert("EXPENSE", "10.00", "2033-01-31T23:59:59Z");
        insert("EXPENSE", "50.00", "2033-02-01T00:00:00Z");
    }

    private Account account() {
        return Account.builder().id(CHECKING).userId(USER_1).currentBalance(CURRENT_BALANCE).build();
    }

    @Test
    @DisplayName("bug regression: should count last-day transactions inside the month and the first instant of the next month outside it (PF-895)")
    void shouldKeepTheLastDayInsideTheMonth() {
        // arrange
        SnapshotService snapshotService = new SnapshotService(null, null, transactionRepository);

        // act
        BigDecimal balance = snapshotService.calculateEndOfMonthBalance(account(), JANUARY_END);

        // assert & verify -- only the february 1st expense of 50 is after january, so 1000 + 50
        assertEquals(0, new BigDecimal("1050.00").compareTo(balance), "expected 1050.00 but was " + balance);
    }

    @Test
    @DisplayName("bug regression: should agree with the net worth view's balance at the same month end (PF-895)")
    void shouldAgreeWithTheNetWorthView() {
        // arrange
        SnapshotService snapshotService = new SnapshotService(null, null, transactionRepository);
        Map<YearMonth, BigDecimal> flows = netWorthRepository.findMonthlyNetFlow(USER_1, LocalDate.of(2033, 1, 1)).get(CHECKING);
        assertNotNull(flows);

        // act
        BigDecimal snapshot = snapshotService.calculateEndOfMonthBalance(account(), JANUARY_END);
        BigDecimal netWorth = NetWorthBalances.balanceAt(CURRENT_BALANCE, flows, JANUARY_END, TODAY);

        // assert & verify
        assertEquals(0, netWorth.compareTo(snapshot), "snapshot " + snapshot + " but net worth view " + netWorth);
    }
}
