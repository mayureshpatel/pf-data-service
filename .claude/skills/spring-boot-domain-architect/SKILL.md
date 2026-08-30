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
