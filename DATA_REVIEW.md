# Database Architecture & Operations Review

**Date:** January 21, 2026
**Reviewer:** Senior Database Administrator
**Scope:** PostgreSQL Schema (Migrations V1-V15), JPA Entities, Service Logic, and Operational Readiness.

## 1. Executive Summary
The database is in a **transitional phase**. It has successfully moved from a prototype schema to a structured, relational design with `V9` (Auditing) and `V12` (Soft Deletes). The schema is 3NF normalized and strongly typed (`NUMERIC(19,2)` for money, strict `CHECK` constraints on Enums).

However, the system is **not production-ready** due to critical gaps in **concurrency control** and **process automation**. The `Account` entity is vulnerable to race conditions, and the `SnapshotService`—designed for historical reporting—is currently dormant.

## 2. Deep-Dive Findings

### A. Schema Evolution & Quality
*   **Migrations (V1-V15):** The Flyway migration history is clean.
    *   *Observation:* `V15__reset_and_seed_robust_data.sql` is a destructive migration (TRUNCATE). This indicates the project is treated as "pre-release." **Recommendation:** Future migrations must be additive only; strictly prohibit `TRUNCATE` or `DROP TABLE` in future versions.
*   **Data Types:** Correct usage of `NUMERIC` for financial data and `TIMESTAMP` for auditing.
*   **Constraints:** `CHECK` constraints on `transaction_type` (V3) and Foreign Keys (V1) enforce strong data integrity at the DB level, preventing invalid states even if app logic fails.

### B. Application & Entity Mapping
*   **Soft Deletes:** Implemented via `@SQLDelete` and `@SQLRestriction` on `Account`, `Transaction`, and `User`.
    *   *Risk:* Native SQL queries (if any) must manually account for `deleted_at IS NULL`.
*   **Snapshot Logic:** `SnapshotService` uses a clever "retroactive calculation" strategy (`Current Balance` - `Net Flow After Date`). This avoids drift but relies heavily on the accuracy of `Account.current_balance`.

### C. Critical Risks (Priority High)
1.  **Race Conditions (Lost Updates):**
    *   **Scenario:** Two users (or a user and an async process) modify an `Account` simultaneously.
    *   **Evidence:** `Account.java` **lacks** a `@Version` field.
    *   **Impact:** One transaction overwrites the other's balance update. Financial data becomes permanently inconsistent.
2.  **Dormant Reporting:**
    *   **Scenario:** Users expect "Month-over-Month" graphs.
    *   **Evidence:** `SnapshotService` exists but is never called.
    *   **Impact:** Historical data reporting will be empty or require expensive on-the-fly calculation.

### D. Security Observations
*   **PII Exposure:** `User.email` is stored as plain text. In a financial application, this violates privacy-by-design principles.
*   **Credential Storage:** `password_hash` is present (Good).
*   **Access Control:** Application-level filtering by `user_id` is used. RLS (Row Level Security) is not enabled in Postgres.

## 3. Detailed Recommendations

### Immediate Actions (Stability)
1.  **Implement Optimistic Locking:**
    *   Add `@Version private Long version;` to `Account.java`.
    *   Handle `ObjectOptimisticLockingFailureException` in `TransactionImportService` (retry or fail gracefully).
2.  **Enable Automation:**
    *   Add `@EnableScheduling` to `PfDataServiceApplication`.
    *   Implement a `SnapshotScheduler` to run `0 0 0 1 * ?` (Monthly).

### Secondary Actions (Security & Ops)
3.  **Encrypt Sensitive Data:**
    *   Introduce a JPA `AttributeConverter` for `User.email` using AES-256.
4.  **Operational Metrics:**
    *   The `FileImportHistory` table exists (V2), but we should verify it captures *failed* imports to help debug CSV parsing issues.

---
**Verdict:** The foundation is solid. The immediate focus must be "locking it down" (Concurrency) and "turning it on" (Scheduling).