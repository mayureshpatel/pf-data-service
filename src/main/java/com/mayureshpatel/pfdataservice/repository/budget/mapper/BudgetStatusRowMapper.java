package com.mayureshpatel.pfdataservice.repository.budget.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetStatusDto;
import com.mayureshpatel.pfdataservice.mapper.CategoryDtoMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class BudgetStatusRowMapper implements RowMapper<BudgetStatusDto> {

    @Override
    public BudgetStatusDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = User.builder()
                .id(rs.getLong("category_user_id"))
                .build();

        Category parentCategory = Category.builder()
                .id(rs.getLong("parent_category_id"))
                .user(user)
                .name(rs.getString("parent_category_name"))
                .build();

        Category category = Category.builder()
                .id(rs.getLong("category_id"))
                .user(user)
                .name(rs.getString("category_name"))
                .type(CategoryType.valueOf(rs.getString("category_type")))
                .parent(parentCategory)
                .build();

        return new BudgetStatusDto(
                CategoryDtoMapper.toDto(category),
                rs.getBigDecimal("budgeted_amount"),
                rs.getBigDecimal("spending_amount"),
                rs.getBigDecimal("remaining_amount"),
                rs.getDouble("percentage_spent")
        );
    }
}
