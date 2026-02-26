package com.mayureshpatel.pfdataservice.repository.transaction.mapper;

import com.mayureshpatel.pfdataservice.domain.Iconography;
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
        Iconography iconography = new Iconography();
        iconography.setColor(rs.getString("category_color"));
        iconography.setIcon(rs.getString("category_icon"));

        CategoryDto parentCategory = null;
        if (rs.getString("parent_category_id") != null) {
            Iconography parentIconography = new Iconography();
            parentIconography.setColor(rs.getString("parent_category_color"));
            parentIconography.setIcon(rs.getString("parent_category_icon"));

            parentCategory = new CategoryDto(
                    rs.getLong("parent_category_id"),
                    null,
                    rs.getString("parent_category_name"),
                    CategoryType.fromValue(rs.getString("parent_category_type")),
                    null,
                    parentIconography
            );
        }

        CategoryDto categoryDto = new CategoryDto(
                rs.getLong("category_id"),
                null,
                rs.getString("category_name"),
                CategoryType.fromValue(rs.getString("category_type")),
                parentCategory,
                iconography
        );

        return new CategoryTransactionsDto(categoryDto, rs.getInt("transaction_count"));
    }
}
