# Backend (`pf-data-service`) Instructions

## Framework & Architecture
- **Framework:** Modern Spring Boot (3.5.x).
- **Layers:** Strictly follow the Controller -> Service -> Repository layer pattern.
- **Data Access:** Use **Spring JDBC Client** exclusively in the Repository layer for data access control.
- **Data Mapping:** Manually map objects; do **NOT** use mapper libraries (e.g., MapStruct). Use Lombok restrictively.

## Coding Standards & Documentation
- **Formatting:** Standard Java 4-space indentation. No formatter plugin (Spotless, Checkstyle,
  etc.) is configured in `pom.xml` — this states the existing convention, not a new enforcement
  tool.
- **API Design:** REST API endpoints must use **kebab-case plural nouns** (e.g., `/api/v1/bank-accounts`).
- **Database Schema:** All tables and columns must use **snake_case**.
- **Imports:** Always optimize imports and remove unused ones.
- **Javadoc:** Provide complete Javadoc at both the **class** and **method** levels explaining the "what" and "why".
- **In-Code Comments:** All in-code comments must be entirely in **lowercase**.

## Error Handling & Logging
- **Logging Strategy:** 
	- `ERROR`: Use **ONLY** at the `@ExceptionHandler` level to prevent duplicate logs. Include full stack trace.
	- `INFO/DEBUG/TRACE/WARN`: Use appropriately for context without breaking the flow.

## Testing (Backend)
- **Tools:** JUnit5, Mockito, Testcontainers, AssertJ.
- **Structure:** Follow the AAA pattern with exact lowercase comments: `// arrange`, `// act`, `// assert & verify`.
- **Quality:** Use **PiTest** for mutation testing to validate test quality. Adhere to Test-Driven Development (TDD) for bugs.
