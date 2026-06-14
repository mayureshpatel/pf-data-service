---
name: spring-boot-service-tester
description: Generates high-quality unit tests for Spring Boot service layers. Focuses on business logic, orchestration, ownership verification, and 100% branch coverage using Mockito and the AAA pattern.
---

# Spring Boot Service Tester

This skill guides the generation of unit tests for service layer classes.

## Workflow

1.  **Analyze the Service**: Read the target `Service.java` to identify all public methods, dependencies (repositories/services), and business rules.
2.  **Reference Best Practices**: Read [best-practices.md](references/best-practices.md) to understand core testing principles (AAA pattern, Mockito isolation, `@Nested` organization).
3.  **Reference Gold Source**: Read [service-gold-source.java](references/service-gold-source.java) for implementation patterns.
4.  **Generate Test Class**:
    -   Use `@ExtendWith(MockitoExtension.class)`.
    -   Mock all constructor-injected dependencies using `@Mock`.
    -   Use `@InjectMocks` for the target service.
    -   Create exhaustive tests for every public method using `@Nested` classes.
    -   Test happy paths, error paths (Exceptions), and edge cases (nulls, empty lists).
    -   Explicitly label sections with `// Arrange`, `// Act`, and `// Assert`.
5.  **Validate**: Run the tests and check JaCoCo coverage.

## Key Requirements

-   **Business Logic Verification**: Ensure logic that calculates values or enforces rules is thoroughly tested.
-   **Security & Ownership**: Explicitly test scenarios where users attempt to access/modify resources they do not own.
-   **100% Coverage**: Every conditional branch (if/else, ternary, etc.) must have a corresponding test case.

## Resources
- [Best Practices](references/best-practices.md)
- [Gold Source Example](references/service-gold-source.java)
