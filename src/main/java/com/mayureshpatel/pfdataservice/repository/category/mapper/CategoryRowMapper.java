package com.mayureshpatel.pfdataservice.repository.category.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CategoryRowMapper extends JdbcMapperUtils implements RowMapper<Category> {

    @Override
    public Category mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Category.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .name(rs.getString("name"))
                .type(rs.getString("type"))
                .parentId(rs.getLong("parent_id"))
                .color(rs.getString("color"))
                .icon(rs.getString("icon"))
                .audit(getAuditColumns(rs))
                .build();
    }
}
