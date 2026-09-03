---
name: spring-boot-repository-architect
description: Generates Spring Boot Repository layers utilizing Spring JDBC Client and manual RowMappers. Trigger this skill when writing data access logic to enforce the strict Anti-ORM (No Hibernate) architectural mandate.
---

# Spring Boot Repository Architect

This skill dictates the exact procedural steps and constraints for writing Data Access code in the backend.

## 🚨 Architectural Constraints
- **NO ORMs**: Do NOT use Hibernate, JPA, or `@Entity`.
- **JdbcClient**: All data access MUST be routed through the modern Spring `JdbcClient`.
- **Manual Mapping**: You MUST write manual `RowMapper<T>` implementations for every domain model. Do NOT use MapStruct or automated mappers.
- **Dynamic Queries**: For dynamic filtering, implement the `TransactionSpecification` (or equivalent) pattern using strict `StringBuilder` logic with parameterized SQL arguments.

## 🛠 Procedural Workflow
1. **Interface Definition**: Define the Repository interface outlining the contract.
2. **SQL Strategy**: Write raw, optimized PostgreSQL statements. Use `RETURNING *` for INSERTS/UPDATES to avoid secondary selects.
3. **Mapper Implementation**: Implement `RowMapper<T>` matching the domain model's immutable builder.
4. **Execution**: Use `jdbcClient.sql(query).params(params).query(Mapper.class).list()`.

## ⚠️ Gotchas
Mined from `references/repository-gold-source.java` (the real `TransactionRepository`) — read it
before writing anything non-trivial, but these are the parts easy to miss on a skim:
- **Userid-scoped methods get a throwing unscoped twin.** `findById(Long id)` throws
  `UnsupportedOperationException("Use findById with userId")`; `findById(Long id, Long userId)` is
  the real implementation. This isn't boilerplate — it's the IDOR guard: it makes the unscoped call
  a compile-time-reachable but runtime-guaranteed-wrong choice instead of silently missing the
  scoping. Any repository method returning user-owned data needs this pair, not just a single
  `userId` parameter on one overload.
- **SQL strings live in a dedicated `*Queries` class** (e.g. `TransactionQueries`), referenced as
  `TransactionQueries.FIND_BY_ID_WITH_DETAILS` etc. — not inlined as string literals in the
  repository method itself. Follow this even for a brand-new repository with only a couple of
  queries.
- **Batch inserts are a hand-built multi-row `VALUES` list**, chunked at 500 rows, with
  per-row-indexed named parameters (`:amount_0`, `:amount_1`, ...) in a `Map<String, Object>` — not
  `jdbcClient`'s per-item `.params(List<Map>)` looping and not a raw JDBC batch update. Copy this
  pattern for any new bulk-insert method rather than reinventing it.
- **Ad-hoc, non-domain query results** (a handful of columns that don't map to a real domain
  object, like a monthly sum grouped by type) use an inline `(rs, rowNum) -> ...` lambda instead of
  a dedicated `RowMapper<T>` class. The "manual `RowMapper<T>` for every domain model" rule is
  about domain models specifically — a one-off tuple doesn't need its own mapper class.
- **Dynamic sort columns go through a `switch` on the logical property name to a literal SQL
  column**, never string-interpolating the client-supplied sort key directly into the query — that
  switch is the injection guard for `ORDER BY`, which can't be parameterized the normal way.

## 📚 References
Agents must refer to the `references/` directory for gold-standard implementations of batch
inserts, paginated queries, and `pg_trgm` fuzzy searches.
- [Repository Gold-Source](references/repository-gold-source.java)
