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

        // filter by user id; users can only access their own transactions
        conditions.add("accounts.user_id = :userId");
        parameters.put("userId", userId);

        if (filter != null) {
            if (filter.accountId() != null) {
                conditions.add("transactions.account_id = :accountId");
                parameters.put("accountId", filter.accountId());
            }

            if (filter.type() != null) {
                if (filter.type() == TransactionType.TRANSFER) {
                    conditions.add("transactions.type IN ('TRANSFER', 'TRANSFER_IN', 'TRANSFER_OUT')");
                } else {
                    conditions.add("transactions.type = :type");
                    parameters.put("type", filter.type().name());
                }
            }

            if (filter.description() != null && !filter.description().isBlank()) {
                conditions.add("LOWER(transactions.description) LIKE :description ESCAPE '\\'");
                parameters.put("description", "%" + escapeLike(filter.description().toLowerCase()) + "%");
            }

            if (filter.categoryName() != null && !filter.categoryName().isBlank()) {
                if ("null".equalsIgnoreCase(filter.categoryName())) {
                    conditions.add("transactions.category_id IS NULL");
                } else {
                    conditions.add("LOWER(categories.name) LIKE :categoryName ESCAPE '\\'");
                    parameters.put("categoryName", "%" + escapeLike(filter.categoryName().toLowerCase()) + "%");
                }
            }

            if (filter.merchantCleanName() != null && !filter.merchantCleanName().isBlank()) {
                conditions.add("LOWER(merchants.clean_name) LIKE :merchantCleanName ESCAPE '\\'");
                parameters.put("merchantCleanName", "%" + escapeLike(filter.merchantCleanName().toLowerCase()) + "%");
            }

            if (filter.minAmount() != null) {
                conditions.add("transactions.amount >= :minAmount");
                parameters.put("minAmount", filter.minAmount());
            }

            if (filter.maxAmount() != null) {
                conditions.add("transactions.amount <= :maxAmount");
                parameters.put("maxAmount", filter.maxAmount());
            }

            if (filter.startDate() != null) {
                conditions.add("transactions.date >= :startDate");
                parameters.put("startDate", filter.startDate());
            }

            if (filter.endDate() != null) {
                conditions.add("transactions.date <= :endDate");
                parameters.put("endDate", filter.endDate());
            }
        }

        // always filter out deleted transactions
        conditions.add("transactions.deleted_at IS NULL");

        String whereClause = String.join(" and ", conditions);
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
            String merchantCleanName,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}
