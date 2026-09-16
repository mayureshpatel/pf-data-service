package com.mayureshpatel.pfdataservice.repository.transaction.specification;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TransactionSpecification {

    // sentinel the frontend sends as a literal categoryName value to mean "uncategorized"
    private static final String UNCATEGORIZED_SENTINEL = "null";
    private static final ZoneOffset UTC_ZONE = ZoneOffset.UTC;

    private TransactionSpecification() {
    }

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
                parameters.put("description", "%" + escapeLike(filter.description().toLowerCase(Locale.ROOT)) + "%");
            }

            if (filter.categoryName() != null && !filter.categoryName().isBlank()) {
                if (UNCATEGORIZED_SENTINEL.equalsIgnoreCase(filter.categoryName())) {
                    conditions.add("transactions.category_id IS NULL");
                } else {
                    conditions.add("LOWER(categories.name) LIKE :categoryName ESCAPE '\\'");
                    parameters.put("categoryName", "%" + escapeLike(filter.categoryName().toLowerCase(Locale.ROOT)) + "%");
                }
            }

            if (filter.merchantCleanName() != null && !filter.merchantCleanName().isBlank()) {
                conditions.add("LOWER(merchants.clean_name) LIKE :merchantCleanName ESCAPE '\\'");
                parameters.put("merchantCleanName", "%" + escapeLike(filter.merchantCleanName().toLowerCase(Locale.ROOT)) + "%");
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
                // bound as an explicit UTC OffsetDateTime, not a bare LocalDate -- a LocalDate
                // parameter compared against a timestamptz column resolves using the database
                // session's timezone (America/New_York in production, see application.yml), not
                // UTC. Under that non-UTC session, a transaction stored at UTC midnight on this
                // exact start date falls *before* the implicitly-shifted lower bound and was
                // silently excluded. See PF-828.
                conditions.add("transactions.date >= :startDate");
                parameters.put("startDate", filter.startDate().atStartOfDay(UTC_ZONE).toOffsetDateTime());
            }

            if (filter.endDate() != null) {
                // exclusive upper bound on the day AFTER endDate, so the filter covers the whole
                // end date rather than cutting off at midnight -- same explicit-UTC reasoning as
                // startDate above, not just the inclusive-end-date reasoning this originally
                // documented (see PF-828).
                conditions.add("transactions.date < :endDate");
                parameters.put("endDate", filter.endDate().plusDays(1).atStartOfDay(UTC_ZONE).toOffsetDateTime());
            }

            if (filter.tagId() != null) {
                // PF-308: EXISTS rather than a JOIN -- transaction_tags is many-to-many, and a
                // transaction can carry other tags besides the one being filtered on. A JOIN
                // filtered to one tag_id wouldn't duplicate rows here (at most one match per
                // transaction), but EXISTS keeps this condition self-contained in the WHERE-clause
                // list without touching ENRICHED_JOINS/baseFrom construction at all.
                conditions.add("EXISTS (SELECT 1 FROM transaction_tags tt WHERE tt.transaction_id = transactions.id AND tt.tag_id = :tagId)");
                parameters.put("tagId", filter.tagId());
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
            LocalDate endDate,
            Long tagId
    ) {
    }
}
