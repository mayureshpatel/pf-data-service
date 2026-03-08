package com.mayureshpatel.pfdataservice.repository.recurring_history.query;

import lombok.NoArgsConstructor;

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
                merchants.original_name as merchant_original_name,
                merchants.clean_name as merchant_clean_name
            from recurring_transactions
                left join accounts on recurring_transactions.account_id = accounts.id
                left join currencies on accounts.currency_code = currencies.code
                left join account_types on accounts.type = account_types.code
                left join merchants on recurring_transactions.merchant_id = merchants.id
            where recurring_transactions.id = :id
                and recurring_transactions.deleted_at is null
            """;

    // language=SQL
    public static final String FIND_BY_USER_ID_ACTIVE_ORDER_BY_NEXT_DATE = """
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
                merchants.original_name as merchant_original_name,
                merchants.clean_name as merchant_clean_name
            from recurring_transactions
                left join accounts on recurring_transactions.account_id = accounts.id
                left join currencies on accounts.currency_code = currencies.code
                left join account_types on accounts.type = account_types.code
                left join merchants on recurring_transactions.merchant_id = merchants.id
            where recurring_transactions.user_id = :userId
                and recurring_transactions.active = true
                and recurring_transactions.deleted_at is null
            order by recurring_transactions.next_date
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
                merchants.original_name as merchant_original_name,
                merchants.clean_name as merchant_clean_name
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
}
