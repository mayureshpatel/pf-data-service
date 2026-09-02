---
name: spring-boot-security-tester
description: Generates high-quality unit tests for Spring Boot security components (Services, UserDetails). Focuses on JWT logic, ownership verification, and 100% code coverage using the AAA pattern.
---

# Spring Boot Security Tester

This skill guides the generation of unit tests for security-critical components.

## Workflow

1.  **Analyze the Component**: Read the target `SecurityService.java`, `JwtService.java`, or
    `JwtAuthenticationFilter.java` to identify security rules, parsing logic, repository calls, or
    response-shaping logic.
2.  **Identify the test category before writing anything** — see Gotchas below. Ownership checks
    and JWT/filter logic are unit-testable in isolation (steps 3-5 apply). Whether an *endpoint* is
    reachable without authentication is a different category this skill does not cover with a unit
    test — see Gotchas.
3.  **Reference Best Practices**: Read [best-practices.md](references/best-practices.md) to understand core testing principles (AAA pattern, POJO-only for JWT, Mockito for ownership).
4.  **Reference Gold Source**: Read [security-gold-source.java](references/security-gold-source.java) for implementation patterns.
5.  **Generate Test Class**:
    -   Use `@Nested` classes for logical grouping (e.g., `TokenLogic`, `OwnershipChecks`,
        `ResponseBodyConsistency`).
    -   For high-logic services, use fast POJO tests with `ReflectionTestUtils`.
    -   For record-based checks, use Mockito to verify repository interactions.
    -   For components that produce an HTTP response body on failure (e.g.
        `JwtAuthenticationFilter`'s 401 response), assert the actual response body shape is
        consistent with the rest of the app's error format, not just the status code — see
        `JwtAuthenticationFilterTest.java` for the real pattern.
    -   Follow the AAA pattern with `// arrange`, `// act`, `// assert & verify` comments.
6.  **Validate**: Run the tests and check JaCoCo coverage.

## 🚨 Gotchas
-   **Ownership/JWT-logic tests and endpoint-exposure tests are different categories — don't try
    to write the second kind as a unit test.** This skill's ownership-check guidance is real and
    specific, and is the exact kind of check that should catch an IDOR-style gap if applied to the
    right method. But "is this endpoint actually reachable without authentication in this
    environment" (e.g. is Swagger UI locked down in `prod`) is not answerable by a POJO/Mockito
    unit test — it requires a real Spring context and profile activation. This project's actual
    pattern for that category is `SwaggerProdProfileIntegrationTest` /
    `SwaggerNonProdProfileIntegrationTest` (Testcontainers-backed, `@ActiveProfiles`-driven,
    asserting real HTTP status codes per profile) — a genuinely different test shape than
    everything else in this skill. If asked to verify endpoint exposure, point to that pattern
    rather than forcing it into a unit test that can't actually answer the question.
-   **Response-body consistency is a real, previously-missed category.** A security component
    returning an inconsistent error-response shape (e.g. `JwtAuthenticationFilter` once returned a
    different 401 body shape than the rest of the app) is a real bug class this skill didn't
    originally cover — it only tested status/authorization outcomes, not response *shape*.

## Key Requirements

-   **Isolation**: Keep security tests isolated from the full Spring filter chain when possible —
    except for endpoint-exposure checks, which need the opposite (see Gotchas).
-   **Exhaustive Scenarios**: Test unauthorized access, expired tokens, and incorrect ownership in addition to happy paths.
-   **Response shape**: For any component that produces its own HTTP response body, assert that
    shape explicitly — a passing status-code check alone can hide a real inconsistency.
-   **100% Coverage**: Ensure every branch of security logic is exercised.

## Resources
- [Best Practices](references/best-practices.md)
- [Gold Source Example](references/security-gold-source.java)
- [`JwtAuthenticationFilterTest.java`](../../../src/test/java/com/mayureshpatel/pfdataservice/security/JwtAuthenticationFilterTest.java) — real response-body-consistency test pattern.
