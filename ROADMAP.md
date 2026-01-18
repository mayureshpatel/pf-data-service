# Technical Roadmap: Refactoring & Modernization

This roadmap focuses on technical excellence, stability, and preparing the codebase for scale, based on the Senior Code Review.

## Phase 1: Stability & Testing Infrastructure
*Goal: Ensure the build passes reliably in all environments.*

- [ ] **1. Test Suite Resilience**
    - [ ] Create a specific Maven profile for Integration Tests (requiring Docker).
    - [ ] Ensure `mvn test` (default) runs fast Unit Tests (Mockito) without requiring Testcontainers.
    - [ ] Add `@Tag("integration")` to container-dependent tests.

## Phase 2: Java 21 Modernization & Clean Code
*Goal: Leverage modern Java features for more concise and readable code.*

- [ ] **2. Adopt Java Records**
    - [ ] Refactor immutable DTOs (`TransactionDto`, `DashboardData`, `CategoryTotal`) from Lombok `@Data/@Value` classes to Java `record` types.
- [ ] **3. Type Safety Refactoring**
    - [ ] Create `BankName` Enum.
    - [ ] Refactor Parsers and the Factory to use the Enum instead of raw Strings.

## Phase 3: Architecture & Performance
*Goal: Improve separation of concerns and data throughput.*

- [ ] **4. Service Decomposition**
    - [ ] Extract Dashboard/Analytics logic from `TransactionService` into `DashboardService`.
    - [ ] Ensure `TransactionService` focuses strictly on Transaction CRUD and Import orchestration.
- [ ] **5. Database Optimization**
    - [ ] Enable Hibernate Batch Inserts in `application.yml`.
    - [ ] Verify `saveAll` behavior during CSV import to ensure bulk inserts are actually occurring.

## Phase 4: API Standardization
*Goal: Align with industry standards for error handling.*

- [ ] **6. RFC 7807 Error Handling**
    - [ ] Refactor `GlobalExceptionHandler` to return `ProblemDetail` objects instead of custom error wrappers where applicable.

## Phase 5: Security Hardening (Post-MVP)
- [ ] **7. JWT Authentication**
    - [ ] Migrate from Basic Auth to stateless JWT Authentication to better support the frontend.