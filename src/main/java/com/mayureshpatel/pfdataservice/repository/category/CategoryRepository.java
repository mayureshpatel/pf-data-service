package com.mayureshpatel.pfdataservice.repository.category;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SqlLoader;
import com.mayureshpatel.pfdataservice.repository.category.mapper.CategoryRowMapper;
import com.mayureshpatel.pfdataservice.repository.category.query.CategoryQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("jdbcCategoryRepository")
@RequiredArgsConstructor
public class CategoryRepository implements JdbcRepository<Category, Long> {

    private final JdbcClient jdbcClient;
    private final CategoryRowMapper rowMapper;
    private final SqlLoader sqlLoader;

    @Override
    public Optional<Category> findById(Long id) {
        return jdbcClient.sql(CategoryQueries.FIND_BY_ID)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    @Override
    public List<Category> findAll() {
        return jdbcClient.sql(CategoryQueries.FIND_ALL)
                .query(rowMapper)
                .list();
    }

    public List<Category> findByUserId(Long userId) {
        return jdbcClient.sql(CategoryQueries.FIND_ALL_BY_USER_ID)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    @Override
    public Category insert(Category category) {
        jdbcClient.sql(CategoryQueries.INSERT)
                .param("name", category.getName())
                .param("color", category.getIconography().getColor())
                .param("icon", category.getIconography().getIcon())
                .param("type", category.getType().name())
                .param("userId", category.getUser().getId());

        return category;
    }

    @Override
    public Category update(Category category) {
        jdbcClient.sql(CategoryQueries.UPDATE)
                .param("name", category.getName())
                .param("color", category.getIconography().getColor())
                .param("icon", category.getIconography().getIcon())
                .param("type", category.getType().name())
                .param("id", category.getId())
                .update();

        return category;
    }

    @Override
    public void deleteById(Long id) {
        jdbcClient.sql(CategoryQueries.DELETE)
                .param("id", id)
                .update();
    }

    @Override
    public long count() {
        return jdbcClient.sql(CategoryQueries.COUNT)
                .query(Long.class)
                .single();
    }
}
