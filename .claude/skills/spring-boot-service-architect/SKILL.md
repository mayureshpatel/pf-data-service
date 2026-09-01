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
2. **Orchestration**: Call multiple repositories if necessary (e.g., fetching a Category before saving a Transaction).
3. **Events**: Publish Spring `@DomainEvents` if side-effects are required (e.g., notifying the SnapshotService when a Transaction changes).

## 📚 References
- [Service Gold-Source](references/service-gold-source.java)
