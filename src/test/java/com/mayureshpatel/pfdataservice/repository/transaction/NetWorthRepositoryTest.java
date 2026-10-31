package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.dto.report.explore.DateWindow;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import com.mayureshpatel.pfdataservice.repository.transaction.NetWorthRepository.IncomeExpense;
import com.mayureshpatel.pfdataservice.repository.transaction.NetWorthRepository.StoryFlows;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies {@code NetWorthRepository} against a real PostgreSQL instance. Every test inserts its own
 * transactions in 2031, far from the baseline fixture's dates, so the expected totals are exact.
 */
@Import(NetWorthRepository.class)
@DisplayName("NetWorthRepository Integration Tests (PostgreSQL)")
class NetWorthRepositoryTest extends BaseRepositoryTest {

    private static final Long USER_1 = 1L;
    private static final long CHECKING = 1L;
    private static final long SAVINGS = 2L;
    private static final long OTHER_USERS_ACCOUNT = 4L;
    private static final String EXPENSE = "EXPENSE";
    private static final String INCOME = "INCOME";
    private static final String TRANSFER_IN = "TRANSFER_IN";
    private static final String TRANSFER_OUT = "TRANSFER_OUT";
    private static final String ADJUSTMENT = "ADJUSTMENT";
    private static final String AMOUNT_100 = "100.00";
    private static final String AMOUNT_30 = "30.00";
    private static final String AMOUNT_20 = "20.00";
    private static final DateWindow YEAR_2031 = new DateWindow(utc(2031, 1, 1), utc(2032, 1, 1));

