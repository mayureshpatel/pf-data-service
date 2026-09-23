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
- **Quality:** PiTest (`pom.xml`) is bound to the Maven `verify` phase (since PF-323), so a full
  `./mvnw clean verify` — what `scripts/verify.sh` runs — triggers a whole-codebase mutation run
  automatically; `mvn test` and narrower `-Dtest=SingleClass` invocations never reach `verify`, so
  they stay fast and unaffected. It's deliberately informational only: no `mutationThreshold`/
  `coverageThreshold` is configured, so a low or failing mutation score reports but never fails the
  build on its own — don't cite a passing `verify.sh` run as proof of a mutation-testing bar, since
  it isn't gating on one. For a fast, scoped check against a single class while writing tests, skip
  the full lifecycle-bound run and invoke the goal directly: `mvn org.pitest:pitest-maven:
  mutationCoverage -DtargetClasses=<FQCN> -DtargetTests=<FQCN>*`, then read
  `target/pit-reports/**/mutations.csv` directly rather than trusting the summary percentage.
  Adhere to Test-Driven Development (TDD) for bugs.
