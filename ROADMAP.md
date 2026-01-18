# Development Roadmap: Refactoring & Improvements

This roadmap outlines the steps to address the findings from the Code Review. We will tackle these in phases, starting with critical bugs and cleanup, followed by architectural improvements.

## Phase 1: Critical Fixes & Cleanup
*Goal: Fix immediate bugs and remove dead/legacy code.*

- [x] **1. Fix `StandardCsvParser`**
    - [x] Add missing `@Component` annotation to register it with `TransactionParserFactory`.
    - [x] Implement dynamic `TransactionType` logic (Expense vs. Income) based on the amount (currently hardcoded to `EXPENSE`).
    - [x] Ensure date parsing is consistent with the rest of the application.
- [x] **2. Consolidate Import Logic**
    - [x] Remove the legacy `importCsv` method from `TransactionService`.
    - [x] Ensure all import functionality flows through `TransactionImportService`.

## Phase 2: Robustness & Error Handling
*Goal: Improve application stability and user feedback.*

- [x] **3. Implement Custom Exceptions**
    - [x] Create specific exception classes (e.g., `CsvParsingException`, `DuplicateTransactionException`).
    - [x] Refactor `TransactionImportService` to throw these exceptions instead of generic RuntimeExceptions.
    - [x] Update `GlobalExceptionHandler` to map these exceptions to proper HTTP status codes and user-friendly messages.

## Phase 3: Enhanced Categorization (Feature)
*Goal: Make categorization dynamic and configurable without code changes.*

- [x] **4. Database-Driven Categorization Rules**
    - [x] Create a Flyway migration (`V6__create_category_rules.sql`) to add a `category_rules` table.
    - [x] Create a JPA Entity `CategoryRule` (fields: `keyword`, `category`, `priority`).
    - [x] Create `CategoryRuleRepository`.
    - [x] Refactor `TransactionCategorizer` to fetch rules from the database (cache them if necessary) instead of using the hardcoded `KEYWORD_RULES` map.

## Phase 4: Testing Coverage
*Goal: Ensure reliability across edge cases.*

- [x] **5. Expand Test Coverage**
    - [x] Add unit tests for the fixed `StandardCsvParser`.
    - [x] Add integration tests for edge cases (malformed CSVs, empty files) in `TransactionControllerTest` or `TransactionImportServiceTest`.

## Phase 5: Architecture & Data Safety (Round 2 Findings)
*Goal: Decouple API from DB and fix Hibernate/Lombok pitfalls.*

- [x] **6. DTO Separation**
    - [x] Create `TransactionDto` (or `TransactionInput`) with validation annotations.
    - [x] Update `SaveTransactionRequest` to use `List<TransactionDto>`.
    - [x] Update `TransactionImportService` and `TransactionController` to map DTOs to Entities.
- [x] **7. JPA Entity Best Practices**
    - [x] Refactor `Transaction` (and other entities) to replace `@Data` with `@Getter` + `@Setter`.
    - [x] Implement robust `equals()` and `hashCode()` using only the ID.
    - [x] Exclude lazy-loaded fields from `@ToString`.
- [x] **8. Concurrency Safety**
    - [x] Mark `cachedRules` in `TransactionCategorizer` as `volatile`.

## Phase 6: Security (Critical)
*Goal: Secure user data and endpoints.*

- [x] **9. Externalize Configuration**
    - [x] Move CORS origins to `application.yml` and inject them via `@Value`.
- [x] **10. Implement Spring Security**
    - [x] Add `spring-boot-starter-security`.
    - [x] Configure `SecurityFilterChain` to require authentication for `/api/**` (Basic Auth enabled).
    - [x] Implement `CustomUserDetailsService` to load users from DB.
    - [x] Remove `userId` parameters from Controllers; extract them from the authenticated Principal to fix IDOR.
    - [x] Update Tests to use `@WithCustomMockUser`.

---

## Phase 7: Final Polish & Performance (Round 3 Findings)
*Goal: Fix remaining security gaps, improve performance, and enable frontend integration.*

- [x] **11. Fix Dashboard Security**
    - [x] Refactor `DashboardController` to remove `userId` param and use `@AuthenticationPrincipal`.
- [x] **12. Database Performance**
    - [x] Create Flyway V7 migration to add indices on `transactions(account_id, date)` and `categories(user_id)`.
- [x] **13. Developer Experience**
    - [x] Add `springdoc-openapi-starter-webmvc-ui` dependency for Swagger.
    - [x] Add `spring-boot-starter-actuator` for health checks.
    - [x] Update SecurityConfig to permit access to Swagger/Actuator.
- [x] **14. Dashboard Testing**
    - [x] Add unit tests for `TransactionService.getDashboardData` to ensure aggregation logic is correct.

---
**Status:** All tasks across all phases are now complete. The project is secure, performant, and production-ready (for an MVP).