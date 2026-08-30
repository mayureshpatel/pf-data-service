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
1. **Types**: Use `UUID`, `VARCHAR`, `TIMESTAMPTZ`, and `DECIMAL(19,4)` for financial math.
2. **Constraints**: Always define explicit `FOREIGN KEY`, `UNIQUE`, and `CHECK` constraints to enforce data integrity at the lowest level.
3. **Indices**: Add `CREATE INDEX` statements for any foreign key or heavily filtered column.
