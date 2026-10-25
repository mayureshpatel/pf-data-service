package com.mayureshpatel.pfdataservice.repository.transaction.query;

import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreFilter;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreGroupBy;
import lombok.NoArgsConstructor;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds the Reports explorer's aggregation SQL. Groupings come from a closed enum and map to fixed
 * SQL fragments here; every filter value is a named parameter, so no request text can reach the SQL.
 * Callers add {@code userId}, {@code start} and {@code end} to the returned parameters.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ExploreQueryBuilder {

    private static final String UTC_DATE = "transactions.date at time zone 'UTC'";

    private static final String BASE_FROM = """
            from transactions
                join accounts on transactions.account_id = accounts.id
                left join categories on transactions.category_id = categories.id
                left join categories parent_categories on categories.parent_id = parent_categories.id
            """;

    private static final String BASE_WHERE = """
            where accounts.user_id = :userId
              and transactions.deleted_at is null
              and transactions.date >= :start
              and transactions.date < :end
            """;

    /**
     * The built SQL and the filter parameters it uses.
     *
     * @param sql    the SQL text
     * @param params the named filter parameters (not including the user id and window)
     */
    public record BuiltQuery(String sql, Map<String, Object> params) {
    }

    /**
     * Builds the grouped income/expense query.
     *
     * @param groupBy the grouping
     * @param filter  the stacked filters
     * @return the SQL and its filter parameters
     */
    public static BuiltQuery build(ExploreGroupBy groupBy, ExploreFilter filter) {
        Map<String, Object> params = new HashMap<>();
        String key = keyExpression(groupBy);
        String label = labelExpression(groupBy);
        String sql = "select " + key + " as group_key,\n"
                + "       " + label + " as group_label,\n"
                + "       coalesce(sum(transactions.amount) filter (where transactions.type = 'INCOME'), 0) as income,\n"
                + "       coalesce(sum(transactions.amount) filter (where transactions.type = 'EXPENSE'), 0) as expense,\n"
                + "       count(*) filter (where transactions.type = 'INCOME') as income_count,\n"
                + "       count(*) filter (where transactions.type = 'EXPENSE') as expense_count\n"
                + BASE_FROM
                + extraJoins(groupBy)
                + BASE_WHERE
                + "  and transactions.type in ('INCOME', 'EXPENSE')\n"
                + filterConditions(filter, params)
                + "group by " + key + ", " + label + "\n"
                + "order by " + key + "\n";
        return new BuiltQuery(sql, params);
    }

    /**
     * Builds the query counting the transactions the explorer leaves out (transfers and adjustments).
     *
     * @param filter the stacked filters
     * @return the SQL and its filter parameters
     */
    public static BuiltQuery buildExcludedCount(ExploreFilter filter) {
        Map<String, Object> params = new HashMap<>();
        String sql = "select count(*)\n"
                + BASE_FROM
                + BASE_WHERE
                + "  and transactions.type not in ('INCOME', 'EXPENSE')\n"
                + filterConditions(filter, params);
        return new BuiltQuery(sql, params);
    }

    private static final Map<ExploreGroupBy, String> KEYS = new EnumMap<>(ExploreGroupBy.class);
    private static final Map<ExploreGroupBy, String> LABELS = new EnumMap<>(ExploreGroupBy.class);
    private static final Map<ExploreGroupBy, String> EXTRA_JOINS = new EnumMap<>(ExploreGroupBy.class);

    static {
        KEYS.put(ExploreGroupBy.CATEGORY, "coalesce(parent_categories.id, categories.id)");
        KEYS.put(ExploreGroupBy.SUBCATEGORY, "categories.id");
        KEYS.put(ExploreGroupBy.MERCHANT, "merchants.id");
        KEYS.put(ExploreGroupBy.ACCOUNT, "accounts.id");
        KEYS.put(ExploreGroupBy.TAG, "tags.id");
        KEYS.put(ExploreGroupBy.MONTH, "date_trunc('month', " + UTC_DATE + ")");
        KEYS.put(ExploreGroupBy.WEEK, "date_trunc('week', " + UTC_DATE + ")");
        KEYS.put(ExploreGroupBy.DAY_OF_WEEK, "extract(isodow from " + UTC_DATE + ")");
        KEYS.put(ExploreGroupBy.DAY, "(" + UTC_DATE + ")::date");

        LABELS.putAll(KEYS);
        LABELS.put(ExploreGroupBy.CATEGORY, "coalesce(parent_categories.name, categories.name, 'Uncategorized')");
        LABELS.put(ExploreGroupBy.SUBCATEGORY, "coalesce(categories.name, 'Uncategorized')");
        LABELS.put(ExploreGroupBy.MERCHANT, "coalesce(merchants.name, 'No merchant')");
        LABELS.put(ExploreGroupBy.ACCOUNT, "accounts.name");
        LABELS.put(ExploreGroupBy.TAG, "coalesce(tags.name, 'No tag')");

        EXTRA_JOINS.put(ExploreGroupBy.MERCHANT, "    left join merchants on transactions.merchant_id = merchants.id\n");
        EXTRA_JOINS.put(ExploreGroupBy.TAG, "    left join transaction_tags group_tags on group_tags.transaction_id = transactions.id\n"
                + "    left join tags on tags.id = group_tags.tag_id\n");
    }

    private static String keyExpression(ExploreGroupBy groupBy) {
        return KEYS.get(groupBy);
    }

    private static String labelExpression(ExploreGroupBy groupBy) {
        return LABELS.get(groupBy);
    }

    private static String extraJoins(ExploreGroupBy groupBy) {
        return EXTRA_JOINS.getOrDefault(groupBy, "");
    }

    private static String filterConditions(ExploreFilter filter, Map<String, Object> params) {
        if (filter == null) {
            return "";
        }
        StringBuilder where = new StringBuilder(512);
        appendAccountAndCategoryConditions(filter, where, params);
        appendMerchantConditions(filter, where, params);
        appendTagConditions(filter, where, params);
        appendAmountConditions(filter, where, params);
        return where.toString();
    }

    private static void appendAccountAndCategoryConditions(ExploreFilter filter, StringBuilder where, Map<String, Object> params) {
        if (filter.accountIds() != null && !filter.accountIds().isEmpty()) {
            where.append("  and accounts.id in (:accountIds)\n");
            params.put("accountIds", filter.accountIds());
        }
        if (filter.categoryId() != null) {
            where.append("  and (transactions.category_id = :categoryId or categories.parent_id = :categoryId)\n");
            params.put("categoryId", filter.categoryId());
        }
        if (filter.uncategorized()) {
            where.append("  and transactions.category_id is null\n");
        }
    }

    private static void appendMerchantConditions(ExploreFilter filter, StringBuilder where, Map<String, Object> params) {
        if (filter.merchantId() != null) {
            where.append("  and transactions.merchant_id = :merchantId\n");
            params.put("merchantId", filter.merchantId());
        }
        if (filter.noMerchant()) {
            where.append("  and transactions.merchant_id is null\n");
        }
    }

    private static void appendTagConditions(ExploreFilter filter, StringBuilder where, Map<String, Object> params) {
        if (filter.tagId() != null) {
            where.append("  and exists (select 1 from transaction_tags where transaction_tags.transaction_id = transactions.id"
                    + " and transaction_tags.tag_id = :tagId)\n");
            params.put("tagId", filter.tagId());
        }
        if (filter.noTag()) {
            where.append("  and not exists (select 1 from transaction_tags where transaction_tags.transaction_id = transactions.id)\n");
        }
    }

    private static void appendAmountConditions(ExploreFilter filter, StringBuilder where, Map<String, Object> params) {
        if (filter.minAmount() != null) {
            where.append("  and transactions.amount >= :minAmount\n");
            params.put("minAmount", filter.minAmount());
        }
        if (filter.maxAmount() != null) {
            where.append("  and transactions.amount <= :maxAmount\n");
            params.put("maxAmount", filter.maxAmount());
        }
    }
}
