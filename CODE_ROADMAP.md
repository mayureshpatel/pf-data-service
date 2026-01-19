# Technical Roadmap: Hardening & Polish

This roadmap focuses on operationalizing the dormant features and reducing boilerplate.

## Phase 1: Operational Activation (Immediate)
- [ ] **1. Enable Scheduling**
    - [ ] Add `@EnableScheduling` to `PfDataServiceApplication`.
    - [ ] Create a `SnapshotScheduler` component to trigger `SnapshotService.createEndOfMonthSnapshot` periodically (e.g., monthly cron).

## Phase 2: Boilerplate Reduction
- [ ] **2. Adopt MapStruct**
    - [ ] Add `mapstruct` and `mapstruct-processor` dependencies.
    - [ ] Create `TransactionMapper` interface.
    - [ ] Refactor `TransactionService` to use the mapper instead of `mapToDto`.

## Phase 3: Deployment Readiness
- [ ] **3. Containerization**
    - [ ] Create a multi-stage `Dockerfile`.
    - [ ] Create `docker-compose.yml` for running the App + Postgres locally (restoring the integration capability for those who have Docker).
- [ ] **4. API Documentation**
    - [ ] Verify `springdoc-openapi` is correctly scanning controllers.
    - [ ] Add `@Operation` and `@ApiResponse` annotations to Controllers for better Swagger UI documentation.

## Phase 4: Integration Testing (Future)
- [ ] **5. Restore Integration Tests**
    - [ ] Re-introduce Testcontainers under a specific Maven profile (`-Pintegration`) so they don't block builds on machines without Docker.

---
**Status:** Codebase is healthy. Testing is robust (Unit). Priority is activating the Scheduler.