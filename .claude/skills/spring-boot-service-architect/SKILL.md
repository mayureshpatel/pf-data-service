---
name: spring-boot-service-architect
description: Generates Spring Boot Service layers. Trigger this skill when implementing core business logic to ensure proper transactional boundaries, exception handling, and domain orchestration.
---

# Spring Boot Service Architect

This skill dictates how to write robust, side-effect-free business logic in Spring Boot.

## 🚨 Architectural Constraints
- **Transactions**: Use `@Transactional` exclusively at the Service level, NEVER at the Controller or Repository level. Use `readOnly = true` for fetch operations.
- **Exceptions**: Throw custom domain exceptions (e.g., `ResourceNotFoundException`, `AccessDeniedException`) rather than generic `RuntimeException`.
- **Immutability**: Treat Domain objects as immutable. Return new instances or use Builders when modifying state.

## 🛠 Procedural Workflow
1. **Validation**: Perform business rule validation (e.g., "Cannot delete an account with a non-zero balance").
2. **Orchestration**: Call multiple repositories or other Services directly via constructor
   injection if necessary (e.g., fetching a Category before saving a Transaction). This codebase
   does not use a Spring Application Events / `@DomainEvents` pattern anywhere — cross-cutting
   side effects are wired as direct calls, not published events. Don't invent an event-bus
   mechanism that isn't actually configured.

## 🚨 Gotchas
- There is no domain-events or pub-sub mechanism in this codebase — `grep -rn "@DomainEvents"
  src/main/java` returns zero hits. If a Service needs to trigger a side effect elsewhere (e.g. an
  account balance change that should update a snapshot), call the collaborating Service/Repository
  directly, the same way `TransactionImportService` directly injects every collaborator it needs
  rather than publishing an event.

## 📚 References
- [Service Gold-Source](references/service-gold-source.java)
