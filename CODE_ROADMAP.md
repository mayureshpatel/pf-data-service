# Development Roadmap: pf-data-service

**Last Updated:** January 21, 2026
**Status:** MVP Development
**Target User:** Single User (Self-hosted)

This roadmap prioritizes fixes and features based on the code review findings. Items are organized into phases with clear dependencies.

---

## Phase 0: Critical Security Fixes (BLOCKING)

> **Must complete before any deployment or sharing the codebase.**

### 0.1 Remove Authentication Bypass Backdoor
- [ ] Remove mock-dev-token handling from `JwtAuthenticationFilter.java`
- [ ] Or: Gate behind `@Profile("dev")` + property flag
- [ ] Add test to verify mock token is rejected in production

**Files:** `security/JwtAuthenticationFilter.java`

### 0.2 Externalize JWT Secret
- [ ] Remove default secret from `application.yml`
- [ ] Remove default from `JwtService.java` `@Value` annotation
- [ ] Add startup validation: fail if `JWT_SECRET` env var not set
- [ ] Document required environment variables

**Files:** `application.yml`, `security/JwtService.java`

### 0.3 Fix TagRepository Interface
- [ ] Add `extends JpaRepository<Tag, Long>` to TagRepository
- [ ] Verify application starts without errors
- [ ] Add basic test for tag operations

**Files:** `repository/TagRepository.java`

**Estimated Effort:** 1-2 hours

---

## Phase 1: Data Integrity & Stability

> **Complete before importing real financial data.**

### 1.1 Add Optimistic Locking to Account
- [ ] Add `@Version private Long version;` to Account entity
- [ ] Create migration: `V18__add_account_version.sql`
- [ ] Update tests to handle OptimisticLockException scenarios
- [ ] Document retry strategy for concurrent operations

**Files:** `model/Account.java`, new migration

### 1.2 Add Unique Constraints
- [ ] Create migration for unique constraints:
  ```sql
  ALTER TABLE categories ADD CONSTRAINT uq_category_user_name UNIQUE (user_id, name);
  ALTER TABLE tags ADD CONSTRAINT uq_tag_user_name UNIQUE (user_id, name);
  ```
- [ ] Update services to handle duplicate constraint violations gracefully
- [ ] Add meaningful error messages for duplicates

**Files:** New migration, exception handling

### 1.3 Consistent Exception Handling
- [ ] Replace `RuntimeException("Access denied")` with `AccessDeniedException`
  - `AccountService.java:49`
  - `AccountService.java:69`
  - `CategoryService.java:48`
- [ ] Verify GlobalExceptionHandler returns 403 for all cases

**Files:** `service/AccountService.java`, `service/CategoryService.java`

### 1.4 Transaction DTO Validation
- [ ] Add `@Positive` to `amount` field
- [ ] Add `@NotNull` to `accountId` for create operations (or use separate CreateTransactionRequest DTO)
- [ ] Add validation tests

**Files:** `dto/TransactionDto.java`

**Estimated Effort:** 3-4 hours

---

## Phase 2: Code Quality & Maintainability

> **Reduce technical debt for easier future development.**

### 2.1 Extract Abstract CSV Parser
- [ ] Create `AbstractCsvParser` base class with:
  - `parseAmount(String value)`
  - `createParsedStream(CSVParser, Function<CSVRecord, Optional<Transaction>>)`
  - Resource cleanup handling
- [ ] Refactor all 4 parsers to extend base class
- [ ] Ensure all parser tests still pass

**Files:** New `parser/AbstractCsvParser.java`, all parser classes

### 2.2 Standardize API Versioning
- [ ] Remove unversioned paths from `AccountController` and `CategoryController`
- [ ] Keep only `/api/v1/` paths
- [ ] Update any client code/documentation

**Files:** `controller/AccountController.java`, `controller/CategoryController.java`

### 2.3 Optimize Category Lookup
- [ ] Add `findByUserIdAndNameIgnoreCase(Long userId, String name)` to CategoryRepository
- [ ] Update `TransactionService.createTransaction()` and `updateTransactionFromDto()`
- [ ] Add test for case-insensitive lookup

**Files:** `repository/CategoryRepository.java`, `service/TransactionService.java`

### 2.4 Fix deleteTransactions N+1
- [ ] Change `findAllById(transactionIds)` to `findAllByIdWithAccountAndUser(transactionIds)`
- [ ] Add test verifying single query execution

**Files:** `service/TransactionService.java`

**Estimated Effort:** 4-6 hours

---

## Phase 3: Testing & Quality Assurance

> **Build confidence in the codebase.**

