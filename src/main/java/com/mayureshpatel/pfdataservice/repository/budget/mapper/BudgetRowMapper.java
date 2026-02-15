package com.mayureshpatel.pfdataservice.repository.budget.mapper;

import com.mayureshpatel.pfdataservice.repository.RowMapperFactory;
import com.mayureshpatel.pfdataservice.repository.category.model.Category;
import com.mayureshpatel.pfdataservice.repository.user.model.User;
import com.mayureshpatel.pfdataservice.repository.budget.model.Budget;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class BudgetRowMapper extends RowMapperFactory implements RowMapper<Budget> {

    @Override
    public Budget mapRow(ResultSet rs, int rowNum) throws SQLException {
        Budget budget = new Budget();
        budget.setId(rs.getLong("id"));
        budget.setAmount(getBigDecimal(rs, "amount"));
        budget.setMonth(rs.getInt("month"));
        budget.setYear(rs.getInt("year"));

        Long userId = getLongOrNull(rs, "user_id");
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            budget.setUser(user);
        }

        Long categoryId = getLongOrNull(rs, "category_id");
        if (categoryId != null) {
            Category category = new Category();
            category.setId(categoryId);
            budget.setCategory(category);
        }

        budget.setCreatedAt(getLocalDateTime(rs, "created_at"));
        budget.setUpdatedAt(getLocalDateTime(rs, "updated_at"));
        budget.setDeletedAt(getLocalDateTime(rs, "deleted_at"));

        return budget;
    }
}
