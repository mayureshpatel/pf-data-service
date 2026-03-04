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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("jdbcBudgetRepository")
@RequiredArgsConstructor
public class BudgetRepository implements JdbcRepository<Budget, Long>, SoftDeleteSupport {

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

    public int insert(BudgetCreateRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        return jdbcClient.sql(BudgetQueries.INSERT)
                .param("userId", request.getUserId())
                .param("categoryId", request.getCategoryId())
                .param("amount", request.getAmount())
                .param("month", request.getMonth())
                .param("year", request.getYear())
                .update(keyHolder);
    }

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

    public List<Budget> findByUserIdAndMonthAndYearAndDeletedAtIsNull(Long userId, Integer month, Integer year) {
        return jdbcClient.sql(BudgetQueries.FIND_BY_USER_ID_AND_MONTH_AND_YEAR)
                .param("userId", userId)
                .param("month", month)
                .param("year", year)
                .query(rowMapper)
                .list();
    }

    public List<Budget> findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(Long userId) {
        return jdbcClient.sql(BudgetQueries.FIND_BY_USER_ID_ORDER_BY_YEAR_DESC_MONTH_DESC)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

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

    public List<BudgetStatusDto> findBudgetStatusByUserIdAndMonthAndYear(Long userId, Integer month, Integer year) {
        return jdbcClient.sql(BudgetQueries.FIND_BUDGET_STATUS_BY_USER_ID_AND_MONTH_AND_YEAR)
                .param("userId", userId)
                .param("month", month)
                .param("year", year)
                .query(budgetStatusRowMapper)
                .list();
    }
}
