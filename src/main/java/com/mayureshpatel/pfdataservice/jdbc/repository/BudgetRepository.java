package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.jdbc.JdbcRepository;
import com.mayureshpatel.pfdataservice.jdbc.SoftDeleteSupport;
import com.mayureshpatel.pfdataservice.jdbc.mapper.BudgetRowMapper;
import com.mayureshpatel.pfdataservice.jdbc.util.SqlLoader;
import com.mayureshpatel.pfdataservice.model.Budget;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
        String query = sqlLoader.load("sql/budget/findById.sql");
        return jdbcClient.sql(query)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    @Override
    public Budget insert(Budget budget) {
        String query = sqlLoader.load("sql/budget/insert.sql");
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(query)
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
        String query = sqlLoader.load("sql/budget/update.sql");

        jdbcClient.sql(query)
                .param("amount", budget.getAmount())
                .param("id", budget.getId())
                .update();

        return budget;
    }

    @Override
    public void deleteById(Long id) {
        String query = sqlLoader.load("sql/budget/deleteById.sql");
        jdbcClient.sql(query)
                .param("id", id)
                .update();
    }
}
