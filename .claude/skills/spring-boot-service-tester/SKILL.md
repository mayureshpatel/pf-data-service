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
    -   Explicitly label sections with `// arrange`, `// act`, and `// assert & verify`.
5.  **Validate**: Run the tests and check JaCoCo coverage.

## Key Requirements

-   **Business Logic Verification**: Ensure logic that calculates values or enforces rules is thoroughly tested.
-   **Security & Ownership**: Explicitly test scenarios where users attempt to access/modify resources they do not own.
-   **100% Coverage**: Every conditional branch (if/else, ternary, etc.) must have a corresponding test case.

## ✅ Validation Loop
Don't consider a generated test finished until it's actually been run — a service test that
merely compiles proves nothing about the business logic it's supposed to cover, especially given
this skill's own sibling architect (`spring-boot-service-architect`) was PF-EPIC-038's most severe
finding (a fabricated `@DomainEvents` pattern). After writing or changing a test class:
1. Run it in isolation: `./mvnw test -Dtest=<TestClassName>` from `pf-data-service/` (fast —
   skips the full suite, which is what `verify.sh` runs and which can take a while).
2. If it fails to compile: fix the compile error and re-run step 1. Don't guess at a second fix
   before seeing whether the first one worked.
3. If it compiles but fails: read the actual expected-vs-actual failure. A test can pass for the
   wrong reason with Mockito specifically — a mock that's never actually verified (e.g. missing
   `verify(transactionRepository).insert(any(Transaction.class))`, per
   [service-gold-source.java](references/service-gold-source.java)) proves the code didn't throw,
   not that it called the right dependency with the right arguments.
4. Repeat 1–3 until it passes. Only then is the test done — a green compile is not a green test.
5. Before moving on, confirm ownership/access-denial paths are asserted with a real exception
   check (`assertThrows(AccessDeniedException.class, ...)`, not just that the happy path returns
   something) — a service test suite with no denial-path coverage isn't exercising the security
   logic it exists to protect.

## Resources
- [Best Practices](references/best-practices.md)
- [Gold Source Example](references/service-gold-source.java)
