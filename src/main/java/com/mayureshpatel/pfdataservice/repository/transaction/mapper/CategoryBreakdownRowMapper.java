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

/**
 * Maps a JDBC {@link ResultSet} row to a {@link CategoryBreakdownDto} -- a report-specific
 * mapper for one spending-by-category aggregation row, always reading the same fixed
 * {@code category}-prefixed and {@code total} columns rather than following the
 * reusable-prefix convention of this package's entity-level row mappers.
 */
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
