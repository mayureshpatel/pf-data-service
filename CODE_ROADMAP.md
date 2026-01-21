# Technical Roadmap: Critical Fixes & Stability

## Phase 1: Critical Bug Fixes (Data Integrity)
*Goal: Ensure account balances and reporting are mathematically correct.*

- [ ] **1. Refactor Transaction Direction Logic**
    - [ ] **Design Decision:** How to handle Transfers? (Split into `TRANSFER_IN`/`OUT` or explicit `amount` sign?)
    - [ ] **Database Migration:** Update existing `TRANSFER` records to map to new types if necessary.
    - [ ] **Service Layer:** Refactor `create`/`update`/`delete` to use a centralized `calculateBalanceChange(transaction)` method.
    - [ ] **Repository Layer:** Update Net Worth SQL queries to respect the new Direction/Type logic.

- [ ] **2. Security Hardening**
    - [ ] Move JWT Secret to environment variable.

## Phase 2: Performance & Validation
*Goal: Ensure system scales for bulk operations.*

- [ ] **3. Optimize Bulk Operations**
    - [ ] Implement `TransactionRepository.countByIdsAndUserId(ids, userId)` for efficient O(1) ownership check.
    - [ ] Refactor `deleteTransactions` to use the optimized check.

- [ ] **4. Test Coverage (Safety Net)**
    - [ ] **Unit Tests:** Add tests for `DashboardService.getNetWorthHistory`.
    - [ ] **Integration Tests:** Add `@DataJpaTest` for `TransactionRepository` to verify SQL logic for Net Worth.

## Phase 3: Refactoring & Cleanup
- [ ] **5. DRY Principle**
    - [ ] Extract Balance Update logic into a Helper or Domain method on the Account entity.
- [ ] **6. MapStruct**
    - [ ] Implement MapStruct for DTO<->Entity mapping.

---
**Priority:** Phase 1 is blocking usage. Phase 2 is required for code quality.