package com.mayureshpatel.pfdataservice.repository.transaction.query;

import lombok.NoArgsConstructor;

/**
 * SQL query constants for {@code NetWorthRepository}. Every query joins {@code accounts}, filters
 * {@code accounts.user_id = :userId} and leaves soft-deleted transactions out. The signed flow
 * expression mirrors the balance math in {@code TransactionQueries.GET_NET_FLOW_AFTER_DATE}: income and
 * transfers in add, expenses and transfers out subtract, an adjustment adds as it is, anything else subtracts (as Transaction.getNetChange does).
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class NetWorthQueries {

    /** The part of a query that restricts to the given accounts; appended only when accounts were chosen. */
    public static final String AND_ACCOUNTS = " and accounts.id in (:accountIds)";

    // language=SQL
    public static final String FIND_MONTHLY_NET_FLOW = """
            select transactions.account_id,
                   to_char(date_trunc('month', transactions.date at time zone 'UTC'), 'YYYY-MM') as flow_month,
                   sum(case
                           when transactions.type in ('INCOME', 'TRANSFER_IN') then abs(transactions.amount)
                           when transactions.type = 'ADJUSTMENT' then transactions.amount
                           else -abs(transactions.amount)
                       end) as net_flow
            from transactions
                     join accounts on accounts.id = transactions.account_id
            where accounts.user_id = :userId
              and transactions.deleted_at is null
              and transactions.date >= :from
            group by transactions.account_id, flow_month
            """;

    // language=SQL
    public static final String FIND_STORY_FLOWS = """
            select transactions.account_id,
                   coalesce(sum(transactions.amount) filter (where transactions.type in ('INCOME', 'TRANSFER_IN')), 0) as deposits,
                   coalesce(sum(transactions.amount) filter (where transactions.type in ('EXPENSE', 'TRANSFER_OUT', 'TRANSFER')), 0) as withdrawals,
                   coalesce(sum(transactions.amount) filter (where transactions.type not in ('INCOME', 'TRANSFER_IN', 'EXPENSE', 'TRANSFER_OUT', 'TRANSFER')), 0) as other_flow
            from transactions
                     join accounts on accounts.id = transactions.account_id
            where accounts.user_id = :userId
              and transactions.deleted_at is null
              and transactions.date >= :start
              and transactions.date < :end
            group by transactions.account_id
            """;

    // language=SQL
    public static final String FIND_INCOME_EXPENSE = """
            select coalesce(sum(transactions.amount) filter (where transactions.type = 'INCOME'), 0) as income,
                   coalesce(sum(transactions.amount) filter (where transactions.type = 'EXPENSE'), 0) as expense
            from transactions
                     join accounts on accounts.id = transactions.account_id
            where accounts.user_id = :userId
              and transactions.deleted_at is null
              and transactions.type in ('INCOME', 'EXPENSE')
              and transactions.date >= :start
              and transactions.date < :end
            """;
}
