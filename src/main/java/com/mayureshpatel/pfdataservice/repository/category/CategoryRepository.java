package com.mayureshpatel.pfdataservice.repository.category;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.category.mapper.CategoryRowMapper;
import com.mayureshpatel.pfdataservice.repository.category.query.CategoryQueries;
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

    public List<Category> findAllSubCategories(Long userId) {
        return jdbcClient.sql(CategoryQueries.FIND_ALL_SUB_CATEGORIES)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    @Override
    public Category insert(Category category) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(CategoryQueries.INSERT)
                .param("name", category.getName())
                .param("color", category.getIconography().getColor())
                .param("icon", category.getIconography().getIcon())
                .param("type", category.getType().name())
                .param("userId", category.getUser().getId())
                .param("parentId", category.getParent() != null ? category.getParent().getId() : null)
                .update(keyHolder);

        category.setId(keyHolder.getKeyAs(Long.class));
        return category;
    }

    @Override
    public Category update(Category category) {
        jdbcClient.sql(CategoryQueries.UPDATE)
                .param("name", category.getName())
                .param("color", category.getIconography().getColor())
                .param("icon", category.getIconography().getIcon())
                .param("type", category.getType().name())
                .param("parentId", category.getParent() != null ? category.getParent().getId() : null)
                .param("id", category.getId())
                .update();

        return category;
    }

    @Override
    public Category save(Category category) {
        if (category.getId() == null) {
            return insert(category);
        } else {
            return update(category);
        }
    }

    @Override
    public void delete(Category category) {
        if (category.getId() != null) {
            deleteById(category.getId());
        }
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
