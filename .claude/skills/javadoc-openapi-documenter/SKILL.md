---
name: javadoc-openapi-documenter
description: Specialized skill for backfilling in-code documentation. Trigger this to ensure strict lowercase inline-comments, method-level Javadoc, and Swagger/OpenAPI annotations.
---

# Javadoc & OpenAPI Documenter

This skill enforces the strict documentation formatting rules across the backend.

## 🚨 Architectural Constraints
- **Lowercase Rule**: ALL inline `//` comments MUST be entirely lowercase. (e.g., `// check if user exists`). No capital letters allowed.
- **Javadoc**: All Controllers and Services MUST have a class-level `/** ... */` block explaining the architectural purpose.
- **Methods**: Public methods must document `@param`, `@return`, and `@throws`.
- **OpenAPI**: Use `@Tag` on Controller classes and `@Operation` / `@ApiResponse` on HTTP methods.

## 🛠 Procedural Workflow
1. **Scan**: Identify undocumented public methods or uppercase inline comments.
2. **Refactor**: Rewrite `//` comments to lowercase.
3. **Annotate**: Inject Swagger and Javadoc blocks.

## 📚 References
See `references/documented-service-gold-source.java` — a real service class (`AccountService`)
with class-level and method-level Javadoc (`@param`/`@return`/`@throws`) and lowercase inline
comments throughout.
