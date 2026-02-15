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
}
