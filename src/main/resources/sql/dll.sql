create table public.flyway_schema_history
(
    installed_rank integer                 not null
        constraint flyway_schema_history_pk
            primary key,
    version        varchar(50),
    description    varchar(200)            not null,
    type           varchar(20)             not null,
    script         varchar(1000)           not null,
    checksum       integer,
    installed_by   varchar(100)            not null,
    installed_on   timestamp default now() not null,
    execution_time integer                 not null,
    success        boolean                 not null
);

alter table public.flyway_schema_history
    owner to postgres;

create index flyway_schema_history_s_idx
    on public.flyway_schema_history (success);

create table public.users
(
    id                     bigserial
        primary key,
    username               varchar(50)  not null
        unique,
    password_hash          varchar(255) not null,
    email                  varchar(100) not null
        unique,
    last_updated_by        varchar(255) not null,
    last_updated_timestamp timestamp,
    created_at             timestamp default CURRENT_TIMESTAMP,
    deleted_at             timestamp
);

alter table public.users
    owner to postgres;

create index idx_users_deleted_at
    on public.users (deleted_at);

create table public.categories
(
    id         bigserial
        primary key,
    user_id    bigint      not null
        constraint fk_categories_user
            references public.users
            on delete cascade,
    name       varchar(50) not null,
    color      varchar(20),
    parent_id  bigint
        constraint fk_categories_parent
            references public.categories
            on delete cascade,
    created_at timestamp   default CURRENT_TIMESTAMP,
    updated_at timestamp   default CURRENT_TIMESTAMP,
    icon       varchar(50),
    type       varchar(20) default 'EXPENSE'::character varying
);

alter table public.categories
    owner to postgres;

create index idx_categories_user
    on public.categories (user_id);

create table public.tags
(
    id         bigserial
        primary key,
    user_id    bigint      not null
        constraint fk_tags_user
            references public.users
            on delete cascade,
    name       varchar(50) not null,
    color      varchar(20),
    created_at timestamp default CURRENT_TIMESTAMP,
    updated_at timestamp default CURRENT_TIMESTAMP,
    constraint uq_tags_name_user
        unique (user_id, name)
);

alter table public.tags
    owner to postgres;

create table public.category_rules
(
    id          bigserial
        primary key,
    keyword     varchar(255) not null,
    priority    integer   default 0,
    user_id     bigint
        constraint fk_rules_user
            references public.users
            on delete cascade,
    created_at  timestamp default CURRENT_TIMESTAMP,
    updated_at  timestamp default CURRENT_TIMESTAMP,
    category_id bigint
        constraint fk_category_rules_category
            references public.categories
            on delete set null
);

alter table public.category_rules
    owner to postgres;

create index idx_rules_user
    on public.category_rules (user_id);

create index idx_category_rules_category_id
    on public.category_rules (category_id);

create table public.budgets
(
    id          bigserial
        primary key,
    user_id     bigint         not null
        references public.users,
    category_id bigint         not null
        references public.categories,
    amount      numeric(19, 2) not null,
    month       integer        not null
        constraint budgets_month_check
            check ((month >= 1) AND (month <= 12)),
    year        integer        not null,
    created_at  timestamp with time zone default CURRENT_TIMESTAMP,
    updated_at  timestamp with time zone default CURRENT_TIMESTAMP,
    deleted_at  timestamp with time zone,
    unique (user_id, category_id, month, year)
);

alter table public.budgets
    owner to postgres;

create index idx_budgets_user_month_year
    on public.budgets (user_id, year, month);

create table public.account_types
(
    code       varchar(20)                            not null
        primary key,
    label      varchar(50)                            not null,
    icon       varchar(50),
    color      varchar(20),
    is_asset   boolean                                not null,
    sort_order integer                                not null,
    is_active  boolean                  default true  not null,
    created_at timestamp with time zone default now() not null,
    updated_at timestamp with time zone default now() not null
);

comment on table public.account_types is 'Lookup table for account types with metadata (icons, colors, labels). Used by frontend for display configuration.';

alter table public.account_types
    owner to postgres;

create table public.currencies
(
    code       char(3)                                not null
        primary key,
    name       varchar(50)                            not null,
    symbol     varchar(5)                             not null,
    is_active  boolean                  default true  not null,
    created_at timestamp with time zone default now() not null
);

comment on table public.currencies is 'ISO 4217 currency codes with display metadata. Validates currency_code in accounts table.';

