# Data Engineering & Analytics Roadmap

This roadmap outlines the evolution of the `pf-data-service` data layer from an MVP to a robust, scalable, and analytically rich system.

## Phase 1: Data Integrity & Auditing (High Priority)
- [ ] **1. Standardize Audit Columns**
    - [ ] Add `created_at` and `updated_at` to `categories`, `tags`, and `category_rules`.
    - [ ] Add `created_by` to track automated vs. manual changes.
- [ ] **2. Personalize Categorization**
    - [ ] Add `user_id` (nullable) to `category_rules`.
    - [ ] Update logic to prioritize user-specific rules over global defaults.
- [ ] **3. Constraint Hardening**
    - [ ] Add `unique` constraint on `transactions(account_id, date, amount, description, type)` to provide a "last line of defense" against duplicates.

## Phase 2: Performance & Scalability
- [ ] **4. Reporting Indices**
    - [ ] Create index on `transactions(category_id)`.
    - [ ] Create index on `transactions(type)`.
    - [ ] Enable `pg_trgm` extension and add GIN index to `transactions(description)`.
- [ ] **5. Balance Snapshots**
    - [ ] Design and implement a `monthly_account_snapshots` table.
    - [ ] Create a trigger or scheduled job to calculate and store end-of-month balances.

## Phase 3: Analytical Depth
- [ ] **6. Soft Deletes (Archival Strategy)**
    - [ ] Add `deleted_at` column to `accounts` and `transactions`.
    - [ ] Update application logic to filter for `NULL` deleted_at.
- [ ] **7. Advanced Aggregates**
    - [ ] Create Database Views for common analytical queries (e.g., `v_monthly_spending_by_category`).
    - [ ] Implement a `transaction_import_summary` table to track the "health" of imports over time.

## Phase 4: Data Lifecycle Management
- [ ] **8. Table Partitioning**
    - [ ] Migrate `transactions` to a partitioned table by `date` (Yearly/Monthly).
- [ ] **9. Data Anonymization**
    - [ ] Implement data masking or views to strip PII for analytical use cases.

---
**Status:** Database architectural review **Complete**. Ready for Phase 1 implementation.
