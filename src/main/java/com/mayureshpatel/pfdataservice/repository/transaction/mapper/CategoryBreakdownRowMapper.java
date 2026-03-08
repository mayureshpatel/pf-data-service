package com.mayureshpatel.pfdataservice.repository.transaction.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.mapper.CategoryDtoMapper;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import com.mayureshpatel.pfdataservice.repository.category.mapper.CategoryRowMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CategoryBreakdownRowMapper extends JdbcMapperUtils implements RowMapper<CategoryBreakdownDto> {

    @Override
    public CategoryBreakdownDto mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        Category category = CategoryRowMapper.mapRow(rs, "category");

        return new CategoryBreakdownDto(
                CategoryDtoMapper.toDto(category),
                rs.getBigDecimal("total")
        );
    }
}
