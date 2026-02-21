package com.mayureshpatel.pfdataservice.repository.budget;

import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetStatusDto;
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

    @Override
    public Budget insert(Budget budget) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(BudgetQueries.INSERT)
                .param("userId", budget.getUser().getId())
                .param("categoryId", budget.getCategory().getId())
                .param("amount", budget.getAmount())
                .param("month", budget.getMonth())
                .param("year", budget.getYear())
                .update(keyHolder);

        budget.setId(keyHolder.getKeyAs(Long.class));
        return budget;
    }

    @Override
    public Budget update(Budget budget) {
        jdbcClient.sql(BudgetQueries.UPDATE)
                .param("amount", budget.getAmount())
                .param("id", budget.getId())
                .update();

        return budget;
    }

    @Override
    public Budget save(Budget budget) {
        if (budget.getId() == null) {
            return insert(budget);
        } else {
            return update(budget);
        }
    }

    @Override
    public void delete(Budget budget) {
        if (budget.getId() != null) {
            deleteById(budget.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        jdbcClient.sql(BudgetQueries.DELETE)
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
