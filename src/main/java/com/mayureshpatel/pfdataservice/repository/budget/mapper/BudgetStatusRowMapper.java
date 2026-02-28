package com.mayureshpatel.pfdataservice.repository.budget.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetStatusDto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class BudgetStatusRowMapper implements RowMapper<BudgetStatusDto> {

    @Override
    public BudgetStatusDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("category_user_id"));

        Category parentCategory = new Category();
        parentCategory.setId(rs.getLong("parent_category_id"));
        parentCategory.setName(rs.getString("parent_category_name"));

        Category category = new Category();
        category.setId(rs.getLong("category_id"));
        category.setUser(user);
        category.setName(rs.getString("category_name"));
        category.setType(CategoryType.valueOf(rs.getString("category_type")));
        category.setParent(parentCategory);

        return new BudgetStatusDto(
                category,
                rs.getBigDecimal("budgeted_amount"),
                rs.getBigDecimal("spending_amount"),
                rs.getBigDecimal("remaining_amount"),
                rs.getDouble("percentage_spent")
        );
    }
}
