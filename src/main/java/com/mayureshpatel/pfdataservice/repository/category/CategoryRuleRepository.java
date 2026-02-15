package com.mayureshpatel.pfdataservice.repository.category;

import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SqlLoader;
import com.mayureshpatel.pfdataservice.repository.category.model.CategoryRule;
import com.mayureshpatel.pfdataservice.repository.category.mapper.CategoryRuleRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CategoryRuleRepository implements JdbcRepository<CategoryRule, Long> {

    private final JdbcClient jdbcClient;
    private final CategoryRuleRowMapper rowMapper;
    private final SqlLoader sqlLoader;

    public List<CategoryRule> findByUserId(Long userId) {
        String sql = """
                    select *
                    from category_rules
                    where user_id = :userId
                    order by priority desc, length(keyword) desc
                """;

        return this.jdbcClient.sql(sql)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    @Override
    public CategoryRule save(CategoryRule categoryRule) {
        String query = """
                    insert into category_rules (id, keyword, category_name, priority, user_id, created_at, updated_at) 
                    values(:id, :keyword, :categoryName, :priority, :userId, current_timestamp, current_timestamp)
                    on conflict (id) do update set keyword = excluded.keyword, category_name = excluded.category_name, priority = excluded.priority
                """;

        this.jdbcClient.sql(query)
                .param("id", categoryRule.getId())
                .param("keyword", categoryRule.getKeyword())
                .param("categoryName", categoryRule.getCategoryName())
                .param("priority", categoryRule.getPriority())
                .param("userId", categoryRule.getUser().getId())
                .update();

        return categoryRule;
    }

    @Override
    public void deleteById(Long id) {
        String sql = "delete from category_rules where id = :id";

        this.jdbcClient.sql(sql)
                .param("id", id)
                .update();
    }
}
