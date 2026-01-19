# Backend Integration Context: Personal Finance App

This document summarizes the backend changes implemented in the Spring Boot 3 / Java 21 service to support the modern Angular frontend integration.

## 1. API Changes & New Endpoints

### Path Consistency
- All controllers now support both `/api/v1/` and `/api/` prefixes to ensure seamless integration while maintaining versioning standards.

### Account Management
- **Endpoints**: `GET /api/accounts` and `POST /api/accounts`
- **DTO**: `AccountDto { id, name, type, currentBalance }`
- **Behavior**: Lists all active accounts for the authenticated user. New accounts are automatically linked to the logged-in principal.

### Category Management
- **Endpoints**: `GET /api/categories` and `POST /api/categories`
- **DTO**: `CategoryDto { id, name }`
- **Features**: Supports a hierarchical structure (Parent/Sub-category) used by the Transaction Categorizer.

### Transaction Management
- **Pagination**: `GET /api/v1/transactions` now strictly follows Spring Data `Pageable`.
    - **Request Params**: `page`, `size`, `sort`.
    - **Response**: `Page<TransactionDto>` (standard Spring wrapper).
- **Manual Entry**: `POST /api/v1/transactions` for single entry.
- **Bulk Save**: `POST /api/v1/accounts/{accountId}/transactions` (supports manual entry lists and file imports).
- **CRUD Operations**:
    - `PUT /api/v1/transactions/{id}`: Fully implemented.
    - `DELETE /api/v1/transactions/{id}`: Fully implemented.
- **Side Effects**: All mutations (Create/Update/Delete) automatically update the `currentBalance` of the associated `Account`.

### Dashboard Aggregation
- **Endpoint**: `GET /api/v1/dashboard`
- **Query Params**: `month` (1-12), `year` (YYYY).
- **Response**: `DashboardData { totalIncome, totalExpense, netSavings, List<CategoryTotal> categoryBreakdown }`.

## 2. DTO & Data Contract Updates

### TransactionDto
Enhanced for UI state management:
- `id`: Unique identifier (Long).
- `accountId`: The ID of the parent account (Long).
- `type`: Enum `INCOME` or `EXPENSE`.
- `categoryName`: Display name of the category.

### Validation
- **Nested Validation**: `SaveTransactionRequest` now uses `@Valid` on the transaction list.
- **Constraints**: Missing `type`, `date`, or `amount` in the JSON body will trigger a `400 Bad Request` with field-level error messages.

## 3. Security & Development Support

### Dev-Only "Mock Auth" (Auth Bypass)
- **Token**: `Bearer mock-dev-token`
- **Behavior**: Automatically authenticates as the `admin` user (ID 1) without checking credentials.
- **Frontend Sync**: Designed to work with `environment.useMockAuth = true`.

### Security Expressions
- **Bean Name**: The security service is aliased as `@ss`.
- **Logic**: Endpoints are protected by `@ss.isAccountOwner(#accountId, principal)` and `@ss.isTransactionOwner(#id, principal)` to ensure data isolation between users.

## 4. Error Handling
The backend uses standard `ProblemDetail` (RFC 7807) responses:
- **400 Bad Request**: Malformed IDs (e.g., `/transactions/undefined`) or validation failures.
- **403 Forbidden**: Ownership mismatch or unauthorized access.
- **404 Not Found**: Endpoint mismatch or entity not found.

## 5. Seed Data & Database State
The database has been pre-populated with a robust 6-month financial simulation:
- **Default User**: `admin` / `password` (ID: 1).
- **Main Account**: `Main Checking` (ID: 1).
- **Timeframe**: August 2025 – January 2026.
- **Data Quality**: Includes randomized utilities, weekly groceries, salary income, and a hierarchical category tree with UI color codes.
- **Reliability**: Sequence IDs are reset on migration; ID 1 is guaranteed for the primary user and account.

## 6. Technical Stack
- **Runtime**: Java 21 / Spring Boot 3.5.3.
- **Database**: PostgreSQL with Flyway Migrations.
- **JSON**: Jackson configured for `LocalDate` and `BigDecimal` precision.
