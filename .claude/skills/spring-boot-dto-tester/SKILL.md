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

## ✅ Validation Loop
Don't consider a generated test finished until it's actually been run — a DTO test that merely
compiles proves nothing about the constraints or field mapping it's supposed to cover. This
skill's own gold source and best-practices reference both had real bugs this session (a missing
AAA section; an example that wouldn't compile against the record's real field count) that running
the test would have caught immediately, rather than only being caught by chance. After writing or
changing a test class:
1. Run it in isolation: `./mvnw test -Dtest=<TestClassName>` from `pf-data-service/` (fast —
   skips the full suite, which is what `verify.sh` runs and which can take a while).
2. If it fails to compile: fix the compile error and re-run step 1. Don't guess at a second fix
   before seeing whether the first one worked — a record constructor mismatch (wrong field count
   or order) is a compile error, not a runtime one, and won't show up until you actually try to
   build it.
3. If it compiles but fails: read the actual expected-vs-actual failure — a `@Size`/`@NotBlank`
   constraint test failing (check `violations.stream().anyMatch(v ->
   v.getPropertyPath().toString().equals("fieldName"))`, per
   [dto-gold-source.java](references/dto-gold-source.java)) means something different than a
   record field-mapping test failing; the fix is different for each.
4. Repeat 1–3 until it passes. Only then is the test done — a green compile is not a green test.
5. Before moving on, confirm every validation annotation on the DTO actually has a corresponding
   negative test case exercising it (not just the happy path) — a validator test suite that only
   calls `shouldPassWithValidData` isn't coverage.

## Resources
- [Best Practices](references/best-practices.md)
- [Gold Source Example](references/dto-gold-source.java)
