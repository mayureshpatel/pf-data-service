---
name: spring-boot-repository-tester
description: Generates high-quality integration tests for Spring Boot repositories using Testcontainers and a PostgreSQL baseline. Use when creating or updating repository tests to ensure 100% coverage, atomic data integrity, and complex aggregation verification.
---

# Spring Boot Repository Tester

This skill guides the creation of robust integration tests for the repository layer using a real PostgreSQL database via Testcontainers.

## Core Workflow

1.  **Inheritance**: ALWAYS extend `BaseRepositoryTest`. This provides the PostgreSQL container, seeds the data baseline, and configures the optimized `@JdbcTest` context.
2.  **Configuration**: Use `@Import` to pull in the specific Repository class being tested.
3.  **Data Baseline**: Tests run against `test-data-baseline.sql`. Familiarize yourself with the existing users (ID 1, 2), accounts, and categories in that file before writing assertions.
4.  **Pattern**: Group tests by functionality (e.g., Filtering, Aggregations, CRUD) using JUnit 5 `@Nested` classes.
5.  **AAA Pattern**: Explicitly label `// Arrange`, `// Act`, and `// Assert` sections in every test.

## Technical Standards

-   **Optimized Context**: Use `@JdbcTest` with the custom `includeFilters` for RowMappers and Queries defined in `BaseRepositoryTest`.
-   **Null Safety**: Verify that RowMappers handle optional fields (like `merchant_id` or `category_id`) without NPEs.
-   **Date Handling**: Use `OffsetDateTime` for all temporal fields to maintain consistency with the database's `TIMESTAMPTZ` columns.
-   **BigDecimal Equality**: Use `compareTo(other) == 0` for `BigDecimal` assertions to avoid scale-related failures.

## Reference Patterns

-   **Base Class**: See [repository-base-gold-source.java](references/repository-base-gold-source.java).
-   **Gold Standard**: See [transaction-repository-gold-source.java](references/transaction-repository-gold-source.java) for complex filtering and aggregation patterns.
-   **Data Baseline**: See [test-data-baseline.sql](references/test-data-baseline.sql) for available test data.
