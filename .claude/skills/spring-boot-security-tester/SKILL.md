---
name: spring-boot-security-tester
description: Generates high-quality unit tests for Spring Boot security components (Services, UserDetails). Focuses on JWT logic, ownership verification, and 100% code coverage using the AAA pattern.
---

# Spring Boot Security Tester

This skill guides the generation of unit tests for security-critical components.

## Workflow

1.  **Analyze the Component**: Read the target `SecurityService.java` or `JwtService.java` to identify security rules, parsing logic, or repository calls.
2.  **Reference Best Practices**: Read [best-practices.md](references/best-practices.md) to understand core testing principles (AAA pattern, POJO-only for JWT, Mockito for ownership).
3.  **Reference Gold Source**: Read [security-gold-source.java](references/security-gold-source.java) for implementation patterns.
4.  **Generate Test Class**:
    -   Use `@Nested` classes for logical grouping (e.g., `TokenLogic`, `OwnershipChecks`).
    -   For high-logic services, use fast POJO tests with `ReflectionTestUtils`.
    -   For record-based checks, use Mockito to verify repository interactions.
    -   Follow the AAA pattern with `// Arrange`, `// Act`, `// Assert` comments.
5.  **Validate**: Run the tests and check JaCoCo coverage.

## Key Requirements

-   **Isolation**: Keep security tests isolated from the full Spring filter chain when possible.
-   **Exhaustive Scenarios**: Test unauthorized access, expired tokens, and incorrect ownership in addition to happy paths.
-   **100% Coverage**: Ensure every branch of security logic is exercised.

## Resources
- [Best Practices](references/best-practices.md)
- [Gold Source Example](references/security-gold-source.java)
