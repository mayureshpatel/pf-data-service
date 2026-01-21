# Codebase Review & Quality Assessment

**Date:** January 21, 2026
**Reviewer:** Senior Software Engineer
**Scope:** Full Codebase Audit

## 1. Executive Summary
The application follows a clean Spring Boot layered architecture. However, there are **critical functional defects** related to how `TransactionType.TRANSFER` is handled, leading to data corruption (incorrect balances) and incorrect reporting (Net Worth).

Performance is generally acceptable for small datasets, but specific areas (Bulk Delete) exhibit **N+1 query patterns**. Testing coverage is low for complex logic (Reporting, Specifications).

## 2. Critical Findings (Bugs & Risks)

### A. The "Transfer" Logic Contradiction (High Severity)
The application handles `TransactionType.TRANSFER` inconsistently, leading to mathematical errors.
1.  **Service Layer (`TransactionService`):**
    *   **Logic:** `if (type == EXPENSE) subtract else add`.
    *   **Result:** `TRANSFER` is treated as **INCOME**.
    *   **Scenario:** A user records a transfer *out* of an account (e.g., to Savings). They select `TRANSFER`. The system **adds** the amount to the account balance instead of subtracting it.
2.  **Repository/Reporting Layer (`TransactionRepository`):**
    *   **Logic:** `CASE WHEN type = 'INCOME' THEN amount ELSE -amount END`.
    *   **Result:** `TRANSFER` is treated as **EXPENSE** (negative flow).
    *   **Scenario:** A user records a transfer *in* (e.g., from Savings). Reporting subtracts it from Net Worth.

**Root Cause:** `TransactionType` mixes *intent* (Transfer vs Expense) with *direction* (Credit vs Debit).
**Recommendation:** Add a `TransactionDirection` (INFLOW, OUTFLOW) or strictly map `TRANSFER` to specific directions.

### B. Security Vulnerabilities
*   **Hardcoded Secret:** The JWT secret key is hardcoded in `application.yml`.
    *   *Risk:* If the code is public, anyone can forge tokens.
    *   *Fix:* Move to environment variables (`${JWT_SECRET}`).

### C. Performance Issues
*   **N+1 in Bulk Delete:** `TransactionService.deleteTransactions` iterates through IDs and calls `t.getAccount().getUser()` for ownership checks. This triggers individual queries if not eagerly fetched.
*   **Bulk Update Inefficiency:** `updateTransactions` iterates and calls `updateTransaction` (singular), which re-fetches the entity (`findById`) for every single item.

## 3. Code Quality & Maintainability

### Strengths
*   **Architecture:** Clear separation of concerns.
*   **Dynamic Filtering:** `TransactionSpecification` is a robust solution for complex search criteria.
*   **Security:** Ownership checks are enforced on data access (good usage of `@PreAuthorize` and manual checks).
*   **Database:** Use of Flyway for migrations is excellent.

### Weaknesses (Technical Debt)
*   **Logic Duplication:** The logic to update account balances (add/subtract based on type) is repeated 3 times in `TransactionService` (Create, Update, Delete). It should be encapsulated.
*   **Missing Tests:**
    *   `DashboardService.getNetWorthHistory` is completely untested.
    *   `TransactionSpecification` is untested.
    *   No integration tests to verify SQL queries.

## 4. Detailed Recommendations

### Immediate Actions
1.  **Refactor Transaction Model:**
    *   Option A: Remove `TRANSFER` from `TransactionType` and use `INCOME`/`EXPENSE` only. Use Categories to denote "Transfers".
    *   Option B (Preferred): Add a `direction` field or split `TRANSFER` into `TRANSFER_IN` and `TRANSFER_OUT`.
2.  **Fix Balance Logic:** Centralize the "apply transaction to balance" logic in a single method that handles the direction correctly.
3.  **Externalize Secrets:** Remove the JWT key from `application.yml`.

### Secondary Actions
4.  **Optimize Bulk Ops:** Use a single JPQL query to check ownership for a list of IDs: `boolean allOwned = repository.countOwnedByUser(ids, userId) == ids.size()`.
5.  **Add Test Coverage:**
    *   Unit test for `DashboardService` math.
    *   Integration test for `TransactionRepository` custom queries.

---
**Verdict:** **DO NOT RELEASE** until the Transfer logic is fixed. The current state corrupts user balance data.