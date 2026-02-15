package com.mayureshpatel.pfdataservice.repository.budget.mapper;

import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class BudgetRowMapper extends JdbcMapperUtils implements RowMapper<Budget> {

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
