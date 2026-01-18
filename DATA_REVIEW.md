# Senior Data Engineer & Data Analyst Review

**Date:** January 18, 2026
**Project:** Personal Finance Data Service (Backend MVP)
**Database:** PostgreSQL (Flyway Managed)

## 1. Schema Design & Data Integrity

### Strengths
- **Precise Financial Types:** Use of `NUMERIC(19, 2)` for all monetary values (`amount`, `current_balance`) correctly avoids floating-point errors.
- **Foreign Key Constraints:** Strong use of FKs with appropriate `ON DELETE` behaviors (mostly `CASCADE`, with some `RESTRICT` safety in V3).
- **Normalization:** The schema is well-normalized (3NF), with clear separations for Users, Accounts, Transactions, Categories, and Tags.
- **Check Constraints:** Implementation of `chk_transaction_type` and `chk_account_type` ensures data quality at the database level.

### Areas for Improvement
- **Global Category Rules:** The `category_rules` table is currently global. From a data analyst's perspective, this is a major limitation as "STARBUCKS" might be "Dining Out" for one user but "Business Expense" for another.
- **Missing Audit Consistency:** While `accounts` and `transactions` have `created_at`/`updated_at`, `categories` and `tags` do not. Standardizing audit columns across all tables is essential for troubleshooting and data lineage.
- **Soft Deletes:** There is no mechanism for soft deletes. If a user accidentally deletes an account, all historical transaction data is lost immediately.
- **Uniqueness in Transactions:** While `file_import_history` prevents double-importing the same file, the `transactions` table itself doesn't have a natural key or unique constraint to prevent manual duplicate entries or cross-file duplicates (e.g., the same transaction appearing in two different CSV exports).

## 2. Performance & Indexing

### Strengths
- **Strategic Indexing:** V7 introduced critical composite indexes like `idx_transactions_account_date`, which will significantly speed up dashboard aggregations.
- **Hash-based Lookup:** `idx_import_account_hash` efficiently handles duplicate file detection.

### Areas for Improvement
- **Missing Category Indices:** `transactions(category_id)` is a very common join/filter path for reporting but currently lacks an explicit index.
- **Large Table Partitioning:** For a high-volume user, the `transactions` table will grow indefinitely.
    - *Recommendation:* Prepare for **Table Partitioning** by `date` (e.g., yearly partitions) to maintain performance and simplify archival.
- **Full-Text Search:** The `description` field in `transactions` is frequently searched. Standard B-tree indexes are inefficient for partial string matches (`LIKE %...%`).
    - *Recommendation:* Consider a `GIN` index with `pg_trgm` for faster description searching.

## 3. Analytical Usability

### Strengths
- **Sub-category Support:** Self-referencing `categories` allows for hierarchical reporting (e.g., "Food" -> "Groceries").
- **Many-to-Many Tagging:** Allows for multi-dimensional analysis that transcends simple category hierarchies.

### Areas for Improvement
- **Historical Balance Tracking:** Currently, to find a user's net worth on a specific date, one must sum all transactions from the beginning of time. This is O(N) and expensive.
    - *Recommendation:* Implement a **Daily/Monthly Account Snapshots** table to store end-of-period balances.
- **Data Dictionary:** There is no documentation of what specific fields mean (e.g., what does `priority` in `category_rules` specifically influence in the algorithm).

## 4. Security & Compliance

### Strengths
- **Ownership Isolation:** Every major table (Accounts, Categories, Tags) is tied to a `user_id`, allowing for strong row-level isolation logic in the application.

### Areas for Improvement
- **PII Protection:** Email addresses and usernames are stored in plaintext. While typical for an MVP, an analyst shouldn't necessarily see these in cleartext in an analytical replica.
    - *Recommendation:* Use Database Views or Column-Level Encryption for PII if the team expands.

---
**Verdict:** The database foundation is technically sound and adheres to financial data best practices. The primary risks are long-term scalability (lack of snapshots/partitioning) and the inflexibility of global categorization rules.
