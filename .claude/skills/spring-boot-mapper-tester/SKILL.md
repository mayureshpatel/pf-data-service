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
    -   Follow the AAA pattern with `// arrange`, `// act`, `// assert & verify` comments.
    -   Use `@Nested` classes for logical organization.
5.  **Validate**: Run the tests and check JaCoCo coverage.

## Key Requirements

-   **Fast Execution**: Tests must be pure unit tests.
-   **100% Coverage**: Every conditional branch and the utility constructor must be exercised.
-   **Precision**: Verify every field mapping explicitly.

## ✅ Validation Loop
Don't consider a generated test finished until it's actually been run — a mapper test that merely
compiles proves nothing about the mapping it's supposed to cover. This skill's own gold source had
a real bug this session (an `Account.builder()` call using two builder methods that don't exist on
the real class) that a validation loop would have caught on the first run. After writing or
changing a test class:
1. Run it in isolation: `./mvnw test -Dtest=<TestClassName>` from `pf-data-service/` (fast —
   skips the full suite, which is what `verify.sh` runs and which can take a while).
2. If it fails to compile: fix the compile error and re-run step 1. Don't guess at a second fix
   before seeing whether the first one worked — a builder call using a method that doesn't exist
   on the real domain class is exactly this kind of compile-time failure, not a runtime one.
3. If it compiles but fails: read the actual expected-vs-actual failure — a null-handling test
   failing (an optional field not guarded) tells you something different than a full-mapping
   field-by-field test failing (see
   [mapper-gold-source.java](references/mapper-gold-source.java)'s
   `assertEquals(account.getId(), dto.id())`-style assertions).
4. Repeat 1–3 until it passes. Only then is the test done — a green compile is not a green test.
5. Before moving on, confirm every field on the source object is actually asserted on the target,
   not just a handful — a full-mapping test that only checks 3 of the source's many fields isn't
   coverage, even if it passes.

## Resources
- [Best Practices](references/best-practices.md)
- [Gold Source Example](references/mapper-gold-source.java)
