# Code Review: Personal Finance Data Service (Round 3)

## Executive Summary
The backend has matured significantly with the addition of Spring Security, DTOs, and improved JPA practices. The application is now "Secure by Design" regarding IDOR and Mass Assignment.

However, three key areas still require attention:
1.  **Incomplete Security Rollout:** The `DashboardController` was missed during the security refactoring and still exposes an IDOR vulnerability.
2.  **Database Performance:** The dashboard relies on multiple aggregate queries over what will likely be the largest table (`transactions`). Missing indices will cause performance degradation.
3.  **Observability & Documentation:** The project lacks API documentation (OpenAPI/Swagger) and health checks (Actuator), making it hard to integrate with a frontend or monitor in production.

---

## 1. Security (Critical Remnants)

### Dashboard IDOR Vulnerability
- **Issue:** `DashboardController` still accepts `userId` as a request parameter: `@RequestParam(defaultValue = "1") Long userId`.
- **Risk:** Any authenticated user can view another user's dashboard by changing this parameter.
- **Recommendation:** Refactor `DashboardController` to use `@AuthenticationPrincipal CustomUserDetails userDetails` and pass `userDetails.getId()` to the service, exactly as implemented in `TransactionController`.

---

## 2. Database & Performance

### Missing Indices
- **Issue:** The dashboard performs heavy aggregation (SUM, GROUP BY) filtering by `account.user.id`, `date`, and `type`.
- **Current Schema:** Foreign keys exist, but there are no composite indices optimized for these specific queries.
- **Risk:** As transaction volume grows, dashboard load times will increase linearly (Full Table Scans).
- **Recommendation:** Add a Flyway migration (`V7__add_performance_indices.sql`) to create indices on:
    - `transactions(account_id, date)`: For range queries.
    - `categories(user_id)`: For lookups.
    - `accounts(user_id)`: For ownership checks.

---

## 3. Developer Experience & Observability

### Missing API Documentation
- **Issue:** There is no automatic documentation for the new `TransactionDto` or the endpoints. Frontend developers will have to guess the JSON structure.
- **Recommendation:** Add `springdoc-openapi-starter-webmvc-ui`.

### Missing Health Checks
- **Issue:** No way to verify if the DB is connected or the app is healthy without hitting a business endpoint.
- **Recommendation:** Add `spring-boot-starter-actuator`.

---

## 4. Testing

### Gap in Service Layer Testing
- **Issue:** `TransactionService.getDashboardData` is not fully covered by unit tests mocking the repository responses.
- **Recommendation:** Add `TransactionServiceTest` to verify that `DashboardData` is constructed correctly from the repository's partial results (handling nulls, zero values).

---

## 5. Summary of New Recommendations

1.  **Fix Dashboard Security:** Update `DashboardController` to use `CustomUserDetails`.
2.  **Optimize DB:** Add indices for dashboard queries.
3.  **Add OpenAPI:** Install Swagger UI.
4.  **Add Actuator:** Enable health endpoints.
5.  **Expand Tests:** Unit test the Dashboard logic.
