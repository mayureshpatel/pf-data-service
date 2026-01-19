# Data Engineering & Analytics Roadmap

This roadmap outlines the evolution of the `pf-data-service` data layer, prioritizing critical fixes and scalability.

## Phase 1: Critical Fixes (Immediate)
- [ ] **1. Fix Code-Schema Discrepancy**
    - [ ] Update `Category.java` to map the `parent_id` column (Self-referencing `@ManyToOne`).
    - [ ] Update `CategoryDto` (if exists) or API models to expose hierarchy.
- [ ] **2. Fix Association Mapping**
    - [ ] Refactor `FileImportHistory` to use `@ManyToOne` for `Account` instead of raw ID.

## Phase 2: Data Integrity & Multi-Tenancy
- [ ] **3. Personalize Categorization**
    - [ ] Add `user_id` column to `category_rules`.
    - [ ] Migrate existing rules to system defaults or specific users.
    - [ ] Update `TransactionCategorizer` logic to respect user scopes.
- [ ] **4. Standardize Auditing**
    - [ ] Add `created_at` and `updated_at` to `categories`, `tags`, and `category_rules` (DB migration).
    - [ ] Update Java entities with `@CreationTimestamp` / `@UpdateTimestamp`.

## Phase 3: Performance & Scalability
- [ ] **5. Reporting Indices**
    - [ ] Create index on `transactions(category_id)`.
    - [ ] Enable `pg_trgm` extension and add GIN index to `transactions(description)`.
- [ ] **6. Balance Snapshots**
    - [ ] Design `monthly_account_snapshots` table.
    - [ ] Implement end-of-month balance calculation job.

## Phase 4: Advanced Features
- [ ] **7. Soft Deletes**
    - [ ] Add `deleted_at` column to critical tables.
    - [ ] Implement Hibernate `@Where(clause = "deleted_at IS NULL")` or similar pattern.
- [ ] **8. Data Archival**
    - [ ] Implement table partitioning for `transactions` (by Year).

---
**Status:** Updated Jan 19, 2026 following comprehensive code & schema review.