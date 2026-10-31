package com.mayureshpatel.pfdataservice.repository.recurring_history.query;

import com.mayureshpatel.pfdataservice.service.DescriptionKeys;
import lombok.NoArgsConstructor;

/** SQL query constants for {@code RecurringTransactionRepository}. */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class RecurringTransactionQueries {

    // language=SQL
    public static final String FIND_BY_ID = """
            select
                recurring_transactions.*,
                accounts.name as account_name,
                accounts.bank_name as account_bank_name,
                account_types.code as account_type_code,
                account_types.label as account_type_label,
                account_types.color as account_type_color,
                account_types.icon as account_type_icon,
                account_types.is_asset as account_type_is_asset,
                currencies.code as currency_code,
                currencies.name as currency_name,
                currencies.symbol as currency_symbol,
                merchants.name as merchant_name
            from recurring_transactions
                left join accounts on recurring_transactions.account_id = accounts.id
                left join currencies on accounts.currency_code = currencies.code
                left join account_types on accounts.type = account_types.code
                left join merchants on recurring_transactions.merchant_id = merchants.id
            where recurring_transactions.id = :id
                and recurring_transactions.deleted_at is null
            """;

    // language=SQL
    public static final String FIND_ALL_BY_USER_ID = """
            select
                recurring_transactions.*,
                accounts.name as account_name,
                accounts.bank_name as account_bank_name,
                account_types.code as account_type_code,
                account_types.label as account_type_label,
                account_types.color as account_type_color,
                account_types.icon as account_type_icon,
                account_types.is_asset as account_type_is_asset,
                currencies.code as currency_code,
                currencies.name as currency_name,
                currencies.symbol as currency_symbol,
                merchants.name as merchant_name
            from recurring_transactions
                left join accounts on recurring_transactions.account_id = accounts.id
                left join currencies on accounts.currency_code = currencies.code
                left join account_types on accounts.type = account_types.code
                left join merchants on recurring_transactions.merchant_id = merchants.id
            where recurring_transactions.user_id = :userId
                and recurring_transactions.deleted_at is null
            order by recurring_transactions.merchant_id
            """;

    // language=SQL
    public static final String INSERT = """
            insert into recurring_transactions (
                                                user_id,
                                                account_id,
                                                merchant_id,
                                                amount,
                                                frequency,
                                                last_date,
                                                next_date,
                                                active,
                                                created_at, updated_at)
            values (
                    :userId,
                    :accountId,
                    :merchantId,
                    :amount,
                    :frequency,
                    :lastDate,
                    :nextDate,
                    :active,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            returning id
            """;

    // language=SQL
    public static final String UPDATE = """
            update recurring_transactions
            set account_id = :accountId,
                merchant_id = :merchantId,
                amount = :amount,
                frequency = :frequency,
                next_date = :nextDate,
                active = :active,
                updated_at = CURRENT_TIMESTAMP
            where id = :id
                and user_id = :userId
            """;

    // language=SQL
    public static final String DELETE = """
            update recurring_transactions
            set deleted_at = CURRENT_TIMESTAMP
            where id = :id
                and user_id = :userId
                and deleted_at is null
            """;

    // language=SQL
    public static final String COUNT_BY_ACCOUNT_ID = """
            select count(*)
            from recurring_transactions
            where account_id = :accountId
              and deleted_at is null
            """;

    // language=SQL -- PF-884: matches each tracked item to the user's real expense charges of the last
    // three years, through the item's merchant id or through a description linked to that merchant
    // (folded exactly like the link key: trim, collapse whitespace, lower case), and keeps the 12 newest
    // per item. each description is folded once (not once per tracked item), and both matches are plain
    // equi-joins so the planner can hash them; union removes a charge both matches found.
    public static final String FIND_RECENT_CHARGES_BY_RECURRING_ID = """
            with user_charges as (
                select transactions.id,
                       transactions.merchant_id,
                       transactions.date,
                       transactions.amount,
                       ${descriptionKey} as norm_description
                from transactions
                    join accounts on transactions.account_id = accounts.id
                where accounts.user_id = :userId
                  and transactions.deleted_at is null
                  and transactions.type = 'EXPENSE'
                  and transactions.date >= :since
            ),
            tracked as (
                select recurring_transactions.id, recurring_transactions.merchant_id
                from recurring_transactions
                where recurring_transactions.user_id = :userId
                  and recurring_transactions.deleted_at is null
                  and recurring_transactions.merchant_id is not null
            ),
            matched as (
                select tracked.id as recurring_id, user_charges.id as charge_id, user_charges.date, user_charges.amount
                from tracked
                    join user_charges on user_charges.merchant_id = tracked.merchant_id
                union
                select tracked.id, user_charges.id, user_charges.date, user_charges.amount
                from tracked
                    join merchant_description_links links
                        on links.merchant_id = tracked.merchant_id and links.user_id = :userId
                    join user_charges on user_charges.norm_description = links.normalized_description
            )
            select ranked.recurring_id, ranked.date, ranked.amount
            from (
                select matched.recurring_id,
                       matched.date,
                       matched.amount,
                       row_number() over (
                           partition by matched.recurring_id
                           order by matched.date desc, matched.charge_id desc
                       ) as rn
                from matched
            ) ranked
            where ranked.rn <= 12
            order by ranked.recurring_id, ranked.rn
            """.replace("${descriptionKey}", DescriptionKeys.sqlFold("transactions.description"));

    // language=SQL -- PF-884: whether a tracked item still uses a merchant (blocks deleting it)
    public static final String COUNT_ACTIVE_BY_MERCHANT_ID = """
            select count(*)
            from recurring_transactions
            where merchant_id = :merchantId
              and user_id = :userId
              and deleted_at is null
            """;

    // language=SQL -- PF-884: merchant_id is NOT NULL, so rows already deleted from the user's view
    // would still block deleting the merchant (the foreign key's SET NULL can not apply); remove them
    public static final String PURGE_DELETED_BY_MERCHANT_ID = """
            delete from recurring_transactions
            where merchant_id = :merchantId
              and user_id = :userId
              and deleted_at is not null
            """;
}
