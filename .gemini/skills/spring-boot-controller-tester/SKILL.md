---
name: spring-boot-controller-tester
description: Generates high-quality, robust Spring Boot controller unit tests following project-specific best practices. Use when creating or updating controller tests to ensure 100% coverage, security verification, and standardized structure.
---

# Spring Boot Controller Tester

This skill guides the generation of "Gold Standard" unit tests for Spring Boot controllers.

## Workflow

1.  **Analyze the Controller**: Read the target `Controller.java` to identify all endpoints, mappings, and service dependencies.
2.  **Analyze DTOs & Schema**: 
    - Read the associated Request and Response DTOs to understand validation constraints and expected JSON structure.
    - Research the Flyway migration scripts in `src/main/resources/db/migration/` to ensure that Request DTO validation annotations (e.g., `@Size`, `@NotBlank`) match the database column constraints (e.g., `VARCHAR(255)`, `NOT NULL`).
3.  **Reference Best Practices**: Read [best-practices.md](references/best-practices.md) to understand the required test patterns (Nested classes, MockMvc, Security).
4.  **Reference Gold Source**: Read [account-controller-gold-source.java](references/account-controller-gold-source.java) for a concrete implementation example.
5.  **Generate Test Class**:
    -   Inherit from `BaseControllerTest`.
    -   Use `@Nested` classes for each endpoint.
    -   Include validation tests (positive and negative).
    -   Include security tests (CSRF, authenticated user context).
    -   Include error handling tests (403, 404, 500).
6.  **Validate**: Run the generated tests and check coverage.

## Key Requirements

-   **Contextual Integrity**: Ensure the test uses the project's existing `BaseControllerTest` and `WithCustomMockUser`.
-   **Mocking**: Use `@MockitoBean` (or the project's equivalent) to mock service layers.
-   **Comprehensive Assertions**: Use `jsonPath` to verify nested DTO fields and collection sizes.
-   **Coverage**: Aim for 100% code and branch coverage.

## Resources
- [Best Practices](references/best-practices.md)
- [Gold Source Example](references/account-controller-gold-source.java)
