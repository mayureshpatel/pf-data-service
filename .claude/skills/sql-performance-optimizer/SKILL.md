---
name: sql-performance-optimizer
description: Analyzes and refactors slow SQL queries. Trigger this skill when fixing database latency issues or when requested to optimize a Spring JdbcClient query.
---
# SQL Performance Optimizer

This skill dictates how to optimize raw PostgreSQL queries.

## 🚨 Constraints & Guardrails
- **No ORM Assumptions:** Remember we use raw SQL, not Hibernate.
- **Explain Analyze:** You MUST instruct the user to run `EXPLAIN ANALYZE` on the target query before modifying the code. Never guess performance bottlenecks.
- **Indices:** If suggesting an index, it MUST be generated as a Flyway migration script using `CREATE INDEX CONCURRENTLY`.

## 🛠 Procedural Workflow
1. **Analyze:** Parse the `EXPLAIN ANALYZE` output to identify Sequential Scans (Seq Scan).
2. **Refactor Code:** Rewrite the SQL string in the Spring Repository to use efficient `JOIN`s, filtering, or pagination (`LIMIT/OFFSET`).
3. **Migration:** If necessary, write a `V{next}__add_index.sql` Flyway script.
