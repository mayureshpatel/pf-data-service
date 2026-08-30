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
5.  **Validate**: Run the tests and check JaCoCo coverage.

## Key Requirements

-   **Pure Unit Tests**: Keep tests fast by avoiding the Spring `ApplicationContext`.
-   **100% Coverage**: Every conditional branch (if/else, switch, ternary) must have a test case.
-   **Immutability Verification**: Explicitly assert that the original object is not modified.

## Resources
- [Best Practices](references/best-practices.md)
- [Gold Source Example](references/domain-gold-source.java)
