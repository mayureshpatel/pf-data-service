package com.mayureshpatel.pfdataservice.repository.budget;

import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetCreateRequest;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetStatusDto;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetUpdateRequest;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SoftDeleteSupport;
import com.mayureshpatel.pfdataservice.repository.budget.mapper.BudgetRowMapper;
import com.mayureshpatel.pfdataservice.repository.budget.mapper.BudgetStatusRowMapper;
import com.mayureshpatel.pfdataservice.repository.budget.query.BudgetQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/** JDBC-backed persistence for {@link Budget}, including the budget-status aggregation query. */
@Repository("jdbcBudgetRepository")
@RequiredArgsConstructor
public class BudgetRepository implements JdbcRepository<Budget, Long>, SoftDeleteSupport {

    private static final ZoneOffset UTC_ZONE = ZoneOffset.UTC;

    private final JdbcClient jdbcClient;
    private final BudgetRowMapper rowMapper;
    private final BudgetStatusRowMapper budgetStatusRowMapper;

    @Override
    public Optional<Budget> findById(Long id) {
        return jdbcClient.sql(BudgetQueries.FIND_BY_ID)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    /**
     * @param request the budget to create
     * @return the generated budget id
     */
    public int insert(BudgetCreateRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(BudgetQueries.INSERT)
                .param("userId", request.getUserId())
                .param("categoryId", request.getCategoryId())
                .param("amount", request.getAmount())
                .param("month", request.getMonth())
                .param("year", request.getYear())
                .update(keyHolder);

        return keyHolder.getKey().intValue();
    }

    /**
     * @param request the amount to update, identified by {@code request.getId()}
     * @return the number of rows updated (0 or 1)
     */
    public int update(BudgetUpdateRequest request) {
        return jdbcClient.sql(BudgetQueries.UPDATE)
                .param("amount", request.getAmount())
                .param("id", request.getId())
                .update();
    }

    @Override
    public int delete(Budget budget) {
        if (budget.getId() != null) {
            return deleteById(budget.getId());
        }

        return 0;
    }

    @Override
    public int deleteById(Long id) {
        return jdbcClient.sql(BudgetQueries.DELETE)
                .param("id", id)
                .update();
    }

    /**
     * @param userId the owning user's id
     * @param month  the budget month (1-12)
     * @param year   the budget year
     * @return every non-deleted budget the user has for that specific month/year
     */
    public List<Budget> findByUserIdAndMonthAndYearAndDeletedAtIsNull(Long userId, Integer month, Integer year) {
        return jdbcClient.sql(BudgetQueries.FIND_BY_USER_ID_AND_MONTH_AND_YEAR)
                .param("userId", userId)
                .param("month", month)
                .param("year", year)
                .query(rowMapper)
                .list();
    }

    /**
     * Unpaginated form of {@link #findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(Long, Pageable)}
     * -- returns every one of the user's non-deleted budgets across all periods at once.
     *
     * @param userId the owning user's id
     * @return every non-deleted budget the user has, most recent period first
     */
    public List<Budget> findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(Long userId) {
        return jdbcClient.sql(BudgetQueries.FIND_BY_USER_ID_ORDER_BY_YEAR_DESC_MONTH_DESC)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    /**
     * Paginated version of {@link #findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc}
     * (PF-320) -- the "manage all budgets" view has never exposed column sorting, so
     * {@code pageable} is used for page/size only; ordering stays fixed at year/month descending.
     *
     * @param userId   the user id
     * @param pageable the requested page and size
     * @return the requested page of the user's budgets, most recent period first
     */
    public Page<Budget> findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(Long userId, Pageable pageable) {
        long total = jdbcClient.sql(BudgetQueries.COUNT_BY_USER_ID)
                .param("userId", userId)
                .query(Long.class)
                .single();

        // Pageable.unpaged() throws UnsupportedOperationException from getPageSize()/getOffset()
        // -- only append limit/offset when the caller actually wants a bounded page.
        String pageSql = BudgetQueries.FIND_BY_USER_ID_ORDER_BY_YEAR_DESC_MONTH_DESC;
        var jdbcCall = jdbcClient.sql(pageable.isPaged() ? pageSql + " limit :limit offset :offset" : pageSql)
                .param("userId", userId);
        if (pageable.isPaged()) {
            jdbcCall = jdbcCall.param("limit", pageable.getPageSize()).param("offset", pageable.getOffset());
        }
        List<Budget> content = jdbcCall.query(rowMapper).list();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * @param userId     the owning user's id
     * @param categoryId the budgeted category
     * @param month      the budget month (1-12)
     * @param year       the budget year
     * @return the matching budget, if one exists (at most one, per the unique period/category pair)
     */
    public Optional<Budget> findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(
            Long userId, Long categoryId, Integer month, Integer year) {
        return jdbcClient.sql(BudgetQueries.FIND_BY_USER_ID_AND_CATEGORY_ID_AND_MONTH_AND_YEAR)
                .param("userId", userId)
                .param("categoryId", categoryId)
                .param("month", month)
                .param("year", year)
                .query(rowMapper)
                .optional();
    }

    /**
     * Combines each budgeted category's target amount with its actual spending for the period --
     * see {@link com.mayureshpatel.pfdataservice.repository.budget.query.BudgetQueries#FIND_BUDGET_STATUS_BY_USER_ID_AND_MONTH_AND_YEAR}
     * for how unbudgeted-but-spent categories are folded in too.
     *
     * @param userId the owning user's id
     * @param month  the reporting month (1-12)
     * @param year   the reporting year
     * @return one status row per category with either a budget or spending in the period
     */
    public List<BudgetStatusDto> findBudgetStatusByUserIdAndMonthAndYear(Long userId, Integer month, Integer year) {
        // explicit UTC bounds for the spending CTE, rather than EXTRACT(YEAR/MONTH FROM t.date)
        // -- EXTRACT() on a timestamptz implicitly converts using the database session's
        // timezone first (America/New_York in production, see application.yml), which silently
        // moves a UTC-midnight transaction on the 1st of a month into the prior month. See
        // PF-836. `:month`/`:year` are still passed too -- they also match plain-integer
        // `budgets.month`/`budgets.year` columns elsewhere in the same query, which aren't
        // timestamptz and aren't affected by this bug.
        OffsetDateTime startDate = ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, UTC_ZONE).toOffsetDateTime();
        OffsetDateTime endDate = startDate.plusMonths(1);

        return jdbcClient.sql(BudgetQueries.FIND_BUDGET_STATUS_BY_USER_ID_AND_MONTH_AND_YEAR)
                .param("userId", userId)
                .param("month", month)
                .param("year", year)
                .param("startDate", startDate)
                .param("endDate", endDate)
                .query(budgetStatusRowMapper)
                .list();
    }

    /**
     * @param categoryId the category to check
     * @return the number of non-deleted budgets (across all users/periods) referencing this category
     */
    public long countByCategoryIdAndDeletedAtIsNull(Long categoryId) {
        return jdbcClient.sql(BudgetQueries.COUNT_BY_CATEGORY_ID)
                .param("categoryId", categoryId)
                .query(Long.class)
                .single();
    }
}
