package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.user.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

@Component
public class JdbcMapperUtils {

    /**
     * Hydrates audit columns from ResultSet.
     *
     * @param rs the result set
     * @return the audit columns
     * @throws SQLException if an error occurs
     */
    public static TableAudit getAuditColumns(ResultSet rs) throws SQLException {
        return getAuditColumns(rs, "");
    }

    /**
     * Hydrates audit columns from ResultSet with a prefix.
     *
     * @param rs     the result set
     * @param prefix the prefix for the column names
     * @return the audit columns
     * @throws SQLException if an error occurs
     */
    public static TableAudit getAuditColumns(ResultSet rs, String prefix) throws SQLException {
        return getAuditColumns(rs, prefix, getAvailableColumns(rs));
    }

    /**
     * Hydrates audit columns from ResultSet with a prefix and pre-calculated available columns.
     *
     * @param rs               the result set
     * @param prefix           the prefix for the column names
     * @param availableColumns the set of available columns in the result set
     * @return the audit columns
     * @throws SQLException if an error occurs
     */
    public static TableAudit getAuditColumns(ResultSet rs, String prefix, Set<String> availableColumns) throws SQLException {
        String safePrefix = prefix == null ? "" : prefix;

        OffsetDateTime createdAt = null;
        OffsetDateTime updatedAt = null;
        OffsetDateTime deletedAt = null;

        User createdBy = null;
        User updatedBy = null;
        User deletedBy = null;

        if (hasColumn(safePrefix + "created_at", availableColumns)) {
            createdAt = getOffsetDateTime(rs, safePrefix + "created_at");
        }

        if (hasColumn(safePrefix + "updated_at", availableColumns)) {
            updatedAt = getOffsetDateTime(rs, safePrefix + "updated_at");
        }

        if (hasColumn(safePrefix + "deleted_at", availableColumns)) {
            deletedAt = getOffsetDateTime(rs, safePrefix + "deleted_at");
        }

        if (hasColumn(safePrefix + "created_by", availableColumns)) {
            Long userId = getLongOrNull(rs, safePrefix + "created_by");
            if (userId != null) {
                createdBy = User.builder().id(userId).build();
            }
        }

        if (hasColumn(safePrefix + "updated_by", availableColumns)) {
            Long userId = getLongOrNull(rs, safePrefix + "updated_by");
            if (userId != null) {
                updatedBy = User.builder().id(userId).build();
            }
        }

        if (hasColumn(safePrefix + "deleted_by", availableColumns)) {
            Long userId = getLongOrNull(rs, safePrefix + "deleted_by");
            if (userId != null) {
                deletedBy = User.builder().id(userId).build();
            }
        }

        return TableAudit.builder()
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .createdBy(createdBy)
                .updatedBy(updatedBy)
                .deletedBy(deletedBy)
                .build();
    }

    /**
     * Get all available column labels from ResultSet.
     *
     * @param rs the result set
     * @return a set of lowercase column labels
     * @throws SQLException if an error occurs
     */
    public static Set<String> getAvailableColumns(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        Set<String> columns = new HashSet<>(columnCount * 2);
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnLabel(i).toLowerCase());
        }
        return columns;
    }

    /**
     * Safely check if column exists in ResultSet.
     *
     * @param rs         the result set
     * @param columnName the column name to check
     * @return true if column exists, false otherwise
     * @throws SQLException if an error occurs
     */
    public static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        return hasColumn(columnName, getAvailableColumns(rs));
    }

    /**
     * Safely check if column exists in pre-calculated set of available columns.
     *
     * @param columnName       the column name to check
     * @param availableColumns the set of available columns
     * @return true if column exists, false otherwise
     */
    public static boolean hasColumn(String columnName, Set<String> availableColumns) {
        return columnName != null && availableColumns.contains(columnName.toLowerCase());
    }

    /**
     * Safely get {@link OffsetDateTime} from {@link ResultSet}
     *
     * @param rs         the result set
     * @param columnName the column name
     * @return the {@link OffsetDateTime}
     * @throws SQLException if column does not exist or cannot be converted to OffsetDateTime
     */
    public static OffsetDateTime getOffsetDateTime(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp != null ? timestamp.toInstant().atOffset(ZoneOffset.UTC) : null;
    }

    /**
     * Safely get {@link LocalDateTime} from {@link ResultSet}
     *
     * @param rs         the result set
     * @param columnName the column name
     * @return the {@link LocalDateTime}
     * @throws SQLException if column does not exist or cannot be converted to LocalDateTime
     */
    public static LocalDateTime getLocalDateTime(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    /**
     * Safely get {@link LocalDate} from {@link ResultSet}
     *
     * @param rs         the result set
     * @param columnName the column name
     * @return the {@link LocalDate}
     * @throws SQLException if column does not exist or cannot be converted to LocalDate
     */
    public static LocalDate getLocalDate(ResultSet rs, String columnName) throws SQLException {
        Date localDate = rs.getDate(columnName);
        return localDate != null ? localDate.toLocalDate() : null;
    }

    /**
     * Safely get {@link BigDecimal} from {@link ResultSet}
     *
     * @param rs         the result set
     * @param columnName the column name
     * @return the {@link BigDecimal} value
     * @throws SQLException if column does not exist or cannot be converted to BigDecimal
     */
    public static BigDecimal getBigDecimal(ResultSet rs, String columnName) throws SQLException {
        BigDecimal value = rs.getBigDecimal(columnName);
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * Safely get {@link String} from {@link ResultSet}
     *
     * @param rs         the result set
     * @param columnName the column name
     * @return the {@link Long} value
     * @throws SQLException if column does not exist or cannot be converted to long
     */
    public static Long getLongOrNull(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    /**
     * Safely get {@link Integer} from {@link ResultSet}
     *
     * @param rs         the result set
     * @param columnName the column name
     * @return the {@link Integer} value
     * @throws SQLException if column does not exist or cannot be converted to Integer
     */
    public static Integer getIntegerOrNull(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}
