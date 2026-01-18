# Development Roadmap: Refactoring & Improvements

This roadmap outlines the steps to address the findings from the Code Review. We will tackle these in phases, starting with critical bugs and cleanup, followed by architectural improvements.

## Phase 1-7: Completed
*(See previous history for detailed completed tasks including Security, Parsing, and Database Optimization)*

---

## Phase 8: Consistency & Core Features (Round 4 Findings)
*Goal: Polish the API surface and implement missing management features.*

- [ ] **15. API Consistency & Transaction Management**
    - [ ] Rename `DashboardController` path to `/api/v1/dashboard`.
    - [ ] Add `@Transactional(readOnly = true)` to `TransactionService`.
- [ ] **16. Implement CRUD Endpoints**
    - [ ] Add `GET /api/v1/transactions` with pagination and filtering (date range, type).
    - [ ] Add `PUT /api/v1/transactions/{id}` for updating details.
    - [ ] Add `DELETE /api/v1/transactions/{id}`.
    - [ ] Ensure all endpoints enforce `userId` ownership checks.

## Phase 9: Performance & Resilience
*Goal: Optimize resource usage for scale.*

- [ ] **17. Optimize File Upload**
    - [ ] Refactor `TransactionImportService` to consume `InputStream`.
    - [ ] Update `TransactionController` to pass `file.getInputStream()`.
- [ ] **18. Observability**
    - [ ] Re-implement/Register `RequestLoggingFilter` for auditing API traffic.

## Phase 10: Containerization & Deployment
*Goal: Prepare for "Production".*

- [ ] **19. Docker Support**
    - [ ] Create `Dockerfile` (optimized layered build).
    - [ ] Create `docker-compose.yml` (App + Postgres).
