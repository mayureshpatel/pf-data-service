---
name: spring-boot-mapper-tester
description: Generates high-quality unit tests for Spring Boot mappers. Focuses on exhaustive mapping coverage, null handling, and 100% code coverage using the AAA pattern.
---

# Spring Boot Mapper Tester

This skill guides the generation of unit tests for mapper classes that bridge domain and DTO layers.

## Workflow

1.  **Analyze the Mapper**: Read the target `Mapper.java` to identify all static mapping methods and branches (null checks).
2.  **Reference Best Practices**: Read [best-practices.md](references/best-practices.md) to understand core testing principles (AAA pattern, POJO-only, reflection for private constructors).
3.  **Reference Gold Source**: Read [mapper-gold-source.java](references/mapper-gold-source.java) for implementation patterns.
4.  **Generate Test Class**:
    -   Do NOT use Spring context.
    -   Test `null` input handling for every method.
    -   Test full mapping with all source fields populated.
    -   Test partial mapping with optional fields as `null`.
    -   Use reflection to test the private constructor.
    -   Follow the AAA pattern with `// Arrange`, `// Act`, `// Assert` comments.
    -   Use `@Nested` classes for logical organization.
5.  **Validate**: Run the tests and check JaCoCo coverage.

## Key Requirements

-   **Fast Execution**: Tests must be pure unit tests.
-   **100% Coverage**: Every conditional branch and the utility constructor must be exercised.
-   **Precision**: Verify every field mapping explicitly.

## Resources
- [Best Practices](references/best-practices.md)
- [Gold Source Example](references/mapper-gold-source.java)
