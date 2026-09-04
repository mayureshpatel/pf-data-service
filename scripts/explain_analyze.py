#!/usr/bin/env python3
"""
Runs EXPLAIN ANALYZE against the target query and flags Sequential Scans.

Bundles the procedure sql-performance-optimizer's SKILL.md previously only described in prose
("parse the EXPLAIN ANALYZE output to identify Sequential Scans"). Uses `psql` (not psycopg2 --
not a project dependency).

Usage:
    ./scripts/explain_analyze.py "SELECT * FROM transactions WHERE account_id = 1"
    ./scripts/explain_analyze.py --file query.sql
    echo "SELECT ..." | ./scripts/explain_analyze.py
"""
import argparse
import os
import subprocess
import sys

DEFAULT_DATABASE_URI = "postgresql://postgres:postgres@localhost:5432/personal_finance"


def run_explain_analyze(database_uri, query):
    query = query.strip().rstrip(";")
    result = subprocess.run(
        ["psql", database_uri, "-c", f"EXPLAIN ANALYZE {query};"],
        capture_output=True, text=True, timeout=60,
    )
    if result.returncode != 0:
        print(f"psql error: {result.stderr.strip()}", file=sys.stderr)
        sys.exit(1)
    return result.stdout


def find_seq_scans(explain_output):
    return [line.strip() for line in explain_output.splitlines() if "Seq Scan" in line]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("query", nargs="?", help="The SQL query to analyze (or use --file / stdin)")
    parser.add_argument("--file", help="Read the query from a file instead of an argument")
    parser.add_argument("--database-uri", default=os.environ.get("DATABASE_URI", DEFAULT_DATABASE_URI))
    args = parser.parse_args()

    if args.file:
        with open(args.file) as f:
            query = f.read()
    elif args.query:
        query = args.query
    elif not sys.stdin.isatty():
        query = sys.stdin.read()
    else:
        parser.error("Provide a query as an argument, --file, or via stdin.")

    output = run_explain_analyze(args.database_uri, query)
    print(output)

    seq_scans = find_seq_scans(output)
    if seq_scans:
        print(f"⚠️  {len(seq_scans)} Sequential Scan(s) found:", file=sys.stderr)
        for line in seq_scans:
            print(f"    {line}", file=sys.stderr)
        print(
            "See sql-performance-optimizer's Gotchas before adding an index: use plain "
            "CREATE INDEX, not CONCURRENTLY, unless postgresql.transactional.lock: false is "
            "added to the Flyway config first.",
            file=sys.stderr,
        )
        sys.exit(1)
    else:
        print("No Sequential Scans found.", file=sys.stderr)


if __name__ == "__main__":
    main()
