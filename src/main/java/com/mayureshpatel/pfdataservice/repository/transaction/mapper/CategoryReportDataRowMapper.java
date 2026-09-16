package com.mayureshpatel.pfdataservice.repository.transaction.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.dto.report.CategoryReportDataDto;
import com.mayureshpatel.pfdataservice.mapper.CategoryDtoMapper;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import com.mayureshpatel.pfdataservice.repository.category.mapper.CategoryRowMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CategoryReportDataRowMapper extends JdbcMapperUtils implements RowMapper<CategoryReportDataDto> {

    @Override
    public CategoryReportDataDto mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        Category category = CategoryRowMapper.mapRow(rs, "category");

        return new CategoryReportDataDto(
                CategoryDtoMapper.toDto(category),
                rs.getBigDecimal("total"),
                rs.getLong("txn_count")
        );
    }
}
