---
name: spring-boot-dto-tester
description: Generates high-quality unit tests for Spring Boot DTOs (Request/Response). Focuses on Jakarta Validation constraints, data model alignment (via Flyway research), and 100% code coverage.
---

# Spring Boot DTO Tester

This skill guides the generation of unit tests for DTOs, ensuring they enforce the API contract and database constraints.

## Workflow

1.  **Analyze the DTO**: Read the target `Dto.java` or `Request.java` to identify fields and validation annotations.
2.  **Verify Schema Alignment**: Research the Flyway migration scripts in `src/main/resources/db/migration/` to ensure that validation constraints (e.g., `@Size`, `@NotNull`) match the database column limits.
3.  **Reference Best Practices**: Read [best-practices.md](references/best-practices.md) to understand DTO testing patterns (Validator-based testing).
4.  **Reference Gold Source**: Read [dto-gold-source.java](references/dto-gold-source.java) for implementation examples.
5.  **Generate Test Class**:
    -   Use `jakarta.validation.Validator` for Request DTOs.
    -   Include a `shouldPassWithValidData` test.
    -   Include `@Nested` classes for field-specific validation failures.
    -   For Response DTOs (records), verify field mapping and structure.
6.  **Validate**: Run the tests and check coverage.

## Key Requirements

-   **Constraint Verification**: Every validation annotation (e.g., `@NotBlank`, `@Positive`) must have at least one corresponding test case.
-   **Schema Consistency**: If a database column is `VARCHAR(50)`, the DTO must have `@Size(max = 50)` and the test must verify this limit.
-   **No Spring Context**: Keep tests fast by using pure JUnit 5 and the standard Validator factory.

## Resources
- [Best Practices](references/best-practices.md)
- [Gold Source Example](references/dto-gold-source.java)
