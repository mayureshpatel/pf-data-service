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

## ✅ Validation Loop
Don't consider a generated test finished until it's actually been run — a controller test that
merely compiles proves nothing about the endpoint it's supposed to cover. After writing or
changing a test class:
1. Run it in isolation: `./mvnw test -Dtest=<TestClassName>` from `pf-data-service/` (fast —
   skips the full suite, which is what `verify.sh` runs and which can take a while).
2. If it fails to compile: fix the compile error and re-run step 1. Don't guess at a second fix
   before seeing whether the first one worked.
3. If it compiles but fails: read the actual assertion failure (expected vs. actual), not just the
   test name — a `jsonPath` assertion failing (e.g.
   `jsonPath("$[0].name").value("Checking Account")` not matching the real response body) means
   the response shape or field mapping is wrong; `status().isOk()` failing against a real 403/500
   means the security/mocking setup is wrong. These are different bugs with different fixes — don't
   patch one when the failure is actually the other.
4. Repeat 1–3 until it passes. Only then is the test done — a green compile is not a green test.
5. Before moving on, confirm it's asserting *behavior* (the right status code, the right response
   body shape via `jsonPath`, the right security/ownership outcome) and not just that the call
   didn't throw — a test that only checks `status().is2xxSuccessful()` with no body or security
   assertion isn't coverage.

## Resources
- [Best Practices](references/best-practices.md)
- [Gold Source Example](references/account-controller-gold-source.java)
