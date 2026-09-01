#!/usr/bin/env python3
"""
Generates realistic mock `transactions` rows as SQL INSERT statements.

Bundles the procedure mock-data-generator's SKILL.md previously only described in prose. Uses
`psql` (not psycopg2 -- not a project dependency) to introspect the transactions table's REAL
current columns at run time, so it self-adapts as migrations change the schema instead of going
stale like a hardcoded column list would.

Requires an existing account (and, optionally, category/merchant) to attach generated
transactions to -- it does not create those itself. Pass --account-id, or let it sample one from
the database.

Usage:
    ./scripts/generate_mock_transactions.py --count 5000 --account-id 1 > seed.sql
    ./scripts/generate_mock_transactions.py --count 100 --execute
"""
import argparse
import os
import random
import subprocess
import sys
from datetime import date, timedelta

DEFAULT_DATABASE_URI = "postgresql://postgres:postgres@localhost:5432/personal_finance"
DESCRIPTIONS = [
    "Grocery Store", "Coffee Shop", "Gas Station", "Online Retailer", "Electric Utility",
    "Streaming Service", "Restaurant", "Pharmacy", "Gym Membership", "Insurance Payment",
    "Hardware Store", "Mobile Phone Bill", "Public Transit", "Bookstore", "Home Improvement",
]


def run_psql(database_uri, sql, tuples_only=True):
    cmd = ["psql", database_uri]
    if tuples_only:
        cmd += ["-t", "-A"]
    cmd += ["-c", sql]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    if result.returncode != 0:
        print(f"psql error: {result.stderr.strip()}", file=sys.stderr)
        sys.exit(1)
    return result.stdout.strip()


def introspect_columns(database_uri, table):
    rows = run_psql(
        database_uri,
        f"SELECT column_name, data_type, is_nullable, column_default "
        f"FROM information_schema.columns WHERE table_name = '{table}' "
        f"ORDER BY ordinal_position",
    )
    if not rows:
        print(f"No columns found for table '{table}' -- does the database/table exist?", file=sys.stderr)
        sys.exit(1)
    columns = []
    for line in rows.splitlines():
        name, data_type, is_nullable, default = (line.split("|") + [None] * 4)[:4]
        columns.append({
            "name": name, "type": data_type, "nullable": is_nullable == "YES", "default": default,
        })
    return columns


def sample_existing_id(database_uri, table, column="id"):
    result = run_psql(database_uri, f"SELECT {column} FROM {table} ORDER BY random() LIMIT 1")
    return int(result) if result else None


def sql_literal(value):
    if value is None:
        return "NULL"
    if isinstance(value, str):
        return "'" + value.replace("'", "''") + "'"
    return str(value)


def generate_value(col, account_id, category_id, merchant_id, start_date, end_date):
    name = col["name"]
    if name == "account_id":
        return account_id
    if name == "category_id":
        return category_id
    if name == "merchant_id":
        return merchant_id
    if name == "amount":
        return round(random.uniform(1.00, 500.00), 2)
    if name in ("date", "post_date"):
        delta_days = (end_date - start_date).days
        return (start_date + timedelta(days=random.randint(0, max(delta_days, 0)))).isoformat()
    if name == "description":
        return random.choice(DESCRIPTIONS)
    if name == "type":
        # matches the real chk_transaction_type constraint (V25) -- EXPENSE/INCOME are the two
        # everyday transaction types; TRANSFER* and ADJUSTMENT represent already-resolved
        # transfers/manual corrections, not the kind of volume this generator is for.
        return random.choices(["EXPENSE", "INCOME"], weights=[85, 15])[0]
    if name in ("created_at", "updated_at"):
        return None if col["nullable"] else "CURRENT_TIMESTAMP"
    if name == "deleted_at":
        return None
    if col["nullable"]:
        return None
    if col["default"] is not None:
        return None  # let the column's own DEFAULT apply -- omit it from the insert instead
    # Unrecognized NOT NULL column with no default -- don't guess silently.
    print(
        f"Warning: column '{name}' ({col['type']}) is NOT NULL with no default and isn't a "
        f"recognized pattern -- generated rows will need this value filled in by hand.",
        file=sys.stderr,
    )
    return "TODO_FILL_IN"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--count", type=int, required=True, help="Number of transactions to generate")
    parser.add_argument("--account-id", type=int, help="Account to attach transactions to (default: sample an existing one)")
    parser.add_argument("--category-id", type=int, help="Category to attach (default: sample an existing one, or NULL)")
    parser.add_argument("--merchant-id", type=int, help="Merchant to attach (default: NULL)")
    parser.add_argument("--start-date", default=(date.today() - timedelta(days=365)).isoformat())
    parser.add_argument("--end-date", default=date.today().isoformat())
    parser.add_argument("--database-uri", default=os.environ.get("DATABASE_URI", DEFAULT_DATABASE_URI))
    parser.add_argument("--batch-size", type=int, default=500, help="Rows per multi-row INSERT (matches the gold-source repository's chunking)")
    parser.add_argument("--execute", action="store_true", help="Run the generated SQL against --database-uri instead of printing it")
    args = parser.parse_args()

    start_date = date.fromisoformat(args.start_date)
    end_date = date.fromisoformat(args.end_date)

    columns = introspect_columns(args.database_uri, "transactions")
    column_names = [c["name"] for c in columns if c["name"] != "id"]

    account_id = args.account_id or sample_existing_id(args.database_uri, "accounts")
    if account_id is None:
        print("No account_id given and none found in the database -- create an account first.", file=sys.stderr)
        sys.exit(1)
    category_id = args.category_id if args.category_id is not None else sample_existing_id(args.database_uri, "categories")
    merchant_id = args.merchant_id

    statements = []
    for batch_start in range(0, args.count, args.batch_size):
        batch = list(range(batch_start, min(batch_start + args.batch_size, args.count)))
        rows_sql = []
        used_columns = None
        for _ in batch:
            row = {}
            for col in columns:
                if col["name"] == "id":
                    continue
                value = generate_value(col, account_id, category_id, merchant_id, start_date, end_date)
                if value is not None:
                    row[col["name"]] = value
            if used_columns is None:
                used_columns = list(row.keys())
            rows_sql.append("(" + ", ".join(sql_literal(row[c]) for c in used_columns) + ")")
        statements.append(
            f"INSERT INTO transactions ({', '.join(used_columns)}) VALUES\n"
            + ",\n".join(rows_sql) + ";"
        )

    full_sql = "\n\n".join(statements) + "\n"

    if args.execute:
        result = subprocess.run(["psql", args.database_uri], input=full_sql, capture_output=True, text=True)
        print(result.stdout)
        if result.returncode != 0:
            print(result.stderr, file=sys.stderr)
            sys.exit(1)
        print(f"Inserted {args.count} mock transactions into account {account_id}.", file=sys.stderr)
    else:
        print(full_sql)


if __name__ == "__main__":
    main()
