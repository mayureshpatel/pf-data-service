---
name: spring-boot-exception-tester
description: Generates high-quality unit tests for Spring Boot custom exceptions. Focuses on exhaustive constructor testing, message formatting, and 100% code coverage using the AAA pattern.
---

# Spring Boot Exception Tester

This skill guides the generation of unit tests for custom exception classes.

## Workflow

1.  **Analyze the Exception**: Read the target `Exception.java` to identify all constructors and custom fields.
2.  **Reference Best Practices**: Read [best-practices.md](references/best-practices.md) to understand core testing principles (AAA pattern, POJO-only).
3.  **Reference Gold Source**: Read [exception-gold-source.java](references/exception-gold-source.java) for implementation patterns.
4.  **Generate Test Class**:
    -   Do NOT use `@SpringBootTest`.
    -   Test every constructor explicitly.
    -   Verify message formatting for dynamic exceptions.
    -   Verify root cause preservation.
    -   Follow the AAA pattern with `// Arrange`, `// Act`, `// Assert` comments.
5.  **Validate**: Run the tests and check JaCoCo coverage.

## Key Requirements

-   **Fast Execution**: Tests must be pure JUnit 5 tests without Spring overhead.
-   **100% Coverage**: Every constructor must be exercised in at least one test case.
-   **Readability**: Use `@DisplayName` and explicit AAA comments.

## Resources
- [Best Practices](references/best-practices.md)
- [Gold Source Example](references/exception-gold-source.java)
