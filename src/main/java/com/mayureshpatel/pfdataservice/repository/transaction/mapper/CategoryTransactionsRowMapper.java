package com.mayureshpatel.pfdataservice.repository.transaction.mapper;

import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.transaction.CategoryTransactionsDto;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CategoryTransactionsRowMapper extends JdbcMapperUtils implements RowMapper<CategoryTransactionsDto> {

    @Override
    public CategoryTransactionsDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        CategoryDto parentCategory = null;
        if (rs.getString("parent_category_id") != null) {
            parentCategory = new CategoryDto(
                    rs.getLong("parent_category_id"),
                    null,
                    rs.getString("parent_category_name"),
                    CategoryType.fromValue(rs.getString("parent_category_type")),
                    null,
                    rs.getString("parent_category_icon"),
                    rs.getString("parent_category_color")
            );
        }

        CategoryDto categoryDto = new CategoryDto(
                rs.getLong("category_id"),
                null,
                rs.getString("category_name"),
                CategoryType.fromValue(rs.getString("category_type")),
                parentCategory,
                rs.getString("category_icon"),
                rs.getString("category_color")
        );

        return new CategoryTransactionsDto(categoryDto, rs.getInt("transaction_count"));
    }
}
