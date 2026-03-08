package com.mayureshpatel.pfdataservice.repository.tag.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

@Component
public class TagRowMapper extends JdbcMapperUtils implements RowMapper<Tag> {

    @Override
    public Tag mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, "");
    }

    /**
     * Maps a ResultSet row to a Tag object with optional prefix for column names.
     * Handles null values gracefully and ensures that only available columns are accessed.
     *
     * @param rs     ResultSet containing row data
     * @param prefix Optional prefix for column names
     * @return Tag object constructed from the ResultSet row
     * @throws SQLException if there is an error accessing the ResultSet
     */
    public static Tag mapRow(ResultSet rs, String prefix) throws SQLException {
        String safePrefix = prefix.endsWith("_") ? prefix : prefix + "_";
        Set<String> availableColumns = getAvailableColumns(rs);

        Tag.TagBuilder builder = Tag.builder();
        builder.id(rs.getLong(safePrefix + "id"));

        if (hasColumn(safePrefix + "user_id", availableColumns)) {
            builder.userId(rs.getLong(safePrefix + "user_id"));
        }
        if (hasColumn(safePrefix + "name", availableColumns)) {
            builder.name(rs.getString(safePrefix + "name"));
        }
        if (hasColumn(safePrefix + "color", availableColumns)) {
            builder.color(rs.getString(safePrefix + "color"));
        }
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }
}
