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

---

## Phase 5: Architecture & Data Safety (Round 2 Findings)
*Goal: Decouple API from DB and fix Hibernate/Lombok pitfalls.*

- [ ] **6. DTO Separation**
    - [ ] Create `TransactionDto` (or `TransactionInput`) with validation annotations.
    - [ ] Update `SaveTransactionRequest` to use `List<TransactionDto>`.
    - [ ] Update `TransactionImportService` and `TransactionController` to map DTOs to Entities.
- [ ] **7. JPA Entity Best Practices**
    - [ ] Refactor `Transaction` (and other entities) to replace `@Data` with `@Getter` + `@Setter`.
    - [ ] Implement robust `equals()` and `hashCode()` using only the ID.
    - [ ] Exclude lazy-loaded fields from `@ToString`.
- [ ] **8. Concurrency Safety**
    - [ ] Mark `cachedRules` in `TransactionCategorizer` as `volatile`.

## Phase 6: Security (Critical)
*Goal: Secure user data and endpoints.*

- [ ] **9. Externalize Configuration**
    - [ ] Move CORS origins to `application.yml` and inject them via `@Value`.
- [ ] **10. Implement Spring Security**
    - [ ] Add `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server` (or simple JWT lib).
    - [ ] Configure `SecurityFilterChain` to require authentication for `/api/**`.
    - [ ] Remove `userId` and `accountId` parameters from Controllers; extract them from the authenticated Principal.
    - [ ] Update Tests to use `@WithMockUser`.
