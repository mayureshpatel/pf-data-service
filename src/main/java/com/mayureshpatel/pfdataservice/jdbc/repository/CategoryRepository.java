package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.jdbc.JdbcRepository;
import com.mayureshpatel.pfdataservice.jdbc.mapper.CategoryRowMapper;
import com.mayureshpatel.pfdataservice.jdbc.util.SqlLoader;
import com.mayureshpatel.pfdataservice.model.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
        String query = sqlLoader.load("sql/category/findById.sql");
        return jdbcClient.sql(query)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    @Override
    public List<Category> findAll() {
        String query = sqlLoader.load("sql/category/findAll.sql");
        return jdbcClient.sql(query)
                .query(rowMapper)
                .list();
    }

    public List<Category> findByUserId(Long userId) {
        String query = sqlLoader.load("sql/category/findByUserId.sql");
        return jdbcClient.sql(query)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    @Override
    public Category insert(Category category) {
        String query = sqlLoader.load("sql/category/insert.sql");
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(query)
                .param("name", category.getName())
                .param("color", category.getColor())
                .param("icon", category.getIcon())
                .param("type", category.getType().name())
                .param("userId", category.getUser().getId())
                .param("parentId", category.getParent() != null ? category.getParent().getId() : null)
                .update(keyHolder);

        category.setId(keyHolder.getKeyAs(Long.class));
        return category;
    }

    @Override
    public Category update(Category category) {
        String query = sqlLoader.load("sql/category/update.sql");

        jdbcClient.sql(query)
                .param("name", category.getName())
                .param("color", category.getColor())
                .param("icon", category.getIcon())
                .param("type", category.getType().name())
                .param("parentId", category.getParent() != null ? category.getParent().getId() : null)
                .param("id", category.getId())
                .update();

        return category;
    }

    @Override
    public void deleteById(Long id) {
        String query = sqlLoader.load("sql/category/deleteById.sql");
        jdbcClient.sql(query)
                .param("id", id)
                .update();
    }

    @Override
    public long count() {
        String query = sqlLoader.load("sql/category/count.sql");
        return jdbcClient.sql(query)
                .query(Long.class)
                .single();
    }
}
