---
name: spring-boot-controller-architect
description: Generates RESTful Spring Boot Controllers. Trigger this skill when designing API endpoints to enforce strict REST semantics, kebab-case routing, and OpenAPI documentation requirements.
---

# Spring Boot Controller Architect

This skill governs the creation of HTTP presentation layers (Controllers).

## 🚨 Architectural Constraints
- **Naming**: All API endpoints MUST use plural `kebab-case` nouns (e.g., `/api/v1/bank-accounts`).
- **Validation**: Use `jakarta.validation` annotations (e.g., `@Valid`, `@NotNull`) on all incoming DTOs.
- **OpenAPI**: Every endpoint MUST be documented using `@Operation` and `@ApiResponses`.
- **DTOs Only**: Controllers must ONLY accept and return DTOs. Never leak Domain objects (`Account`, `Transaction`) to the client.

## 🛠 Procedural Workflow
1. **Routing**: Define the `@RestController` and `@RequestMapping`.
2. **Injection**: Use `Lombok` `@RequiredArgsConstructor` for constructor injection of Services.
3. **Mapping**: Map incoming DTOs to Domain objects via manual Mapper classes.
4. **Delegation**: Pass Domain objects to the Service layer.
5. **Response**: Wrap the Service layer output in a `ResponseEntity` using the correct HTTP status code (`201 CREATED`, `200 OK`).

## 🛡️ Security
Ensure endpoints are protected via `@PreAuthorize` where tenant isolation is required (e.g., `@PreAuthorize("@securityService.isAccountOwner(#accountId, principal)")`).

## 📚 References
- [Controller Gold-Source](references/controller-gold-source.java)

## ⚠️ Gotchas
- **One real controller already violates the plural-noun rule: `CurrencyController` maps
  `/api/v1/currency` (singular).** Confirmed by direct check against all 13 controllers' real
  `@RequestMapping` values — every other one is plural. Don't copy this one as precedent for a new
  endpoint; it's a live, uncorrected exception, not an accepted alternate style. Renaming it is a
  breaking API change (existing callers depend on the current path), so it's a deliberate fix for
  someone to schedule, not something to silently correct as a side effect of an unrelated task.
