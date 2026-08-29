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

## 📚 References
Agents must refer to the `references/` directory (if populated) for gold-standard implementations of batch inserts, paginated queries, and `pg_trgm` fuzzy searches.
