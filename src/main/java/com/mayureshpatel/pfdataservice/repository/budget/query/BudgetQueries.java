package com.mayureshpatel.pfdataservice.repository.budget.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class BudgetQueries {

    // language=SQL
    public static final String FIND_BY_ID = """
            select
                budgets.*,
                categories.name as category_name,
                categories.type as category_type,
                categories.icon as category_icon,
                categories.color as category_color,
                categories.parent_id as category_parent_id
            from budgets
                left join categories on categories.id = budgets.category_id
            where budgets.id = :id
              and budgets.deleted_at is null
            """;

    // language=SQL
    public static final String INSERT = """
            insert into budgets
                (user_id,
                 category_id,
                 amount,
                 month,
                 year,
                 created_at, updated_at)
            values (
                    :userId,
                    :categoryId,
                    :amount,
                    :month,
                    :year,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            returning id
            """;

    // language=SQL
    public static final String UPDATE = """
            update budgets
            set amount = :amount,
                updated_at = CURRENT_TIMESTAMP
            where id = :id
              and deleted_at is null
            """;

    // language=SQL
    public static final String DELETE = """
            update budgets
            set deleted_at = CURRENT_TIMESTAMP
            where id = :id
             and deleted_at is null
            """;

    // language=SQL
    public static final String FIND_BY_USER_ID_AND_MONTH_AND_YEAR = """
            select
                budgets.*,
                categories.name as category_name,
                categories.type as category_type,
                categories.icon as category_icon,
                categories.color as category_color,
                categories.parent_id as category_parent_id
            from budgets
                left join categories on categories.id = budgets.category_id
            where budgets.user_id = :userId
              and budgets.month = :month
              and budgets.year = :year
              and budgets.deleted_at is null
            """;

    // language=SQL
    public static final String FIND_BY_USER_ID_ORDER_BY_YEAR_DESC_MONTH_DESC = """
            select
                budgets.*,
                categories.name as category_name,
                categories.type as category_type,
                categories.icon as category_icon,
                categories.color as category_color,
                categories.parent_id as category_parent_id
            from budgets
                left join categories on categories.id = budgets.category_id
            where budgets.user_id = :userId
              and budgets.deleted_at is null
            order by budgets.year desc, budgets.month desc
            """;

    // language=SQL
    public static final String FIND_BY_USER_ID_AND_CATEGORY_ID_AND_MONTH_AND_YEAR = """
            select
                budgets.*,
                categories.name as category_name,
                categories.type as category_type,
                categories.icon as category_icon,
                categories.color as category_color,
                categories.parent_id as category_parent_id
            from budgets
                left join categories on categories.id = budgets.category_id
            where budgets.user_id = :userId
              and budgets.category_id = :categoryId
              and budgets.month = :month
              and budgets.year = :year
              and budgets.deleted_at is null
            """;

    // language=SQL
    public static final String FIND_BUDGET_STATUS_BY_USER_ID_AND_MONTH_AND_YEAR = """
            WITH spending AS (
                SELECT
                    t.category_id,
                    SUM(t.amount) AS total_spent
                FROM transactions t
                         JOIN accounts a ON t.account_id = a.id
                WHERE a.user_id     = :userId
                  AND a.deleted_at  IS NULL
                  AND t.deleted_at  IS NULL
                  AND t.type        = 'EXPENSE'
                  AND EXTRACT(YEAR  FROM t.date) = :year
                  AND EXTRACT(MONTH FROM t.date) = :month
                GROUP BY t.category_id
            )
            
            -- 1. Budgeted categories (spending may or may not exist)
            SELECT
                c.id                                                     AS category_id,
                c.user_id                                                AS category_user_id,
                c.name                                                   AS category_name,
                c.type                                                   AS category_type,
                pc.id                                                    AS category_parent_id,
                pc.name                                                  AS category_parent_name,
                b.amount                                                 AS budgeted_amount,
                COALESCE(s.total_spent, 0)                               AS spending_amount,
                b.amount - COALESCE(s.total_spent, 0)                    AS remaining_amount,
                CASE
                    WHEN b.amount = 0 THEN 0.0
                    ELSE ROUND((COALESCE(s.total_spent, 0) / b.amount) * 100, 2)
                    END                                                      AS percentage_spent
            FROM budgets b
                     JOIN  categories c  ON c.id        = b.category_id
                     LEFT JOIN categories pc ON pc.id   = c.parent_id
                     LEFT JOIN spending   s  ON s.category_id = b.category_id
            WHERE b.user_id    = :userId
              AND b.month    = :month
              AND b.year     = :year
              AND b.deleted_at IS NULL
            
            UNION ALL
            
            -- 2. Unbudgeted categories (spending exists, but no budget defined for the period)
            SELECT
                c.id                                                     AS category_id,
                c.user_id                                                AS category_user_id,
                c.name                                                   AS category_name,
                c.type                                                   AS category_type,
                pc.id                                                    AS category_parent_id,
                pc.name                                                  AS category_parent_name,
                0                                                        AS budgeted_amount,
                s.total_spent                                            AS spending_amount,
                -s.total_spent                                           AS remaining_amount,
                100.0                                                    AS percentage_spent
            FROM spending s
                     JOIN  categories c  ON c.id      = s.category_id
                     LEFT JOIN categories pc ON pc.id = c.parent_id
            WHERE NOT EXISTS (
                SELECT 1
                FROM budgets b2
                WHERE b2.category_id = s.category_id
                  AND b2.user_id   = :userId
                  AND b2.month     = :month
                  AND b2.year      = :year
                  AND b2.deleted_at IS NULL
            )
            
            ORDER BY category_name;
            """;
}