### 3.1 Add Missing Unit Tests
- [ ] `TransactionSpecification` - test all filter combinations
- [ ] `SecurityService.isAccountOwner()` and `isTransactionOwner()`
- [ ] `DashboardService.getNetWorthHistory()` - verify calculation logic

### 3.2 Add Integration Tests
- [ ] `@DataJpaTest` for `TransactionRepository` custom queries:
  - `getSumByDateRange()`
  - `findCategoryTotals()`
  - `getDailyNetFlows()`
- [ ] `@SpringBootTest` for end-to-end import flow
- [ ] Security integration tests (verify @PreAuthorize works)

### 3.3 Add Test Data Fixtures
- [ ] Create reusable test fixtures for User, Account, Transaction
- [ ] Document test data setup patterns

**Estimated Effort:** 6-8 hours

---

## Phase 4: Feature Enhancements (MVP+)

> **Improve user experience and data management.**

### 4.1 Soft Delete for Remaining Entities
- [ ] Add soft delete to Category, Tag, CategoryRule, VendorRule
- [ ] Create migrations for `deleted_at` columns
- [ ] Update repositories with `@SQLRestriction`
- [ ] Handle cascade: what happens to transactions when category is soft-deleted?

### 4.2 Category Rule → Category Relationship
- [ ] Add `@ManyToOne Category category` to CategoryRule
- [ ] Create migration to add `category_id` column
- [ ] Migrate existing data: lookup categories by name
- [ ] Update categorization logic

### 4.3 Scheduled Snapshots
- [ ] Enable `SnapshotService` scheduling
- [ ] Add `@Scheduled` annotation for end-of-day/month snapshots
- [ ] Create endpoint to view historical snapshots
- [ ] Use snapshots for faster net worth history calculation

### 4.4 User Registration
- [ ] Add registration endpoint
- [ ] Password strength validation
- [ ] Email verification (optional for single-user)
- [ ] Initial category/rule seeding for new users

### 4.5 MapStruct Integration
- [ ] Add MapStruct dependency
- [ ] Create mapper interfaces for all entity/DTO pairs
- [ ] Replace manual `mapToDto()` methods
- [ ] Add mapping tests

**Estimated Effort:** 12-16 hours

---

## Phase 5: Advanced Features (Future)

> **Enhancements for power users and analytics.**

### 5.1 Budgeting
- [ ] Budget entity (user, category, amount, period)
- [ ] Budget vs actual comparison endpoint
- [ ] Alerts when approaching/exceeding budget

### 5.2 Recurring Transactions
- [ ] RecurringTransaction entity
- [ ] Scheduler to auto-create transactions
- [ ] Support for various frequencies (weekly, monthly, yearly)

### 5.3 Advanced Reporting
- [ ] Year-over-year comparison
- [ ] Category trend analysis
- [ ] Savings rate calculation
- [ ] Export to CSV/PDF

### 5.4 Multi-Currency Support
- [ ] Currency field on Account
- [ ] Exchange rate integration
- [ ] Consolidated reporting in base currency

### 5.5 Account Reconciliation
- [ ] Mark transactions as reconciled
- [ ] Reconciliation workflow
- [ ] Discrepancy detection

### 5.6 Tags Enhancement
- [ ] Tag CRUD endpoints (currently missing)
- [ ] Tag-based filtering in dashboard
- [ ] Tag analytics

---

## Technical Debt Backlog

Items that improve code quality but aren't blocking:

| Item | Priority | Effort |
|------|----------|--------|
| Add Javadoc to public service methods | Low | 2h |
| Implement request/response logging interceptor | Low | 1h |
| Add health check endpoint details | Low | 1h |
| Database connection pool monitoring | Low | 2h |
| API rate limiting | Low | 3h |
| Request validation error messages improvement | Low | 2h |
| OpenAPI documentation enrichment | Low | 3h |

---

## Migration Checklist

When deploying to a new environment:

1. [ ] Set `JWT_SECRET` environment variable (min 256 bits)
2. [ ] Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
3. [ ] Set `CORS_ALLOWED_ORIGINS` for frontend URL
4. [ ] Run Flyway migrations
5. [ ] Create initial admin user
6. [ ] Seed default categories and rules (if desired)
7. [ ] Verify all endpoints require authentication
8. [ ] Test CSV import with sample file

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 0.1.0 | Jan 2026 | Initial MVP |

---

## Dependencies & Blockers

```
Phase 0 ──► Phase 1 ──► Phase 2 ──┬──► Phase 4
                                  │
                                  └──► Phase 3

Phase 4 ──► Phase 5
```

- Phase 0 blocks everything (security critical)
- Phase 1 blocks importing real data
- Phase 2 and 3 can run in parallel
- Phase 4 requires Phase 1 complete
- Phase 5 requires Phase 4 complete
