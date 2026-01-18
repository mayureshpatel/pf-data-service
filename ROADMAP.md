# Technical Roadmap: Deployment Readiness

This roadmap focuses on operational excellence and preparing the application for a secure production release, now that the core architectural refactoring is complete.

## Phase 1: Operational Stability (High Priority)
*Goal: Ensure the build pipeline is robust and environment-agnostic.*

- [ ] **1. Resilient Test Infrastructure**
    - [ ] Configure Maven Profiles: Create a `docker` profile that activates Testcontainers.
    - [ ] Configure `mvn test` (default) to skip container-dependent tests if Docker is unavailable, ensuring the build succeeds based on Unit Tests alone.

## Phase 2: Security & Configuration Hardening
*Goal: Secure sensitive data and prepare for cloud deployment.*

- [ ] **2. Secrets Management**
    - [ ] Remove the hardcoded JWT Secret from `application.yml`.
    - [ ] Replace it with an Environment Variable reference (`${JWT_SECRET_KEY}`).
    - [ ] Update documentation to instruct developers how to set this key locally.
- [ ] **3. CORS Configuration**
    - [ ] Verify CORS settings for the frontend integration (React/Angular). Currently allows `localhost:4200`, ensure this is configurable via environment variables for production domains.

## Phase 3: Architectural Polish
*Goal: Consistency across layers.*

- [ ] **4. Mapping Layer Refactoring**
    - [ ] Refactor `TransactionImportService` to accept `TransactionDto` instead of `Transaction` entities.
    - [ ] Move the DTO -> Entity conversion logic from `TransactionController` into the Service layer.

## Phase 4: Containerization (Deployment)
*Goal: Ship the application.*

- [ ] **5. Docker Support**
    - [ ] Create a multi-stage `Dockerfile` (Build -> Run) to minimize image size.
    - [ ] Create a `docker-compose.yml` that spins up the Application and a PostgreSQL database container with the correct networking and environment variables.

---
**Status:** Phases 2-5 of the previous roadmap are **Complete**. The focus now shifts to Operations and Consistency.
