package com.mayureshpatel.pfdataservice.repository.category;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.category.mapper.CategoryRuleRowMapper;
import com.mayureshpatel.pfdataservice.repository.category.query.CategoryRuleQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryRuleRepository implements JdbcRepository<CategoryRule, Long> {

    private final JdbcClient jdbcClient;
    private final CategoryRuleRowMapper rowMapper;

    @Override
    public Optional<CategoryRule> findById(Long id) {
        return this.jdbcClient.sql(CategoryRuleQueries.FIND_BY_ID)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    public List<CategoryRule> findByUserId(Long userId) {
        return this.jdbcClient.sql(CategoryRuleQueries.FIND_ALL_BY_USER_ID)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    public int insert(CategoryRule categoryRule) {
        return this.jdbcClient.sql(CategoryRuleQueries.INSERT)
                .param("id", categoryRule.getId())
                .param("keyword", categoryRule.getKeyword())
                .param("categoryId", categoryRule.getCategory().getId())
                .param("priority", categoryRule.getPriority())
                .param("userId", categoryRule.getUser().getId())
                .update();
    }

    @Override
    public int deleteById(Long id, Long userId) {
        return this.jdbcClient.sql(CategoryRuleQueries.DELETE)
                .param("id", id)
                .param("userId", userId)
                .update();
    }

    @Override
    public int deleteById(Long id) {
        throw new UnsupportedOperationException("Use deleteById with userId");
    }
}
