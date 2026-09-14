---
name: spring-boot-domain-tester
description: Generates high-quality unit tests for Spring Boot domain objects (Entities). Focuses on logic, immutability, equality (ID-based), and 100% branch coverage without requiring a Spring context.
---

# Spring Boot Domain Object Tester

This skill guides the generation of unit tests for domain objects that map to the data model.

## Workflow

1.  **Analyze the Domain Object**: Read the target `DomainObject.java` to identify fields, builders, equality logic, and business methods.
2.  **Verify Data Model Integrity**: Research the Flyway migration scripts in `src/main/resources/db/migration/` to ensure the domain object's fields and types map correctly to the database schema.
3.  **Reference Best Practices**: Read [best-practices.md](references/best-practices.md) to understand core testing principles (Immutability, POJO-only).
4.  **Reference Gold Source**: Read [domain-gold-source.java](references/domain-gold-source.java) for implementation patterns.
5.  **Generate Test Class**:
    -   Do NOT use `@SpringBootTest` or Mockito extensions if not needed.
    -   Test constructor and builder defaults.
    -   Test ID-based equality (`equals` and `hashCode`).
    -   Test all business methods with exhaustive branch coverage (including null scenarios).
    -   Verify immutability (ops return new instances).
6.  **Validate**: Run the tests and check JaCoCo coverage.

## Key Requirements

-   **Pure Unit Tests**: Keep tests fast by avoiding the Spring `ApplicationContext`.
-   **100% Coverage**: Every conditional branch (if/else, switch, ternary) must have a test case.
-   **Immutability Verification**: Explicitly assert that the original object is not modified.

## ✅ Validation Loop
Don't consider a generated test finished until it's actually been run — a domain-object test that
merely compiles proves nothing about the behavior it's supposed to cover. After writing or
changing a test class:
1. Run it in isolation: `./mvnw test -Dtest=<TestClassName>` from `pf-data-service/` (fast —
   skips the full suite, which is what `verify.sh` runs and which can take a while). No Spring
   context or Testcontainers here, so this is especially cheap for this skill.
2. If it fails to compile: fix the compile error and re-run step 1. Don't guess at a second fix
   before seeing whether the first one worked.
3. If it compiles but fails: read the actual assertion failure (expected vs. actual), not just the
   test name — a business-method output mismatch and a broken `equals`/`hashCode` contract fail
   for different reasons and need different fixes.
4. Repeat 1–3 until it passes. Only then is the test done — a green compile is not a green test.
5. Before moving on, confirm immutability is asserted *completely*, not partially — see
   [domain-gold-source.java](references/domain-gold-source.java)'s
   `applyTransaction_shouldReturnNewInstance`: `assertNotSame(original, updated, ...)` alone isn't
   enough, and neither is only checking `updated`'s new value. A test that checks only the new
   instance's value and never re-asserts the original's own value is unchanged would still pass
   against a method that mutates `this` in place and returns it — both `assertNotSame` *and* the
   original's own unchanged value need to be checked together.

## Resources
- [Best Practices](references/best-practices.md)
- [Gold Source Example](references/domain-gold-source.java)
