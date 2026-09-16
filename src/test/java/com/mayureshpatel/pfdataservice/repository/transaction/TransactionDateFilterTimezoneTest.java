package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.repository.TimezoneBoundaryRepositoryTest;
import com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PF-828 regression: {@code TransactionSpecification.buildWhereClause()} binds
 * {@code filter.startDate()}/{@code filter.endDate()} as bare {@code LocalDate} parameters
 * against the {@code timestamptz} {@code transactions.date} column -- resolved using the database
 * session's timezone (see {@link TimezoneBoundaryRepositoryTest}), not UTC. A transaction stored
 * at UTC midnight on a range's start date falls *before* that non-UTC-shifted lower bound and is
 * silently excluded from a query for "that exact date."
 */
@Import(TransactionRepository.class)
@DisplayName("TransactionRepository Date Filter Timezone Boundary (PF-828)")
class TransactionDateFilterTimezoneTest extends TimezoneBoundaryRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_1 = 1L;

    @Test
    @DisplayName("a transaction dated at UTC midnight is included when querying that exact date as both start and end")
    void shouldIncludeUtcMidnightTransactionOnItsOwnDate() {
        // arrange -- matches this app's own storage convention: every CSV-imported transaction is
        // dated at UTC midnight, never a "local" time
        TransactionCreateRequest onTheBoundary = TransactionCreateRequest.builder()
                .accountId(ACCOUNT_1)
                .amount(new BigDecimal("29.99"))
                .transactionDate(OffsetDateTime.parse("2026-03-01T00:00:00Z"))
                .description("Hulu")
                .type(TransactionType.EXPENSE.name())
                .build();
        transactionRepository.insert(onTheBoundary);

        TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                null, null, null, null, null, null, null,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1), null
        );

        // act
        Page<Transaction> result = transactionRepository.findAll(
                TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 10)
        );

        // assert & verify
        assertTrue(result.getContent().stream().anyMatch(t -> t.getDescription().equals("Hulu")),
                "expected the UTC-midnight transaction to be included when querying its own date, got: "
                        + result.getContent().stream().map(Transaction::getDescription).toList());
    }

    @Test
    @DisplayName("findMonthlySums buckets a UTC-midnight transaction into its own calendar month, not the prior one")
    void shouldBucketUtcMidnightTransactionIntoCorrectMonth() {
        // arrange -- FIND_MONTHLY_SUMS used EXTRACT(YEAR/MONTH FROM transactions.date) directly
        // in its SELECT/GROUP BY, a second instance of the same bug class in a different clause
        // (grouping, not filtering) -- backs DashboardService.getCashFlowTrend(), the Dashboard's
        // own Cash Flow Trend widget.
        TransactionCreateRequest firstOfMonth = TransactionCreateRequest.builder()
                .accountId(ACCOUNT_1)
                .amount(new BigDecimal("29.99"))
                .transactionDate(OffsetDateTime.parse("2026-06-01T00:00:00Z"))
                .description("Hulu - June")
                .type(TransactionType.EXPENSE.name())
                .build();
        transactionRepository.insert(firstOfMonth);

        // act
        List<Object[]> sums = transactionRepository.findMonthlySums(USER_ID, LocalDate.of(2026, 6, 1));

        // assert & verify -- bucketed as June (month=6), not phantom-bucketed into May (month=5)
        boolean bucketedInJune = sums.stream().anyMatch(row ->
                ((Number) row[0]).intValue() == 2026 && ((Number) row[1]).intValue() == 6
                        && "EXPENSE".equals(row[2]) && new BigDecimal("29.99").compareTo((BigDecimal) row[3]) == 0);
        boolean leakedIntoMay = sums.stream().anyMatch(row ->
                ((Number) row[0]).intValue() == 2026 && ((Number) row[1]).intValue() == 5);

        assertTrue(bucketedInJune, "expected a 2026/June/EXPENSE/29.99 row, got: " + rowsToString(sums));
        assertTrue(!leakedIntoMay, "expected no May 2026 row at all, got: " + rowsToString(sums));
    }

    private static String rowsToString(List<Object[]> rows) {
        return rows.stream()
                .map(row -> "[" + row[0] + "/" + row[1] + "/" + row[2] + "=" + row[3] + "]")
                .toList().toString();
    }
}
