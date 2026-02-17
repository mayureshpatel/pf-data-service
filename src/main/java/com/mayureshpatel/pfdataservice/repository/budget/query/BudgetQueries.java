package com.mayureshpatel.pfdataservice.repository.budget.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class BudgetQueries {

    // language=SQL
    public static final String FIND_BY_ID = """
            select *
            from budgets
            where id = :id
              and deleted_at is null
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
            select *
            from budgets
            where user_id = :userId
              and month = :month
              and year = :year
              and deleted_at is null
            """;

    // language=SQL
    public static final String FIND_BY_USER_ID_ORDER_BY_YEAR_DESC_MONTH_DESC = """
            select *
            from budgets
            where user_id = :userId
              and deleted_at is null
            order by year desc, month desc
            """;

    // language=SQL
    public static final String FIND_BY_USER_ID_AND_CATEGORY_ID_AND_MONTH_AND_YEAR = """
            select *
            from budgets
            where user_id = :userId
              and category_id = :categoryId
              and month = :month
              and year = :year
              and deleted_at is null
            """;
}
