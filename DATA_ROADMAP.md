# Data Engineering & Analytics Roadmap

This roadmap outlines the evolution of the `pf-data-service` data layer, prioritizing critical fixes and scalability.

## Phase 1: Critical Fixes (Immediate)
- [x] **1. Fix Code-Schema Discrepancy**
    - [x] Update `Category.java` to map the `parent_id` column (Self-referencing `@ManyToOne`).
    - [x] Update `CategoryDto` (if exists) or API models to expose hierarchy.
- [x] **2. Fix Association Mapping**
    - [x] Refactor `FileImportHistory` to use `@ManyToOne` for `Account` instead of raw ID.

## Phase 2: Data Integrity & Multi-Tenancy
- [x] **3. Personalize Categorization**
    - [x] Add `user_id` column to `category_rules`.
    - [x] Migrate existing rules to system defaults or specific users.
    - [x] Update `TransactionCategorizer` logic to respect user scopes.
- [x] **4. Standardize Auditing**
    - [x] Add `created_at` and `updated_at` to `categories`, `tags`, and `category_rules` (DB migration).
    - [x] Update Java entities with `@CreationTimestamp` / `@UpdateTimestamp`.

## Phase 3: Performance & Scalability
- [x] **5. Reporting Indices**
    - [x] Create index on `transactions(category_id)`.
    - [x] Enable `pg_trgm` extension and add GIN index to `transactions(description)`.
- [x] **6. Balance Snapshots**
    - [x] Design `monthly_account_snapshots` table.
    - [x] Implement end-of-month balance calculation job (Service implemented).

## Phase 4: Advanced Features
- [x] **7. Soft Deletes**
    - [x] Add `deleted_at` column to critical tables.
    - [x] Implement Hibernate `@SQLRestriction("deleted_at IS NULL")` pattern.
- [ ] **8. Data Archival**
    - [ ] Implement table partitioning for `transactions` (Deferred: Requires PK refactoring, overkill for MVP).

---
**Status:** Updated Jan 19, 2026. Phases 1-3 Complete. Phase 4 (Soft Deletes) Complete. Partitioning deferred.