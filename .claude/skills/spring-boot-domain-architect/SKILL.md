---
name: spring-boot-domain-architect
description: Generates or refactors Domain entities using strict immutability and Lombok Builder patterns. Use this skill when asked to create a new Domain model for the backend.
---
# Spring Boot Domain Architect

This skill governs the creation of Backend Domain objects.

## 🚨 Constraints & Guardrails
- **No ORM:** You MUST NOT use `@Entity`, `@Table`, or any JPA annotations.
- **Immutability:** Fields MUST be `final`.
- **Lombok:** Use `@Builder(toBuilder = true)` and `@Getter`.
- **Logic:** Domain models should contain rich business logic (e.g., `applyTransaction`) rather than being anemic data bags.

## 🛠 Procedural Workflow
1. Read the provided schema or requirements.
2. Define the POJO using Lombok annotations.
3. Review `references/Account.java` for exact formatting expectations.

## ⚠️ Gotchas
- **Literal `final` fields are the exception, not the norm, in the real domain layer today.**
  `Account.java` (this skill's own gold-source basis) is the one class that actually achieves it.
  Of the other 18 real domain classes, 4 are enums (immaterial) and 14 use plain, non-`final`
  fields. That's less alarming than it sounds: 13 of those 14 pair `@Getter` with
  `@Builder(toBuilder = true)`/`@SuperBuilder(toBuilder = true)` and *no* `@Setter` — so there's no
  generated way to mutate a field after construction even though the compiler doesn't enforce it.
  Treat `final` as the correct target for anything new (matching `Account.java`), but don't be
  surprised the rest of the domain layer hasn't been retrofitted — that's a real, pre-existing gap,
  not something to silently mass-fix as a side effect of an unrelated task.
- **`Iconography.java` is a genuine, complete outlier — don't copy it.** It uses `@Setter` +
  `@NoArgsConstructor`/`@AllArgsConstructor` with no `@Builder` at all, meaning it's actually,
  fully mutable via generated setters — not just non-`final` in letter like the 13 above. If asked
  to extend or reference it, that's worth a second look, not a pattern to propagate.
