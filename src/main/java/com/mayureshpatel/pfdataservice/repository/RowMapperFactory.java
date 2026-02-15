package com.mayureshpatel.pfdataservice.repository;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class RowMapperFactory {

    /**
     * Safely get {@link OffsetDateTime} from {@link ResultSet}
     *
     * @param rs         the result set
     * @param columnName the column name
     * @return the {@link OffsetDateTime}
     * @throws SQLException if column does not exist or cannot be converted to OffsetDateTime
     */
    protected OffsetDateTime getOffsetDateTime(ResultSet rs, String columnName) throws SQLException {
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
    protected LocalDateTime getLocalDateTime(ResultSet rs, String columnName) throws SQLException {
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
    protected LocalDate getLocalDate(ResultSet rs, String columnName) throws SQLException {
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
    protected BigDecimal getBigDecimal(ResultSet rs, String columnName) throws SQLException {
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
    protected Long getLongOrNull(ResultSet rs, String columnName) throws SQLException {
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
    protected Integer getIntegerOrNull(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}
