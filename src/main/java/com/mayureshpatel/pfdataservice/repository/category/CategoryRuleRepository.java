package com.mayureshpatel.pfdataservice.repository.category;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.category.mapper.CategoryRuleRowMapper;
import com.mayureshpatel.pfdataservice.repository.category.query.CategoryRuleQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CategoryRuleRepository implements JdbcRepository<CategoryRule, Long> {

    private final JdbcClient jdbcClient;
    private final CategoryRuleRowMapper rowMapper;

    public List<CategoryRule> findByUserId(Long userId) {
        return this.jdbcClient.sql(CategoryRuleQueries.FIND_ALL_BY_USER_ID)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    public CategoryRule save(CategoryRule categoryRule) {
        this.jdbcClient.sql(CategoryRuleQueries.INSERT)
                .param("id", categoryRule.getId())
                .param("keyword", categoryRule.getKeyword())
                .param("categoryId", categoryRule.getCategory().getId())
                .param("priority", categoryRule.getPriority())
                .param("userId", categoryRule.getUser().getId())
                .update();

        return categoryRule;
    }

    @Override
    public void deleteById(Long id) {
        this.jdbcClient.sql(CategoryRuleQueries.DELETE)
                .param("id", id)
                .update();
    }
}
