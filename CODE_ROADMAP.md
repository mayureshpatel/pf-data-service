# Technical Roadmap: Deployment & Operational Excellence

This roadmap marks the final steps to move the service from a stable local development state to a production-ready cloud deployment.

## Phase 1: Build & Infrastructure (High Priority)
- [ ] **1. Resilient CI/CD Integration**
    - [ ] Create a `docker-it` Maven profile.
    - [ ] Tag all Testcontainers-dependent tests with `@Tag("integration")`.
    - [ ] Configure `maven-surefire-plugin` to exclude the `integration` tag by default, allowing a simple `mvn test` to pass anywhere.
- [ ] **2. Cloud Configuration**
    - [ ] Externalize sensitive keys (JWT Secret, DB Password) using `${VAR}` placeholders.
    - [ ] Create a `prod` profile in `application-prod.yml`.

## Phase 2: Security & Developer Experience
- [ ] **3. Swagger Authorization**
    - [ ] (Completed) Integrated JWT Bearer Auth into OpenAPI UI.
- [ ] **4. Account Registration**
    - [ ] Implement a secure `/register` endpoint with password strength validation.

## Phase 3: Architectural Polish
- [ ] **5. Automated Mapping**
    - [ ] Replace manual `mapToDto` and `mapToEntity` methods with **MapStruct** mappers.
- [ ] **6. Domain Invariants**
    - [ ] Add `@Column(nullable=false)` and length constraints to JPA entities to mirror database schema strictly.

## Phase 4: Observability & Health
- [ ] **7. Advanced Monitoring**
    - [ ] Configure Micrometer to export metrics to Prometheus/Grafana.
    - [ ] Add custom Actuator `HealthIndicator` for external bank API connectivity (if added later).

## Phase 5: Containerization
- [ ] **8. Multi-Stage Dockerfile**
    - [ ] Implement a multi-stage `Dockerfile` to build the JAR and then run it in a slimmed-down Alpine JRE image.
- [ ] **9. Docker Compose**
    - [ ] Provide a `docker-compose.yml` for "One-Click" local environment setup.

---
**Status:** MVP Architectural refactoring is **Complete**. The project is now in the **Hardening** stage.