alter table public.currencies
    owner to postgres;

create table public.accounts
(
    id              bigserial
        primary key,
    user_id         bigint                                         not null
        constraint fk_accounts_user
            references public.users
            on delete cascade,
    name            varchar(100)                                   not null,
    type            varchar(20)                                    not null
        constraint fk_accounts_type
            references public.account_types,
    current_balance numeric(19, 2)           default 0.00          not null
        constraint chk_balance_reasonable
            check ((current_balance >= '-9999999999.99'::numeric) AND (current_balance <= 9999999999.99)),
    created_at      timestamp with time zone default now(),
    updated_at      timestamp with time zone default now(),
    currency_code   char(3)                  default 'USD'::bpchar not null
        constraint fk_accounts_currency
            references public.currencies,
    deleted_at      timestamp with time zone,
    version         bigint                   default 0             not null
        constraint chk_version_positive
            check (version >= 0),
    bank_name       varchar(50),
    created_by      bigint
        references public.users,
    updated_by      bigint
        references public.users,
    deleted_by      bigint
        references public.users
);

comment on table public.accounts is 'User financial accounts including bank accounts, credit cards, and investment accounts.
Uses soft deletes (deleted_at IS NULL = active) and optimistic locking (version column).
Balances stored in specified currency (default USD).';

comment on column public.accounts.id is 'Primary key - auto-generated account ID';

comment on column public.accounts.user_id is 'Owner of this account (FK to users table)';

comment on column public.accounts.name is 'User-defined account name (e.g., "Chase Checking")';

comment on column public.accounts.type is 'Account type code (FK to account_types lookup table)';

comment on column public.accounts.current_balance is 'Current account balance in specified currency. For credit cards: negative = debt owed, positive = overpayment/credit.';

comment on constraint chk_balance_reasonable on public.accounts is 'Prevents unreasonable balance values. Limit: ±$9.9 trillion';

comment on column public.accounts.created_at is 'Timestamp when account was created (stored in UTC, displayed in user timezone)';

comment on column public.accounts.updated_at is 'Timestamp when account was last updated (stored in UTC, auto-updated by trigger)';

comment on column public.accounts.currency_code is 'ISO 4217 currency code (FK to currencies table, default USD)';

comment on column public.accounts.deleted_at is 'Timestamp when account was soft-deleted (stored in UTC)';

comment on column public.accounts.version is 'Optimistic locking version counter. Application must increment on each update and verify unchanged during UPDATE.';

comment on constraint chk_version_positive on public.accounts is 'Optimistic locking version must be non-negative';

comment on column public.accounts.bank_name is 'Optional: Financial institution name';

comment on column public.accounts.created_by is 'User ID who created this account';

comment on column public.accounts.updated_by is 'User ID who last updated this account';

comment on column public.accounts.deleted_by is 'User ID who soft-deleted this account';

alter table public.accounts
    owner to postgres;

create index idx_accounts_user_id
    on public.accounts (user_id);

create index idx_accounts_created_by
    on public.accounts (created_by);

create index idx_accounts_updated_by
    on public.accounts (updated_by);

create index idx_accounts_active
    on public.accounts (user_id, type, current_balance)
    where (deleted_at IS NULL);

create index idx_accounts_deleted
    on public.accounts (deleted_at, user_id)
    where (deleted_at IS NOT NULL);

create index idx_accounts_type
    on public.accounts (type)
    where (deleted_at IS NULL);

create unique index idx_accounts_user_name_unique
    on public.accounts (user_id, lower(name::text))
    where (deleted_at IS NULL);

comment on index public.idx_accounts_user_name_unique is 'Ensures account names are unique per user (case-insensitive). Allows name reuse after soft deletion.';

create table public.file_import_history
(
    id                bigserial
        primary key,
    account_id        bigint       not null
        constraint fk_import_account
            references public.accounts
            on delete cascade,
    file_name         varchar(255) not null,
    file_hash         varchar(64)  not null,
    transaction_count integer      not null,
    imported_at       timestamp default CURRENT_TIMESTAMP
);

alter table public.file_import_history
    owner to postgres;

create index idx_import_account_hash
    on public.file_import_history (account_id, file_hash);

create table public.account_snapshots
(
    id            bigserial
        primary key,
    account_id    bigint         not null
        constraint fk_snapshots_account
            references public.accounts
            on delete cascade,
    snapshot_date date           not null,
    balance       numeric(19, 2) not null,
    created_at    timestamp default CURRENT_TIMESTAMP,
    constraint uq_account_snapshot_date
        unique (account_id, snapshot_date)
);

