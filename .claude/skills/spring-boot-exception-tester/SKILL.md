---
name: spring-boot-exception-tester
description: Generates high-quality unit tests for Spring Boot custom exceptions. Focuses on exhaustive constructor testing, message formatting, and 100% code coverage using the AAA pattern.
---

# Spring Boot Exception Tester

This skill guides the generation of unit tests for custom exception classes.

## Workflow

1.  **Analyze the Exception**: Read the target `Exception.java` to identify all constructors and custom fields.
2.  **Check Handler Registration**: `grep -n "@ExceptionHandler" src/main/java/.../exception/GlobalExceptionHandler.java` for this exception's own class. This is not optional — see Gotchas below.
3.  **Reference Best Practices**: Read [best-practices.md](references/best-practices.md) to understand core testing principles (AAA pattern, POJO-only).
4.  **Reference Gold Source**: Read [exception-gold-source.java](references/exception-gold-source.java) for implementation patterns.
5.  **Generate Test Class**:
    -   Do NOT use `@SpringBootTest`.
    -   Test every constructor explicitly.
    -   Verify message formatting for dynamic exceptions.
    -   Verify root cause preservation.
    -   Follow the AAA pattern with `// arrange`, `// act`, `// assert & verify` comments.
6.  **Validate**: Run the tests and check JaCoCo coverage.

## 🚨 Gotchas
-   **A well-tested exception class is not the same as a well-tested exception.** This skill's
    scope is the exception class itself (constructors, message formatting, cause preservation) —
    but a custom exception that's thrown from a Controller-reachable code path also needs a
    registered `@ExceptionHandler` in `GlobalExceptionHandler.java`, and that handler method needs
    its own test in `GlobalExceptionHandlerTest.java` (which instantiates the handler directly and
    calls its methods — not a full Spring context). This project found a missing-handler bug
    recur **4 times** independently (most recently `IllegalStateException`, which shipped as a
    generic 500 instead of a 409 until caught) — testing the exception class in isolation, exactly
    as steps 3-6 above describe, would not have caught any of the 4 occurrences. Step 2 exists
    specifically to close this gap: if `GlobalExceptionHandler.java` has no `@ExceptionHandler` for
    this exception and the exception is meant to be thrown from a Controller-reachable path, flag
    that as a likely gap — don't silently assume it's intentional.

## Key Requirements

-   **Fast Execution**: Tests must be pure JUnit 5 tests without Spring overhead.
-   **100% Coverage**: Every constructor must be exercised in at least one test case.
-   **Readability**: Use `@DisplayName` and explicit AAA comments.
-   **Handler coverage**: If a corresponding `@ExceptionHandler` exists or is needed, add/update
    its test in `GlobalExceptionHandlerTest.java` as part of the same change — not a separate,
    optional follow-up.

## Resources
- [Best Practices](references/best-practices.md)
- [Gold Source Example](references/exception-gold-source.java)
- [`GlobalExceptionHandlerTest.java`](../../../src/test/java/com/mayureshpatel/pfdataservice/exception/GlobalExceptionHandlerTest.java) — the real pattern for handler-level tests.
