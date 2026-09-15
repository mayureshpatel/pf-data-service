package com.mayureshpatel.pfdataservice.repository.budget;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetCreateRequest;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetStatusDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.repository.TimezoneBoundaryRepositoryTest;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PF-836 regression: {@code FIND_BUDGET_STATUS_BY_USER_ID_AND_MONTH_AND_YEAR}'s {@code spending}
 * CTE used {@code EXTRACT(MONTH FROM t.date)}, which implicitly converts the {@code timestamptz}
 * value using the database session's timezone before extracting -- under a non-UTC session (see
 * {@link TimezoneBoundaryRepositoryTest}), a transaction stored at UTC midnight on the 1st of a
 * month resolves to the *previous* month's last day, silently moving it out of the budget it
 * actually belongs to.
 */
@Import({BudgetRepository.class, TransactionRepository.class})
@DisplayName("BudgetRepository Timezone Boundary (PF-836)")
class BudgetStatusTimezoneTest extends TimezoneBoundaryRepositoryTest {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private static final Long USER_1 = 1L;
    private static final Long ACCOUNT_1 = 1L;
    private static final Long CAT_RENT = 6L; // from baseline

    @Test
    @DisplayName("a transaction dated the 1st of a month at UTC midnight counts toward that month, not the prior one")
    void shouldCountFirstOfMonthTransactionInTheCorrectMonth() {
        // arrange -- a real transaction dated exactly midnight UTC on June 1st, matching this
        // app's own storage convention for every CSV-imported transaction. June/May 2026 are
        // deliberately outside test-data-baseline.sql's own recurring "Monthly Rent" fixture
        // (2025-09 through 2026-02 only), so this test's own data is the only Rent spending in
        // either month -- no accidental collision with baseline fixture data.
        TransactionCreateRequest firstOfMonth = TransactionCreateRequest.builder()
                .accountId(ACCOUNT_1)
                .categoryId(CAT_RENT)
                .amount(new BigDecimal("544.45"))
                .transactionDate(OffsetDateTime.parse("2026-06-01T00:00:00Z"))
                .description("Rent - June")
                .type(TransactionType.EXPENSE.name())
                .build();
        transactionRepository.insert(firstOfMonth);

        budgetRepository.insert(BudgetCreateRequest.builder()
                .userId(USER_1).categoryId(CAT_RENT).amount(new BigDecimal("1000.00")).month(6).year(2026).build());
        budgetRepository.insert(BudgetCreateRequest.builder()
                .userId(USER_1).categoryId(CAT_RENT).amount(new BigDecimal("1000.00")).month(5).year(2026).build());

        // act
        List<BudgetStatusDto> juneStatus = budgetRepository.findBudgetStatusByUserIdAndMonthAndYear(USER_1, 6, 2026);
        List<BudgetStatusDto> mayStatus = budgetRepository.findBudgetStatusByUserIdAndMonthAndYear(USER_1, 5, 2026);

        // assert & verify -- spent in June, not phantom-counted into May
        BigDecimal juneSpent = juneStatus.stream()
                .filter(s -> s.category().id().equals(CAT_RENT))
                .findFirst().orElseThrow().spentAmount();
        BigDecimal maySpent = mayStatus.stream()
                .filter(s -> s.category().id().equals(CAT_RENT))
                .findFirst().orElseThrow().spentAmount();

        assertEquals(0, new BigDecimal("544.45").compareTo(juneSpent),
                "expected the June 1st transaction to count toward June's budget, got: " + juneSpent);
        assertEquals(0, BigDecimal.ZERO.compareTo(maySpent),
                "expected nothing spent in May, got: " + maySpent);
    }
}
