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
        OffsetDateTime createdAt = null;
        OffsetDateTime updatedAt = null;
        OffsetDateTime deletedAt = null;

        User createdBy = null;
        User updatedBy = null;
        User deletedBy = null;

        if (isColumnExists(rs, "created_at")) {
            createdAt = getOffsetDateTime(rs, "created_at");
        }

        if (isColumnExists(rs, "updated_at")) {
            updatedAt = getOffsetDateTime(rs, "updated_at");
        }

        if (isColumnExists(rs, "deleted_at")) {
            deletedAt = getOffsetDateTime(rs, "deleted_at");
        }

        if (isColumnExists(rs, "created_by")) {
            createdBy = User.builder()
                    .id(rs.getLong("created_by"))
                    .build();
        }

        if (isColumnExists(rs, "updated_by")) {
            updatedBy = User.builder()
                    .id(rs.getLong("updated_by"))
                    .build();
        }

        if (isColumnExists(rs, "deleted_by")) {
            deletedBy = User.builder()
                    .id(rs.getLong("deleted_by"))
                    .build();
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
     * Safely check if column exists in ResultSet.
     *
     * @param rs         the result set
     * @param columnName the column name to check
     * @return true if column exists, false otherwise
     * @throws SQLException if an error occurs
     */
    public static boolean isColumnExists(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(metaData.getColumnName(i))) {
                return true;
            }
        }
        return false;
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
