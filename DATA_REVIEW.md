# Senior Data Engineer & Data Analyst Review

**Date:** January 19, 2026
**Project:** Personal Finance Data Service (Backend MVP)
**Database:** PostgreSQL (Flyway Managed)
**Application:** Spring Boot 3 / Hibernate 6

## 1. Schema vs. Code Consistency (Critical)

### Findings
- **CRITICAL: Missing Sub-category Mapping:** The database schema (`V4__sub_category_update.sql`) adds a `parent_id` foreign key to the `categories` table. However, the `Category.java` entity **does not map this field**.
    - *Impact:* The sub-category feature is defined in the database but completely inaccessible to the application logic. The backend cannot read or write category hierarchies.
- **Association Mapping:** `FileImportHistory` stores `accountId` as a raw `Long` rather than a `@ManyToOne` relationship to the `Account` entity.
    - *Impact:* While functional, this bypasses Hibernate's referential integrity checks within the persistence context and prevents easy traversal (e.g., `history.getAccount().getUser()`).

## 2. Schema Design & Data Integrity

### Strengths
- **Precise Financial Types:** Use of `NUMERIC(19, 2)` for `amount` and `current_balance` prevents floating-point arithmetic errors.
- **Constraint Enforcement:** Excellent use of database-level constraints (`chk_transaction_type`, `chk_account_type`) to enforce enum values at the storage layer.
- **Normalization:** The schema is effectively normalized (3NF), reducing data redundancy.
- **Duplicate Prevention:** `file_import_history` uses a file hash to prevent re-importing the same CSV.

### Areas for Improvement
- **Global Category Rules:** `category_rules` is a global table without a `user_id`.
    - *Impact:* Rules are shared across all users. If User A defines "Amazon" as "Groceries" and User B defines it as "Shopping", they will conflict. This breaks multi-tenancy.
- **Audit Inconsistency:**
    - `users`, `accounts`, `transactions` have audit timestamps (`created_at`/`updated_at`).
    - `categories`, `tags`, `category_rules` **lack** audit timestamps.
    - *Recommendation:* Apply a standard `Auditable` base class or interface to all entities.
- **Soft Deletes:** No soft delete mechanism exists. Deleting an account permanently destroys all associated transaction history (`CASCADE` delete).
- **Hard Deletes on Cascade:** `accounts` -> `transactions` cascade delete is dangerous for financial data. V3 changed this to `RESTRICT` (good), but other relationships like `users` -> `accounts` remain `CASCADE`.

## 3. Performance & Indexing

### Strengths
- **Dashboard Optimization:** `idx_transactions_account_date` (V7) is perfectly targeted for the most common query: "Show me transactions for this account in this date range."
- **Ownership Lookups:** Indexes on `user_id` for `accounts` and `categories` ensure security checks are fast.

### Areas for Improvement
- **Category Filtering:** There is no index on `transactions(category_id)`. Filtering spending by category (e.g., "How much did I spend on Food?") will trigger full table scans or inefficient filtering.
- **Description Search:** Searching transactions by description relies on `LIKE %...%`, which cannot use standard B-Tree indexes efficiently.
    - *Recommendation:* Implement `pg_trgm` (trigram) indexes for performant text search.
- **Partitioning:** The `transactions` table is a prime candidate for list/range partitioning (by User or Date) as the dataset grows.

## 4. Security & Compliance

### Strengths
- **Row-Level Isolation:** All primary data entities (`Account`, `Category`, `Tag`, `Transaction`) are directly or indirectly linked to a `User`, enabling effective application-level tenancy checks.

### Areas for Improvement
- **PII Storage:** Emails are stored in plaintext in the `users` table.
- **Sensitive Data in Logs:** Ensure `RequestLoggingFilter` does not log request bodies containing passwords or PII (needs code verification, but flagged here as a standard check).

---
**Verdict:** The database foundation is solid for an MVP but has a **critical code-schema disconnect** regarding sub-categories. Immediate remediation is required to map the `parent` category in the Java entity. Following that, fixing the multi-tenancy issue in `category_rules` is the next highest priority.