alter table public.account_snapshots
    owner to postgres;

create index idx_snapshots_account_date
    on public.account_snapshots (account_id, snapshot_date);

create table public.merchants
(
    id            bigserial
        primary key,
    user_id       bigint
        references public.users
            on delete cascade,
    original_name varchar(255)                           not null,
    clean_name    varchar(255)                           not null,
    created_at    timestamp with time zone default now() not null,
    updated_at    timestamp with time zone default now() not null
);

comment on table public.merchants is 'Merchant registry mapping raw bank descriptions (original_name) to normalized display names (clean_name). Global merchants have user_id = NULL; user-scoped overrides have a specific user_id.';

comment on column public.merchants.original_name is 'Raw text as it appears in the bank/CSV import (e.g. "WHOLEFDS #12345").';

comment on column public.merchants.clean_name is 'Normalized display name shown in the UI (e.g. "Whole Foods").';

alter table public.merchants
    owner to postgres;

create table public.transactions
(
    id          bigserial
        primary key,
    account_id  bigint                   not null
        constraint fk_transactions_account
            references public.accounts
            on delete restrict,
    category_id bigint
        constraint fk_transactions_category
            references public.categories
            on delete set null,
    amount      numeric(19, 2)           not null,
    date        timestamp with time zone not null,
    description varchar(255),
    type        varchar(20)              not null
        constraint chk_transaction_type
            check ((type)::text = ANY
                   ((ARRAY ['INCOME'::character varying, 'EXPENSE'::character varying, 'TRANSFER'::character varying, 'TRANSFER_IN'::character varying, 'TRANSFER_OUT'::character varying, 'ADJUSTMENT'::character varying])::text[])),
    created_at  timestamp default CURRENT_TIMESTAMP,
    updated_at  timestamp default CURRENT_TIMESTAMP,
    deleted_at  timestamp,
    post_date   timestamp with time zone,
    merchant_id bigint
        constraint fk_transactions_merchant
            references public.merchants
            on delete set null
);

alter table public.transactions
    owner to postgres;

create index idx_transactions_account_id
    on public.transactions (account_id);

create index idx_transactions_category_id
    on public.transactions (category_id);

create index idx_transactions_type
    on public.transactions (type);

create index idx_transactions_description_trgm
    on public.transactions using gin (description public.gin_trgm_ops);

create index idx_transactions_deleted_at
    on public.transactions (deleted_at);

create index idx_transactions_merchant_id
    on public.transactions (merchant_id);

create index idx_transactions_date
    on public.transactions (date);

create index idx_transactions_account_date
    on public.transactions (account_id, date);

create table public.transaction_tags
(
    transaction_id bigint not null
        constraint fk_tt_transaction
            references public.transactions
            on delete cascade,
    tag_id         bigint not null
        constraint fk_tt_tag
            references public.tags
            on delete cascade,
    primary key (transaction_id, tag_id)
);

alter table public.transaction_tags
    owner to postgres;

create index idx_tt_tag_id
    on public.transaction_tags (tag_id);

create table public.recurring_transactions
(
    id          bigserial
        primary key,
    user_id     bigint         not null
        references public.users,
    account_id  bigint
        references public.accounts,
    amount      numeric(19, 2) not null,
    frequency   varchar(20)    not null,
    last_date   date,
    next_date   date           not null,
    active      boolean                  default true,
    created_at  timestamp with time zone default CURRENT_TIMESTAMP,
    updated_at  timestamp with time zone default CURRENT_TIMESTAMP,
    deleted_at  timestamp with time zone,
    merchant_id bigint         not null
        constraint fk_recurring_transactions_merchant
            references public.merchants
            on delete set null
);

alter table public.recurring_transactions
    owner to postgres;

create index idx_recurring_user_next_date
    on public.recurring_transactions (user_id, next_date);

create index idx_recurring_transactions_merchant_id
    on public.recurring_transactions (merchant_id);

create unique index idx_merchants_global_original_name
    on public.merchants (original_name)
    where (user_id IS NULL);

create unique index idx_merchants_user_original_name
    on public.merchants (user_id, original_name)
    where (user_id IS NOT NULL);

create index idx_merchants_user_id
    on public.merchants (user_id);

