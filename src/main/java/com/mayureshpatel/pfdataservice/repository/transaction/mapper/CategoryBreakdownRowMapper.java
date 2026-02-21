package com.mayureshpatel.pfdataservice.repository.transaction.mapper;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CategoryBreakdownRowMapper extends JdbcMapperUtils implements RowMapper<CategoryBreakdownDto> {

    @Override
    public CategoryBreakdownDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        Iconography categoryIconography = new Iconography();
        categoryIconography.setColor(rs.getString("category_color"));
        categoryIconography.setIcon(rs.getString("category_icon"));

        CategoryDto parentCategory = new CategoryDto(
                rs.getLong("category_parent_id"),
                null, null, null, null, null
        );

        CategoryDto categoryDto = new CategoryDto(
                rs.getLong("category_id"),
                null,
                rs.getString("category_name"),
                CategoryType.fromValue(rs.getString("category_type")),
                parentCategory,
                categoryIconography
        );

        return new CategoryBreakdownDto(categoryDto, rs.getBigDecimal("total"));
    }
}
