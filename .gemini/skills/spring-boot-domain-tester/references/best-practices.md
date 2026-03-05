# Spring Boot Domain Object Unit Testing Best Practices

Domain objects in this project map directly to the data model and often contain critical business logic. Follow these patterns to ensure robust testing.

## 1. Core Principles
- **Data Model Integrity**: Domain objects must strictly reflect the schema defined in `src/main/resources/db/migration/`. Always grep the migration scripts to confirm column types, nullability, and primary key structures.
- **No Spring Context**: Domain unit tests should be "Plain Old Java Object" (POJO) tests. Do NOT use `@SpringBootTest` or `@WebMvcTest`. Use only JUnit 5 and AssertJ/JUnit assertions.
- **Immutability Testing**: Verify that operations which "change" state actually return a NEW instance (using `toBuilder()`) rather than modifying the current one.
- **Exhaustive Branch Coverage**: Domain logic often contains conditional branching based on enums (e.g., `TransactionType`) or null checks. Ensure every branch is tested.

## 2. Testing Identity & Equality
- **ID-Based Equality**: Since domain objects use `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` on the `id`, verify that two objects with the same ID are considered equal even if other fields differ.
- **HashCode Consistency**: Ensure that equal objects return the same hash code.

## 3. Business Logic Testing
- **Information Expert**: Test methods that calculate values based on internal state (e.g., `Account.applyTransaction`, `Transaction.getNetChange`).
- **Null Safety**: Always test how methods handle `null` values for fields used in calculations. Verify they default to safe values like `BigDecimal.ZERO`.
- **Enum Transitions**: For objects with types or statuses, test every relevant enum constant to ensure the business logic handles them correctly.

## 4. Default Values
- **Builder Defaults**: Verify that optional fields have correct default values when created via a builder (e.g., balances defaulting to `ZERO`).

## 5. Organizational Structure
- **Group by Logic**: Use `@Nested` classes to group tests for specific methods or logic areas.
- **Descriptive Names**: Use `@DisplayName` to explain the business rule being verified (e.g., "should negate amount for EXPENSE type").
