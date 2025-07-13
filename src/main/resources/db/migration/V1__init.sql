/*--------------------------------------------------------------------
  Personal‑Finance MVP – PostgreSQL DDL  (v4  – 2025‑07‑12)
  Adds created_by / updated_by tracking to every table.
--------------------------------------------------------------------*/

----------------------------------------------------------------------
-- 1️⃣  Extensions
----------------------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS citext;     -- case‑insensitive text

----------------------------------------------------------------------
-- 2️⃣  Domain & ENUMs (unchanged)
----------------------------------------------------------------------

CREATE DOMAIN currency_amount AS BIGINT
       CHECK (VALUE IS NOT NULL);

CREATE TYPE account_type_enum AS ENUM (
  'checking','savings','credit','investment','loan','cash'
);

CREATE TYPE transaction_type_enum AS ENUM (
  'expense','income','transfer','refund'
);

----------------------------------------------------------------------
-- 3️⃣  Helper: per‑session acting‑user mechanism
----------------------------------------------------------------------

/* We store the acting user‑id in a custom GUC (session variable).
   The backend should call    SELECT set_current_user_id('uuid‑here');   */
CREATE OR REPLACE FUNCTION set_current_user_id(uid uuid)
RETURNS void
LANGUAGE plpgsql
AS $$ BEGIN
  PERFORM set_config('app.current_user_id', uid::text, true);
END $$;

CREATE OR REPLACE FUNCTION current_app_user_id()
RETURNS uuid
LANGUAGE plpgsql
AS $$ DECLARE
  v TEXT;
BEGIN
  v := current_setting('app.current_user_id', true);
  IF v IS NULL THEN
     RETURN NULL;              -- fallback when called from maintenance jobs
  END IF;
  RETURN v::uuid;
END $$;

----------------------------------------------------------------------
-- 4️⃣  Core tables  (NEW columns highlighted)
----------------------------------------------------------------------

/*-------------------------------------------------- 4.1  USERS */
CREATE TABLE app_user (
    id            UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    email         CITEXT    NOT NULL,
    password_hash BYTEA     NOT NULL,
    full_name     TEXT      NOT NULL,
    locale        TEXT      DEFAULT 'en-US'
                  CHECK (locale ~ '^[a-z]{2}(-[A-Z]{2})?$'),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    created_by    UUID REFERENCES app_user(id),
    updated_by    UUID REFERENCES app_user(id),

    CONSTRAINT uk_app_user_email UNIQUE (email)
);

/*-------------------------------------------------- 4.2  ACCOUNTS */
CREATE TABLE account (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    name            TEXT NOT NULL,
    institution     TEXT,
    account_type    account_type_enum NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD'
                   CHECK (currency ~ '^[A-Z]{3}$'),
    current_balance currency_amount NOT NULL DEFAULT 0,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID REFERENCES app_user(id),
    updated_by      UUID REFERENCES app_user(id)
);
CREATE UNIQUE INDEX uk_account_user_name_ci
               ON account (user_id, lower(name));
CREATE INDEX idx_account_user_type ON account(user_id, account_type);

/*-------------------------------------------------- 4.3  TRANSACTIONS */
CREATE TABLE fin_transaction (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id       UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    user_id          UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    occurred_at      TIMESTAMPTZ NOT NULL,
    posted_at        TIMESTAMPTZ,
    amount           currency_amount NOT NULL,
    transaction_type transaction_type_enum NOT NULL,
    subtype          TEXT,
    payee            TEXT,
    description      TEXT,
    transfer_group_id UUID,

    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by       UUID REFERENCES app_user(id),
    updated_by       UUID REFERENCES app_user(id),

    CONSTRAINT chk_fin_transaction_sign CHECK (
          (transaction_type = 'expense'  AND amount <  0) OR
          (transaction_type = 'income'   AND amount >  0) OR
          (transaction_type = 'refund'   AND amount <  0) OR
          (transaction_type = 'transfer')
    ),
    CONSTRAINT chk_fin_transaction_nonzero CHECK (amount <> 0)
);
CREATE INDEX idx_tx_user_type_date  ON fin_transaction(user_id, transaction_type, occurred_at DESC);
CREATE INDEX idx_tx_account_date    ON fin_transaction(account_id, occurred_at DESC);
CREATE INDEX idx_tx_transfer_group  ON fin_transaction(transfer_group_id);

/* integrity trigger unchanged from v3 */
CREATE OR REPLACE FUNCTION verify_tx_user_ownership()
RETURNS TRIGGER AS $$
DECLARE acct_user UUID;
BEGIN
  SELECT user_id INTO acct_user FROM account WHERE id = NEW.account_id;
  IF NEW.user_id IS DISTINCT FROM acct_user THEN
     RAISE EXCEPTION 'fin_transaction.user_id must equal account.user_id';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_verify_tx_user
  BEFORE INSERT OR UPDATE ON fin_transaction
  FOR EACH ROW EXECUTE FUNCTION verify_tx_user_ownership();

