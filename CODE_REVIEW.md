# Code Review: Personal Finance Data Service (Round 4)

## Executive Summary
The `pf-data-service` has reached a stable and secure MVP state. The architecture is sound, leveraging Spring Boot best practices, robust security, and efficient database interactions.

However, to transition from a "Backend MVP" to a fully functional "Application Backend," we need to address specific functional gaps (CRUD), enforce stricter transactional boundaries, and improve resource handling for scale.

---

## 1. Functional Gaps (The Missing Features)

### Missing CRUD Operations
- **Issue:** The current API allows *importing* (bulk create) via CSV and JSON, but offers no way to:
    - List transactions (paginated).
    - Edit a specific transaction (e.g., correct a category).
    - Delete a transaction.
- **Impact:** The frontend will be read-only (Dashboard) and write-only (Import), with no management capabilities.
- **Recommendation:** Implement `GET /api/v1/transactions` (paginated), `PUT /api/v1/transactions/{id}`, and `DELETE /api/v1/transactions/{id}`.

### API Versioning Consistency
- **Issue:** `TransactionController` is mapped to `/api/v1/...`, but `DashboardController` is mapped to `/api/dashboard` (no version).
- **Impact:** Inconsistent API surface. Harder to manage future breaking changes.
- **Recommendation:** Move `DashboardController` to `/api/v1/dashboard`.

---

## 2. Resource Management & Performance

### Large File Handling
- **Issue:** `TransactionController` reads the entire `MultipartFile` into a `byte[]` (`file.getBytes()`) before passing it to the service.
- **Risk:** High memory pressure. If 10 users upload 10MB files simultaneously, that's 100MB+ of heap churn instantly.
- **Recommendation:** Refactor `TransactionImportService` to accept an `InputStream` instead of `byte[]`. Stream the file processing.

### Transactional Boundaries
- **Issue:** `TransactionService.getDashboardData` performs multiple repository calls (`getSumByDateRange`, `findCategoryTotals`). It is **missing** the `@Transactional(readOnly = true)` annotation.
- **Risk:**
    - Each repository call might acquire/release a database connection separately (connection pool thrashing).
    - No read-consistency guarantee between the three queries (e.g., if a transaction is inserted mid-request).
- **Recommendation:** Add `@Transactional(readOnly = true)` to `TransactionService` class or method.

---

## 3. Observability & Logging

### Missing Request Logging
- **Issue:** The file structure scan reveals `LoggingAspect` exists, but the `RequestLoggingFilter` (which was seen in earlier file lists) seems to be missing from the `src/main/.../filter` directory in the latest scan, or simply not registered.
- **Recommendation:** Ensure a standard Request/Response logging filter is present and registered to trace incoming API calls in production.

---

## 4. Testing

### Controller Testing & Security
- **Strength:** The use of `@WithCustomMockUser` is excellent.
- **Gap:** We verify `status().isOk()`, but we should also verify that the `transactionService` is called with the *correct* `userId` extracted from the principal.
- **Recommendation:** Add `verify(transactionService).getDashboardData(eq(10L), ...)` to `DashboardControllerTest`.

---

## 5. Summary of New Recommendations

1.  **Consistency:** Move `DashboardController` to `/api/v1/dashboard`.
2.  **Performance:** Add `@Transactional(readOnly = true)` to `TransactionService`.
3.  **Scalability:** Refactor file upload to use `InputStream` instead of `byte[]`.
4.  **Feature:** Implement Paginated List, Edit, and Delete endpoints for Transactions.
5.  **Quality:** Restore/Implement `RequestLoggingFilter`.