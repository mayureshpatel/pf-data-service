# Senior Software Engineer Code Review

**Date:** January 19, 2026
**Project:** Personal Finance Data Service (Backend MVP)
**Version:** Spring Boot 3.5.3 | Java 21

## Executive Summary
The codebase is in excellent shape for a "Hardening" phase. The recent addition of **Unit Tests** achieving >90% coverage on core logic is a major win. The application correctly implements a layered architecture (Controller -> Service -> Repository) and adheres to modern Spring Boot practices.

However, the removal of Integration Tests (Testcontainers) leaves a gap in verifying end-to-end flows (specifically DB interactions). Additionally, the **Scheduler** for snapshots is implemented but not enabled, rendering the feature dormant.

## 1. Architectural Integrity

### Strengths
*   **Immutability:** Extensive use of Java `records` for DTOs (`TransactionDto`, `DashboardData`).
*   **Stateless Design:** Services are stateless singleton beans, relying on the DB for state, which is correct.
*   **Separation of Concerns:** `TransactionImportService` separates heavy parsing logic from CRUD logic in `TransactionService`.

### Areas for Improvement
*   **Manual Mapping:** `TransactionService.mapToDto` is boilerplat-y. As the domain grows, this will become a maintenance burden.
    *   *Recommendation:* Adopt **MapStruct** for type-safe, compile-time mapping.
*   **Dormant Features:** `SnapshotService` contains valuable business logic but is never invoked. The `PfDataServiceApplication` is missing `@EnableScheduling`.

## 2. Code Quality & Security

### Strengths
*   **Security Fixes:** Recent changes to `TransactionService` added explicit ownership checks (`!transaction.getAccount().getUser().getId().equals(userId)`), fixing a critical IDOR vulnerability.
*   **Validation:** DTOs are properly annotated with `jakarta.validation` annotations (`@NotNull`).
*   **Soft Deletes:** `User`, `Account`, and `Transaction` use Hibernate's `@SQLDelete` and `@SQLRestriction` for robust soft deletion.

### Areas for Improvement
*   **Entity Validation:** While DTOs are validated, Entities like `User` rely solely on Database constraints (`nullable=false`). Adding JSR-303 annotations to Entities is a good defensive practice.
*   **Magic Strings:** Security expressions like `@PreAuthorize("@ss.isAccountOwner...")` rely on the bean name `ss`. Refactoring this to a constant or stricter naming convention would prevent regression if the bean is renamed.

## 3. Testing

### Strengths
*   **Unit Coverage:** High coverage for Services and Controllers using `Mockito` and `@WebMvcTest`.
*   **Mocking:** Proper isolation of dependencies (Security, Repositories).

### Risks
*   **Integration Gap:** Testcontainers was removed to resolve local environment issues. This means strictly relying on Unit Tests. We have no verification that the custom JPQL queries in `TransactionRepository` (e.g., `getNetFlowAfterDate`) actually execute correctly against a real Postgres instance.

## 4. Maintainability

### Strengths
*   **Java 21 Features:** Good use of `var` and Streams.
*   **Project Structure:** Clear package organization by feature/layer.

---
**Verdict:** The code is **Clean and Testable**. The immediate priority is enabling the Scheduler to activate the Snapshot feature and then setting up MapStruct to reduce boilerplate.