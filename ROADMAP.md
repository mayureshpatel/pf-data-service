# Technical Roadmap: Production Hardening & Scalability

This roadmap outlines the final transition from a high-quality MVP to a production-ready, scalable service.

## Phase 1: Operational Stability (Critical)
- [ ] **1. Environment-Agnostic Build**
    - [ ] Create a `docker-it` Maven profile for Testcontainers.
    - [ ] Configure the default `mvn test` to exclude tests tagged with `@Tag("integration")`.
- [ ] **2. Secrets & Configuration**
    - [ ] Externalize JWT secret and Database credentials using Environment Variables.
    - [ ] Implement a `dev` profile with local defaults and a `prod` profile for deployment.

## Phase 2: Security & DX (Developer Experience)
- [ ] **3. Swagger JWT Integration**
    - [ ] Configure `OpenApiCustomizer` to add Bearer Authentication to the Swagger UI.
- [ ] **4. Declarative Authorization**
    - [ ] Implement custom SpEL expressions (e.g., `@IsAccountOwner`) to replace manual ownership checks in the service layer.

## Phase 3: Performance & Scalability
- [ ] **5. Streaming CSV Processing**
    - [ ] Refactor `TransactionParser` to return `Stream<Transaction>`.
    - [ ] Update `TransactionImportService` to process the stream, maintaining low memory usage regardless of file size.
- [ ] **6. Audit Logging**
    - [ ] Enhance `RequestLoggingFilter` to include User IDs and correlation IDs for better production troubleshooting.

## Phase 4: Deployment
- [ ] **7. Containerization**
    - [ ] Optimized `Dockerfile` using JRE 21 alpine images.
    - [ ] `docker-compose.yml` for local production-like testing.

---
**Status:** Architectural Polish (Mapping layer) is **Complete**. The project is now moving into the "Hardening" stage.