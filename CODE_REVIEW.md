# Senior Software Engineer Code Review
**Date:** January 18, 2026
**Project:** Personal Finance Data Service (Backend MVP)
**Version:** Spring Boot 3.5.3 | Java 21

## 1. Architecture & Design Patterns

### Strengths
*   **Layered Architecture:** Clear separation of concerns (Controller -> Service -> Repository) is maintained.
*   **Strategy Pattern:** The use of `TransactionParser` interface with specific implementations (`CapitalOneCsvParser`, etc.) is an excellent use of the Strategy pattern, making the system extensible for new bank formats.
*   **Database Migration:** Usage of **Flyway** ensures robust schema versioning and reproducibility across environments.

### Areas for Improvement
*   **Single Responsibility Principle (SRP):** `TransactionService` appears to be accumulating multiple responsibilities: CRUD operations, business rules validation, and complex dashboard analytics/aggregation.
    *   *Recommendation:* Extract dashboard/analytics logic into a dedicated `DashboardService` or `AnalyticsService`.
*   **Type Safety:** Bank names are currently hardcoded string literals (e.g., "CAPITAL_ONE", "DISCOVER").
    *   *Recommendation:* Introduce a `BankName` Enum to enforce type safety and centralize the definition of supported banks.
*   **DTO Modernization:** The project uses standard Java Classes with Lombok for DTOs.
    *   *Recommendation:* Since the project runs on Java 21, migrate immutable DTOs (Data Transfer Objects) to **Java Records**. This reduces boilerplate and improves semantics for immutable data carriers.

## 2. Testing Strategy & Quality

### Strengths
*   **Integration Testing:** The use of `Testcontainers` is the industry gold standard for ensuring tests run against a real PostgreSQL instance rather than an H2 simulation.

### Critical Issues
*   **Test Fragility:** The build currently fails if a Docker environment is not present (`TransactionRepositoryTest`). This blocks development in non-Dockerized environments.
    *   *Recommendation:* Implement Maven profiles to separate "Unit Tests" (fast, no Docker) from "Integration Tests" (slow, requires Docker). Alternatively, configure tests to degrade gracefully or be skipped if the Docker engine is unavailable.
*   **Unit Test Isolation:** Ensure Service layer tests mock the Repository layer strictly to allow logic verification without spinning up the Spring Context.

## 3. Data Persistence & Performance

### Strengths
*   **JPA/Hibernate:** Good usage of standard JPA patterns.

### Areas for Improvement
*   **Batch Processing:** The CSV import feature likely saves transactions one by one or in a loop.
    *   *Recommendation:* Verify and enable Hibernate Batch Inserts (`spring.jpa.properties.hibernate.jdbc.batch_size`) to significantly improve performance when importing large CSV files (e.g., 1000+ rows).
*   **N+1 Selects:** Ensure that fetching Transactions does not inadvertently trigger N+1 queries when fetching associated entities (like Categories or Tags). Use `EntityGraph` or `JOIN FETCH` where appropriate.

## 4. Error Handling & API Standards

### Strengths
*   **Global Handling:** `GlobalExceptionHandler` is present and intercepts standard exceptions.

### Areas for Improvement
*   **Standardization:** Ensure the API returns consistent error structures.
    *   *Recommendation:* Adopt **RFC 7807 (Problem Details for HTTP APIs)**. Spring Boot 3+ provides native support for `ProblemDetail` which standardizes error responses (type, title, status, detail, instance).

## 5. Security

### Strengths
*   **Security Config:** Basic Auth is correctly configured for the MVP.

### Areas for Improvement
*   **State Management:** Currently relies on Session/Basic Auth.
    *   *Recommendation:* For a modern specific frontend (React/Mobile), plan a migration to **Stateless JWT (JSON Web Tokens)** or OAuth2 Resource Server. This decouples the backend state from the frontend client and improves scalability.

---
**Verdict:** The project is solid for an MVP. The codebase is clean and uses modern tools. The immediate priority should be stabilizing the test suite for non-Docker environments and refactoring DTOs to Java Records to leverage the full power of Java 21.