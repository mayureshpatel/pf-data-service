package com.mayureshpatel.pfdataservice.repository.budget;

import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SoftDeleteSupport;
import com.mayureshpatel.pfdataservice.repository.SqlLoader;
import com.mayureshpatel.pfdataservice.repository.budget.mapper.BudgetRowMapper;
import com.mayureshpatel.pfdataservice.repository.budget.query.BudgetQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("jdbcBudgetRepository")
@RequiredArgsConstructor
public class BudgetRepository implements JdbcRepository<Budget, Long>, SoftDeleteSupport {

    private final JdbcClient jdbcClient;
    private final BudgetRowMapper rowMapper;
    private final SqlLoader sqlLoader;

    @Override
    public Optional<Budget> findById(Long id) {
        return jdbcClient.sql(BudgetQueries.FIND_BY_ID)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    @Override
    public Budget insert(Budget budget) {
        jdbcClient.sql(BudgetQueries.INSERT)
                .param("userId", budget.getUser().getId())
                .param("categoryId", budget.getCategory().getId())
                .param("amount", budget.getAmount())
                .param("month", budget.getMonth())
                .param("year", budget.getYear());

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
    public void deleteById(Long id) {
        jdbcClient.sql(BudgetQueries.DELETE)
                .param("id", id)
                .update();
    }
}