/*-------------------------------------------------- 4.4  CATEGORIES */
CREATE TABLE category (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    color_hex   VARCHAR(7) DEFAULT '#4e91f9'
                 CHECK (color_hex ~ '^#[0-9A-Fa-f]{6}$'),

    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  UUID REFERENCES app_user(id),
    updated_by  UUID REFERENCES app_user(id)
);
CREATE UNIQUE INDEX uk_category_user_name_ci
    ON category (user_id, lower(name));

/*-------------------------------------------------- 4.5  TRANSACTION↔CATEGORY */
CREATE TABLE transaction_category (
    transaction_id UUID NOT NULL REFERENCES fin_transaction(id) ON DELETE CASCADE,
    category_id    UUID NOT NULL REFERENCES category(id)        ON DELETE CASCADE,

    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  UUID REFERENCES app_user(id),
    updated_by  UUID REFERENCES app_user(id),

    PRIMARY KEY (transaction_id, category_id)
);
CREATE INDEX idx_txcat_category     ON transaction_category(category_id);
CREATE INDEX idx_txcat_transaction  ON transaction_category(transaction_id);

----------------------------------------------------------------------
-- 4.6  Budgeting
----------------------------------------------------------------------

CREATE TABLE budget (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    month_year  DATE NOT NULL CHECK (EXTRACT(DAY FROM month_year) = 1),

    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  UUID REFERENCES app_user(id),
    updated_by  UUID REFERENCES app_user(id),

    CONSTRAINT uk_budget_user_month UNIQUE (user_id, month_year)
);

CREATE TABLE budget_item (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_id    UUID NOT NULL REFERENCES budget(id)   ON DELETE CASCADE,
    category_id  UUID NOT NULL REFERENCES category(id) ON DELETE CASCADE,
    limit_amount currency_amount NOT NULL CHECK (limit_amount > 0),

    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   UUID REFERENCES app_user(id),
    updated_by   UUID REFERENCES app_user(id),

    CONSTRAINT uk_budget_item UNIQUE (budget_id, category_id)
);
CREATE INDEX idx_budget_item_category ON budget_item(category_id);

----------------------------------------------------------------------
-- 5️⃣  Triggers to maintain updated_at / updated_by automatically
----------------------------------------------------------------------

/* 5.1 shared timestamp trigger (unchanged) */
CREATE OR REPLACE FUNCTION touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  IF ROW(NEW.*) IS DISTINCT FROM ROW(OLD.*) THEN
     NEW.updated_at := NOW();
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

/* 5.2 new trigger for created_by / updated_by */
CREATE OR REPLACE FUNCTION set_audit_user_columns()
RETURNS TRIGGER AS $$
DECLARE v_user uuid := current_app_user_id();
BEGIN
  /* ON INSERT */
  IF TG_OP = 'INSERT' THEN
      IF NEW.created_by IS NULL THEN NEW.created_by := v_user; END IF;
      IF NEW.updated_by IS NULL THEN NEW.updated_by := v_user; END IF;
  END IF;

  /* ON UPDATE */
  IF TG_OP = 'UPDATE' THEN
      IF v_user IS NOT NULL THEN
         NEW.updated_by := v_user;
      END IF;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

/* 5.3 attach triggers to every table with audit columns */
DO $$
DECLARE
  t text;
BEGIN
  FOREACH t IN ARRAY ARRAY[
      'app_user','account','fin_transaction',
      'category','transaction_category',
      'budget','budget_item'
  ] LOOP
    /* created_by / updated_by trigger */
    EXECUTE format(
      'CREATE TRIGGER trg_audit_%I
         BEFORE INSERT OR UPDATE ON %I
         FOR EACH ROW EXECUTE FUNCTION set_audit_user_columns();',
      t, t);

    /* updated_at touch trigger */
    EXECUTE format(
      'CREATE TRIGGER trg_touch_%I
         BEFORE UPDATE ON %I
         FOR EACH ROW EXECUTE FUNCTION touch_updated_at();',
      t, t);
  END LOOP;
END;
$$ LANGUAGE plpgsql;

----------------------------------------------------------------------
-- 6️⃣  Comments
----------------------------------------------------------------------
COMMENT ON TABLE fin_transaction IS
'Each row = one ledger entry. Transfers share a transfer_group_id.';
COMMENT ON COLUMN fin_transaction.amount IS
'Negative = money out; positive = money in; units = currency_amount (cents).';
COMMENT ON FUNCTION set_current_user_id(uuid) IS
'Call this at session start: SELECT set_current_user_id(''uuid''); triggers use it for created_by / updated_by.';
