package com.mayureshpatel.pfdataservice.repository.category;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.dto.category.CategoryCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryUpdateRequest;
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
        return jdbcClient.sql("SELECT * FROM categories WHERE id = :id")
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

    public List<Category> findAllWIthParent(Long userId) {
        return jdbcClient.sql(CategoryQueries.FIND_ALL_WITH_PARENT)
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

    public int insert(CategoryCreateRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        return jdbcClient.sql(CategoryQueries.INSERT)
                .param("name", request.getName())
                .param("color", request.getColor())
                .param("icon", request.getIcon())
                .param("type", request.getType())
                .param("userId", request.getUserId())
                .param("parentId", request.getParentId() != null ? request.getParentId() : null)
                .update(keyHolder);
    }

    public int update(CategoryUpdateRequest request) {
        return jdbcClient.sql(CategoryQueries.UPDATE)
                .param("name", request.getName())
                .param("color", request.getColor())
                .param("icon", request.getIcon())
                .param("type", request.getType())
                .param("parentId", request.getParentId() != null ? request.getParentId() : null)
                .param("id", request.getId())
                .param("userId", request.getUserId())
                .update();
    }

    @Override
    public int delete(Category category) {
        if (category.getId() != null) {
            return jdbcClient.sql(CategoryQueries.DELETE)
                    .param("id", category.getId())
                    .param("userId", category.getUser().getId())
                    .update();
        }
        return 0;
    }

    @Override
    public int deleteById(Long id) {
        throw new UnsupportedOperationException("Use delete(Category category) to ensure userId is provided");
    }

    public long count(Long userId) {
        return jdbcClient.sql(CategoryQueries.COUNT)
                .param("userId", userId)
                .query(Long.class)
                .single();
    }
}
