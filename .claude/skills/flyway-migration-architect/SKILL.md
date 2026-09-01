---
name: flyway-migration-architect
description: Generates Flyway SQL migration scripts. Trigger this skill when altering the database schema to ensure PostgreSQL-specific syntax and strict versioning rules.
---

# Flyway Migration Architect

This skill guides the creation of structural database changes.

## 🚨 Architectural Constraints
- **Dialect**: Strictly use PostgreSQL syntax.
- **Naming Convention**: Files MUST be named `V{version}__{description}.sql` (e.g., `V1__init_schema.sql`). Note the double underscore.
- **Casing**: All tables, columns, and indices MUST use `snake_case`.
- **Immutability**: NEVER modify an existing, committed Flyway script. Always create a new script (e.g., `V2`, `V3`) to alter tables.

## 🛠 Procedural Workflow
1. **Types**: Use `BIGSERIAL` for primary keys (not `UUID` — see Gotchas), `VARCHAR`, `TIMESTAMPTZ`, and `NUMERIC(19,2)` for financial math.
2. **Constraints**: Always define explicit `FOREIGN KEY`, `UNIQUE`, and `CHECK` constraints to enforce data integrity at the lowest level.
3. **Indices**: Add `CREATE INDEX` statements for any foreign key or heavily filtered column.

## 📚 References
See `references/migration-gold-source.sql` for a real, complete migration demonstrating naming
convention, `FOREIGN KEY` constraints, `CREATE INDEX` (including partial indexes), and table
documentation via `COMMENT ON TABLE`.

## ⚠️ Gotchas
- **No table in this schema uses `UUID` anywhere.** Every primary key is `BIGSERIAL`; lookup
  tables use a natural key instead (`account_types.code VARCHAR(20) PRIMARY KEY`,
  `currencies.code CHAR(3) PRIMARY KEY`). Confirmed by direct search across every real migration —
  don't introduce `UUID` on the assumption it's a modern-Postgres default this project follows.
- **Money columns are `NUMERIC(19,2)` or `DECIMAL(19,2)` everywhere in this schema — two decimal
  places, not four.** Every real migration from `V1__init_schema.sql` onward uses `(19, 2)`;
  `DECIMAL(19,4)` would be a real, live inconsistency with every existing money column, not a
  stricter version of the same thing. `NUMERIC` and `DECIMAL` are interchangeable in Postgres —
  both spellings appear in real migrations — but the precision must match.
- **The next version number is not always "count the files plus one."** Check the actual highest
  `V{n}` in `src/main/resources/db/migration/` directly (currently `V33`) — don't infer it from a
  stale reference or from `target/classes/db/migration/` (build output; can be stale/duplicated
  relative to the real source directory).
- **Timestamps were migrated to `TIMESTAMPTZ` in `V28`.** If copying a pattern from a migration
  older than `V28`, check whether it still uses a plain `TIMESTAMP` — that's the pre-migration
  convention, not the current one.
- **No repeatable (`R__`) migrations exist in this project.** If a task genuinely calls for one,
  it'll be the first — double-check that a plain versioned `V{n}__` migration isn't actually the
  right tool before introducing a new migration *kind* to the codebase.
