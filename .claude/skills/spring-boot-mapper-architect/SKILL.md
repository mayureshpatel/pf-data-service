---
name: spring-boot-mapper-architect
description: Generates static Mapper utility classes to convert between Domain Entities and DTOs. Use this skill when asked to create Mappers in the backend.
---
# Spring Boot Mapper Architect

This skill governs the creation of Backend Mappers.

## 🚨 Constraints & Guardrails
- **No Auto-Mappers:** You MUST NOT use MapStruct or ModelMapper. All mapping logic must be explicitly handwritten.
- **Utility Class:** The mapper MUST be a `final class` with a `private` constructor.
- **Methods:** Methods should be `public static`.

## 🛠 Procedural Workflow
1. Identify the Source and Target classes (e.g., `Account` and `AccountDto`).
2. Write a static `toDto` (or `toDomain`) method handling null checks meticulously.
3. Review `references/AccountDtoMapper.java` for exact formatting expectations.
