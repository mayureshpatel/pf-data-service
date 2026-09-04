---
name: mock-data-generator
description: Generates deterministic, high-volume mock data for local testing. Trigger this skill when asked to seed the database or test performance with large datasets.
---
# Mock Data Generator

This skill ensures the local developer environment can handle production-scale volumes without relying on manual data entry.

## 🚨 Constraints & Guardrails
- **No Embedded Databases:** The mock data must be generated for the target PostgreSQL schema.
- **Relational Integrity:** You MUST strictly adhere to Foreign Key constraints. Transactions must map to valid Users, Accounts, and Categories.
- **Volume:** If asked for high-volume data (e.g., 5000+ rows), rely on procedural generation rather than writing 5000 individual `INSERT` lines.

## 🛠 Procedural Workflow
1. **Analyze Schema:** Query the MCP server or review Flyway migrations to understand the current relational schema.
2. **Choose Generator Method:**
   - For low volume (< 50 rows): Generate a standard Flyway repeatable migration script (e.g., `R__seed_mock_data.sql`).
   - For high volume (> 50 rows) into the actual local dev database: use the bundled
     `pf-data-service/scripts/generate_mock_transactions.py` (path is relative to the tri-repo
     root, not to this skill's own directory — the script lives in that repo's `scripts/`, not
     under `.claude/skills/mock-data-generator/`; if already working with `pf-data-service` as
     the cwd, it's just `./scripts/generate_mock_transactions.py`). It introspects the real
     current schema via `psql`, so it doesn't go stale as migrations change columns — see its
     `--help` for options. Extend the same introspect-then-generate pattern to other tables if the
     task needs them.
   - For high volume needed *inside an automated integration test* specifically (not the
     developer's persistent local DB) — seed through a real repository/service call in a
     Testcontainers-backed test instead; that data is ephemeral per test run, which is what you
     want there and not what you want for interactive local development.
3. **Execute:** Run the generated script (or `--execute` its output directly) against the local developer DB.

## ⚠️ Gotchas
- **Neither Java Faker nor DataFaker is a project dependency** — `pom.xml` has no Faker library at
  all. Code that calls one won't compile. Generate field values procedurally instead (loops over a
  small hand-written array of realistic sample values, or arithmetic on a seeded `Random`) rather
  than assuming a Faker import is available. If a task genuinely needs Faker-quality realism, that
  starts with a separate, deliberate "add this dependency" step — not something to add silently
  inside a data-generation task.
- **No `R__` repeatable migration exists anywhere in this project yet.** The low-volume path
  above would be the first. Double check a plain `V{next}__seed_*.sql` versioned migration isn't
  actually the better fit before introducing a new migration kind — a repeatable migration re-runs
  on every checksum change, which is rarely what a one-off seed actually wants.
- **Testcontainers (`org.testcontainers`, `spring-boot-testcontainers`) is already a real
  dependency**, used for backend integration tests — reach for it specifically when the volume is
  needed *inside a test run* (ephemeral, per-test data through the real repository layer), not for
  seeding the developer's actual persistent local database — that's what
  `generate_mock_transactions.py` is for.
- **`generate_mock_transactions.py` needs `psql` on `PATH`**, not `psycopg2` (also not a project
  dependency). It shells out to `psql` for both schema introspection and (with `--execute`)
  running the generated inserts.
- **The `postgres` MCP server needs a one-time interactive approval and isn't guaranteed connected
  even then.** It previously showed `status: failed` on init — that root cause (missing `uvx`, a
  Python-3.14/`pglast` build failure, an `mcp`-package version mismatch) was fixed under PF-179;
  current sessions show `status: pending` instead (normal — awaiting the one-time approval, not a
  failure). Still, don't treat an MCP schema query as guaranteed available in any given session;
  fall back to reading the Flyway migrations directly (they're the actual source of truth for the
  schema regardless of MCP availability).
