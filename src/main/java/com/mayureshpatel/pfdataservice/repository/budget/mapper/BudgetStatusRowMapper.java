package com.mayureshpatel.pfdataservice.repository.budget.mapper;

import com.mayureshpatel.pfdataservice.dto.budget.BudgetStatusDto;
import com.mayureshpatel.pfdataservice.mapper.CategoryDtoMapper;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import com.mayureshpatel.pfdataservice.repository.category.mapper.CategoryRowMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class BudgetStatusRowMapper extends JdbcMapperUtils implements RowMapper<BudgetStatusDto> {

    @Override
    public BudgetStatusDto mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return new BudgetStatusDto(
                // todo: make sure the query returns parent cateogry as 'category_parent_<column>'
                CategoryDtoMapper.toDto(CategoryRowMapper.mapRow(rs, "category")),
                rs.getBigDecimal("budgeted_amount"),
                rs.getBigDecimal("spending_amount"),
                rs.getBigDecimal("remaining_amount"),
                rs.getDouble("percentage_spent")
        );
    }
}
