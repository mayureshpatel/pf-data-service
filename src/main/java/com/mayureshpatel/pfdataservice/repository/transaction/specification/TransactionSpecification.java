package com.mayureshpatel.pfdataservice.repository.transaction.specification;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionSpecification {

    public static FilterResult withFilter(Long userId, TransactionFilter filter) {
        return buildWhereClause(userId, filter);
    }

    public static FilterResult buildWhereClause(Long userId, TransactionFilter filter) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();

        // Always filter by User ID (Security) - join with accounts table
        conditions.add("a.user_id = :userId");
        parameters.put("userId", userId);

        if (filter != null) {
            if (filter.accountId() != null) {
                conditions.add("t.account_id = :accountId");
                parameters.put("accountId", filter.accountId());
            }

            if (filter.type() != null) {
                if (filter.type() == TransactionType.TRANSFER) {
                    conditions.add("t.type IN ('TRANSFER', 'TRANSFER_IN', 'TRANSFER_OUT')");
                } else {
                    conditions.add("t.type = :type");
                    parameters.put("type", filter.type().name());
                }
            }

            if (filter.description() != null && !filter.description().isBlank()) {
                conditions.add("LOWER(t.description) LIKE :description ESCAPE '\\'");
                parameters.put("description", "%" + escapeLike(filter.description().toLowerCase()) + "%");
            }

            if (filter.categoryName() != null && !filter.categoryName().isBlank()) {
                if ("null".equalsIgnoreCase(filter.categoryName())) {
                    conditions.add("t.category_id IS NULL");
                } else {
                    conditions.add("LOWER(c.name) LIKE :categoryName ESCAPE '\\'");
                    parameters.put("categoryName", "%" + escapeLike(filter.categoryName().toLowerCase()) + "%");
                }
            }

            if (filter.vendorName() != null && !filter.vendorName().isBlank()) {
                conditions.add("LOWER(m.clean_name) LIKE :vendorName ESCAPE '\\'");
                parameters.put("vendorName", "%" + escapeLike(filter.vendorName().toLowerCase()) + "%");
            }

            if (filter.minAmount() != null) {
                conditions.add("t.amount >= :minAmount");
                parameters.put("minAmount", filter.minAmount());
            }

            if (filter.maxAmount() != null) {
                conditions.add("t.amount <= :maxAmount");
                parameters.put("maxAmount", filter.maxAmount());
            }

            if (filter.startDate() != null) {
                conditions.add("t.date >= :startDate");
                parameters.put("startDate", filter.startDate());
            }

            if (filter.endDate() != null) {
                conditions.add("t.date <= :endDate");
                parameters.put("endDate", filter.endDate());
            }
        }

        // Always filter out deleted transactions
        conditions.add("t.deleted_at IS NULL");

        String whereClause = String.join(" AND ", conditions);
        return new FilterResult(whereClause, parameters);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
    }

    public record FilterResult(String whereClause, Map<String, Object> parameters) {
    }

    public record TransactionFilter(
            Long accountId,
            TransactionType type,
            String description,
            String categoryName,
            String vendorName,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}
