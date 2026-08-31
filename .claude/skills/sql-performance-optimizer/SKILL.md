---
name: sql-performance-optimizer
description: Analyzes and refactors slow SQL queries. Trigger this skill when fixing database latency issues or when requested to optimize a Spring JdbcClient query.
---
# SQL Performance Optimizer

This skill dictates how to optimize raw PostgreSQL queries.

## 🚨 Constraints & Guardrails
- **No ORM Assumptions:** Remember we use raw SQL, not Hibernate.
- **Explain Analyze:** You MUST instruct the user to run `EXPLAIN ANALYZE` on the target query before modifying the code. Never guess performance bottlenecks.
- **Indices:** If suggesting an index, generate it as a Flyway migration script using plain
  `CREATE INDEX` — see Gotchas below on why `CONCURRENTLY` is not a safe default here.

## 🛠 Procedural Workflow
1. **Analyze:** Run `pf-data-service/scripts/explain_analyze.py "<query>"` — path is relative to
   the tri-repo root, **not** to this skill's own directory (there's nothing under
   `.claude/skills/sql-performance-optimizer/` besides `SKILL.md`; the script lives in
   `pf-data-service/scripts/`, alongside the rest of that repo's tooling, not inside the skill
   folder). If running with `pf-data-service` as the working directory already, it's just
   `./scripts/explain_analyze.py`. Bundled — shells out to `psql`, runs `EXPLAIN ANALYZE`, and
   flags every `Seq Scan` line itself, exit code 1 if any are found — rather than eyeballing raw
   `EXPLAIN ANALYZE` output for `Seq Scan` by hand.
2. **Refactor Code:** Rewrite the SQL string in the Spring Repository to use efficient `JOIN`s, filtering, or pagination (`LIMIT/OFFSET`).
3. **Migration:** If necessary, write a `V{next}__add_index.sql` Flyway script, then re-run step 1
   against the same query to confirm the Seq Scan is actually gone — don't assume the index fixed
   it without checking.

## ⚠️ Gotchas
- **`CREATE INDEX CONCURRENTLY` will fail every migration that uses it, as this project is
  currently configured — and Flyway does not handle this automatically, despite how it might
  sound.** Postgres refuses to run `CONCURRENTLY` inside a transaction block. Flyway's PostgreSQL
  support additionally takes an advisory lock around each migration, and that lock is itself
  transactional by default (`postgresql.transactional.lock`, defaults to `true`, per Flyway's own
  docs) — so both the migration's own transaction *and* Flyway's locking mechanism need addressing,
  not just one. This project sets neither `postgresql.transactional.lock` nor Flyway's general
  `mixed` option anywhere in `application*.yml`. Confirmed against real history: all 30 real
  migrations that add an index use plain `CREATE INDEX`; none has ever used `CONCURRENTLY`. Don't
  trust a same-turn claim that Flyway "detects and handles this automatically" without a citation —
  verify against Flyway's own docs first; a model reasoning from general Postgres knowledge alone
  produced exactly that confident, wrong claim while this Gotcha was being written. Only reach for
  `CONCURRENTLY` if you're also explicitly setting `postgresql.transactional.lock: false` (and
  likely `mixed: true`, since a lone `CONCURRENTLY` statement still needs to run outside the
  per-migration transaction) as part of the same change — treat that as a separate, deliberate
  config decision, not something to slip into a one-line index migration.
- Every existing index migration is a plain, single-purpose `CREATE INDEX ... ON table (column)` —
  no partial indexes, no `CONCURRENTLY`, no `INCLUDE` clauses, except where a migration explicitly
  calls out a *reason* for the exception (e.g. `V26` uses a partial index specifically because most
  queries filter `WHERE deleted_at IS NULL`, and says so in a comment). Match that: a plain index
  is the default; anything fancier gets a comment explaining why the default wasn't enough.