    @Autowired
    private NetWorthRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    private static OffsetDateTime utc(int year, int month, int day) {
        return LocalDate.of(year, month, day).atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private long insert(long accountId, String type, String amount, OffsetDateTime date) {
        return jdbcClient.sql("""
                        insert into transactions (account_id, amount, date, description, type)
                        values (:accountId, :amount, :date, 'net worth test', :type)
                        returning id
                        """)
                .param("accountId", accountId)
                .param("amount", new BigDecimal(amount))
                .param("date", date)
                .param("type", type)
                .query(Long.class)
                .single();
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    /** Checking: income 100 and expense 30 in march, a transfer out of 20 and an adjustment of 5 in april; savings: a transfer in of 20 in april; a deleted row and another user's row that must never count. */
    @BeforeEach
    void seed() {
        insert(CHECKING, INCOME, AMOUNT_100, utc(2031, 3, 5));
        insert(CHECKING, EXPENSE, AMOUNT_30, utc(2031, 3, 20));
        insert(CHECKING, TRANSFER_OUT, AMOUNT_20, utc(2031, 4, 2));
        insert(CHECKING, ADJUSTMENT, "5.00", utc(2031, 4, 3));
        insert(SAVINGS, TRANSFER_IN, AMOUNT_20, utc(2031, 4, 2));
        long deleted = insert(CHECKING, EXPENSE, "999.00", utc(2031, 3, 12));
        jdbcClient.sql("update transactions set deleted_at = now() where id = :id").param("id", deleted).update();
        insert(OTHER_USERS_ACCOUNT, EXPENSE, "777.00", utc(2031, 3, 13));
    }

    @Test
    @DisplayName("should net each account's flows per month with the balance sign rules")
    void shouldNetEachAccountsFlowsPerMonthWithTheBalanceSignRules() {
        // act
        Map<Long, Map<YearMonth, BigDecimal>> flows = repository.findMonthlyNetFlow(USER_1, LocalDate.of(2031, 1, 1));

        // assert & verify
        assertMoney("70.00", flows.get(CHECKING).get(YearMonth.of(2031, 3)));
        assertMoney("-15.00", flows.get(CHECKING).get(YearMonth.of(2031, 4)));
        assertMoney("20.00", flows.get(SAVINGS).get(YearMonth.of(2031, 4)));
    }

    @Test
    @DisplayName("should include a transaction made on the last day of a month in that month")
    void shouldIncludeATransactionMadeOnTheLastDayOfAMonthInThatMonth() {
        // arrange
        insert(SAVINGS, EXPENSE, "10.00", OffsetDateTime.of(2031, 3, 31, 23, 30, 0, 0, ZoneOffset.UTC));

        // act
        Map<Long, Map<YearMonth, BigDecimal>> flows = repository.findMonthlyNetFlow(USER_1, LocalDate.of(2031, 1, 1));

        // assert & verify
        assertMoney("-10.00", flows.get(SAVINGS).get(YearMonth.of(2031, 3)));
    }

    @Test
    @DisplayName("should only look from the given month onward")
    void shouldOnlyLookFromTheGivenMonthOnward() {
        // act
        Map<Long, Map<YearMonth, BigDecimal>> flows = repository.findMonthlyNetFlow(USER_1, LocalDate.of(2031, 4, 1));

        // assert & verify
        assertFalse(flows.get(CHECKING).containsKey(YearMonth.of(2031, 3)));
        assertMoney("-15.00", flows.get(CHECKING).get(YearMonth.of(2031, 4)));
    }

    @Test
    @DisplayName("should split deposits, withdrawals and other per account")
    void shouldSplitDepositsWithdrawalsAndOtherPerAccount() {
        // act
        Map<Long, StoryFlows> story = repository.findStoryFlows(USER_1, YEAR_2031);

        // assert & verify
        StoryFlows checking = story.get(CHECKING);
        assertMoney(AMOUNT_100, checking.deposits());
        assertMoney("50.00", checking.withdrawals());
        assertMoney("5.00", checking.interestAndAdjustments());
        assertMoney(AMOUNT_20, story.get(SAVINGS).deposits());
    }

    @Test
    @DisplayName("should put adjustments and plain transfers in the other bucket")
    void shouldPutAdjustmentsAndPlainTransfersInTheOtherBucket() {
        // arrange
        insert(SAVINGS, "TRANSFER", "7.00", utc(2031, 5, 1));
        insert(SAVINGS, ADJUSTMENT, "-3.00", utc(2031, 5, 2));

        // act
        StoryFlows savings = repository.findStoryFlows(USER_1, YEAR_2031).get(SAVINGS);

        // assert & verify
        assertMoney("4.00", savings.interestAndAdjustments());
        assertMoney(AMOUNT_20, savings.deposits());
    }

    @Test
    @DisplayName("should total income and expense but not transfers or adjustments")
    void shouldTotalIncomeAndExpenseButNotTransfersOrAdjustments() {
        // act
        IncomeExpense totals = repository.findIncomeExpense(USER_1, List.of(), YEAR_2031);

        // assert & verify
        assertMoney(AMOUNT_100, totals.income());
        assertMoney(AMOUNT_30, totals.expense());
        assertMoney(AMOUNT_30, repository.findExpense(USER_1, List.of(), YEAR_2031));
    }

    @Test
    @DisplayName("should limit income and expense to the given accounts")
    void shouldLimitIncomeAndExpenseToTheGivenAccounts() {
        // arrange
        insert(SAVINGS, EXPENSE, "8.00", utc(2031, 6, 1));

        // act
        IncomeExpense savings = repository.findIncomeExpense(USER_1, List.of(SAVINGS), YEAR_2031);

        // assert & verify
        assertMoney("0", savings.income());
        assertMoney("8.00", savings.expense());
    }

    @Test
    @DisplayName("should never count soft-deleted rows or another user's accounts")
    void shouldNeverCountSoftDeletedRowsOrAnotherUsersAccounts() {
        // act
        IncomeExpense totals = repository.findIncomeExpense(USER_1, List.of(), YEAR_2031);
        Map<Long, Map<YearMonth, BigDecimal>> flows = repository.findMonthlyNetFlow(USER_1, LocalDate.of(2031, 1, 1));

        // assert & verify -- the 999 deleted expense and the 777 other-user expense are absent
        assertMoney(AMOUNT_30, totals.expense());
        assertFalse(flows.containsKey(OTHER_USERS_ACCOUNT));
    }

    @Test
    @DisplayName("should respect the half-open window at both ends")
    void shouldRespectTheHalfOpenWindowAtBothEnds() {
        // arrange
        DateWindow june = new DateWindow(utc(2031, 6, 1), utc(2031, 7, 1));
        insert(SAVINGS, EXPENSE, "1.00", utc(2031, 6, 1));
        insert(SAVINGS, EXPENSE, "2.00", utc(2031, 6, 30));
        insert(SAVINGS, EXPENSE, "4.00", utc(2031, 7, 1));
        insert(SAVINGS, EXPENSE, "8.00", utc(2031, 5, 31));

        // act
        IncomeExpense totals = repository.findIncomeExpense(USER_1, List.of(SAVINGS), june);

        // assert & verify -- the first instant is in, the end instant is out
        assertMoney("3.00", totals.expense());
    }
}
