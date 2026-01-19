# Expert Database Review

**Date:** January 19, 2026
**Reviewer:** Senior Database Administrator
**Scope:** PostgreSQL Schema, JPA Entities, Configuration, and Scalability

## 1. Executive Summary
The database architecture has matured significantly. The recent implementation of **Soft Deletes**, **Standardized Auditing**, **Multi-Tenant Categorization**, and **Reporting Indices** has transformed the system from a basic MVP into a robust, enterprise-grade foundation. The schema is normalized (3NF), type-safe, and optimized for the most frequent read patterns (Dashboards).

However, **Operational Automation** and **Concurrency Control** are the newly identified bottlenecks. The system calculates snapshots but never schedules them, and it lacks protection against concurrent updates to account balances.

## 2. Detailed Findings

### A. Schema & Integrity (Strong)
*   **Type Safety:** Excellent use of `NUMERIC(19,2)` ensures financial precision.
*   **Constraints:** Check constraints on `TransactionType` and `AccountType` effectively prevent bad data entry.
*   **Auditing:** The standardization of `created_at`/`updated_at` across all tables, coupled with `deleted_at` for soft deletes, provides a complete audit trail.
*   **Referential Integrity:** `Category` hierarchy and `FileImportHistory` relationships are now correctly mapped in both DB and Java.

### B. Performance (Good, with caveats)
*   **Indexing:** The new `GIN` index on `description` and composite indexes on `transactions` will ensure sub-millisecond query performance for standard users.
*   **Connection Pooling:** HikariCP is configured (`max-pool-size: 10`). This is adequate for small loads but will become a bottleneck under concurrency.
*   **Missing Automation:** The `AccountSnapshot` infrastructure exists, but the **Scheduler is missing**. The `SnapshotService` is currently dead code unless triggered manually.

### C. Concurrency & Locking (Critical Risk)
*   **Missing Optimistic Locking:** The `Account` entity represents a shared resource. If two file imports (or an import and a manual adjustment) happen simultaneously, the `current_balance` could be overwritten, resulting in a **Lost Update** anomaly.
    *   *Recommendation:* Immediate addition of `@Version` column to `Account`.

### D. Security (Moderate)
*   **PII Exposure:** User emails are stored in plaintext. While `password_hash` is secure, PII leaks are a liability.
*   **Row-Level Security:** The application uses explicit `user_id` filters. This is acceptable for now, but migrating to PostgreSQL Row-Level Security (RLS) could be considered for defense-in-depth in the future.

## 3. Recommendations

1.  **Concurrency Control:** Add `@Version` to the `Account` entity immediately to prevent balance corruption.
2.  **Activate Automation:** Enable Spring Scheduling and create a Cron job to execute `SnapshotService` on the 1st of every month.
3.  **Security Hardening:** Implement an `AttributeConverter` to encrypt sensitive fields (Email) at rest.
4.  **Operational Config:** Tune HikariCP validation timeout and leak detection.

---
**Verdict:** The database is **Structurally Sound** but **Operationally Incomplete**. Focus must shift from "Schema Design" to "Runtime Behavior" (Locking, Scheduling, Security).
