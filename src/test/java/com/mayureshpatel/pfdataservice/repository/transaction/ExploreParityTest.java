package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreCompare;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreFilter;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreGroupBy;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreMeasure;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreResultDto;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreSide;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import com.mayureshpatel.pfdataservice.service.ExploreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/** The explorer's totals must equal what the Dashboard reports for the same period and definition. */
@Import({ExploreService.class, ExploreRepository.class, TransactionReportRepository.class})
@DisplayName("Explorer and Dashboard parity (PostgreSQL)")
class ExploreParityTest extends BaseRepositoryTest {

    private static final Long USER_1 = 1L;
    private static final LocalDate START = LocalDate.of(2025, 9, 1);
    private static final LocalDate END = LocalDate.of(2026, 4, 1);

    @Autowired
    private ExploreService explorer;

    @Autowired
    private TransactionReportRepository dashboardQueries;

    private BigDecimal explore(ExploreMeasure measure, ExploreGroupBy groupBy) {
        ExploreResultDto result = explorer.explore(USER_1, measure, ExploreSide.SPENDING, groupBy, START, END,
                ExploreCompare.NONE, ExploreFilter.none(), LocalDate.of(2027, 1, 1));
        return result.total();
    }

    private BigDecimal dashboard(TransactionType type) {
        OffsetDateTime start = START.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = END.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1000);
        return dashboardQueries.getSumByDateRange(USER_1, start, end, type);
    }

    @Test
    @DisplayName("should equal the Dashboard's expense and income totals for the same period")
    void shouldEqualDashboardTotals() {
        // act & assert & verify
        BigDecimal expense = dashboard(TransactionType.EXPENSE);
        BigDecimal income = dashboard(TransactionType.INCOME);
        assertTrue(expense.signum() > 0, "the baseline must have expenses in the window");
        assertEquals(0, expense.compareTo(explore(ExploreMeasure.SPENDING, ExploreGroupBy.CATEGORY)));
        assertEquals(0, expense.compareTo(explore(ExploreMeasure.SPENDING, ExploreGroupBy.MERCHANT)));
        assertEquals(0, expense.compareTo(explore(ExploreMeasure.SPENDING, ExploreGroupBy.TAG)));
        assertEquals(0, income.compareTo(explore(ExploreMeasure.INCOME, ExploreGroupBy.ACCOUNT)));
        assertEquals(0, income.subtract(expense).compareTo(explore(ExploreMeasure.NET, ExploreGroupBy.MONTH)));
    }
}
