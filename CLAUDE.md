# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Personal Finance Data Service - A Spring Boot 3.5.3 REST API backend for personal finance management. Uses PostgreSQL for persistence, JWT for authentication, and integrates with an Angular frontend.

## Build & Development Commands

```bash
# Build
./mvnw clean package

# Run locally
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=TransactionServiceTest

# Run a single test method
./mvnw test -Dtest=TransactionServiceTest#testCreateTransaction

# Database migrations
./mvnw flyway:migrate

# Clean and compile
./mvnw clean compile
```

## Architecture

**Layered Spring MVC Architecture:**

```
Controller → Service → Repository → PostgreSQL
     ↓           ↓
    DTO       Entity (JPA)
```

**Key Packages:**
- `controller/` - REST endpoints (7 controllers)
- `service/` - Business logic including `parser/` (CSV parsers) and `categorization/` (auto-categorization)
- `repository/` - Spring Data JPA repositories with `specification/` for dynamic queries
- `model/` - JPA entities with soft deletes via `@SQLDelete`/`@SQLRestriction`
- `security/` - JWT authentication filter and service
- `config/` - Spring configuration (Security, CORS, OpenAPI)

**Entity Relationships:**
- User → Account → Transaction, AccountSnapshot
- Transaction → Category (N:1), Tags (N:M)
- Category → CategoryRule (auto-categorization rules)
- VendorRule (rules-based vendor name cleaning)

**CSV Import Flow:**
`TransactionController.importTransactions()` → `TransactionImportService` → Bank-specific parser (Standard, CapitalOne, Discover, Synovus) → `VendorCleaner` → `TransactionCategorizer` → `TransactionRepository`

## Critical Guidelines

**Flyway Migrations:**
- Always create new migration files in `src/main/resources/db/migration/`
- Never modify existing migration files
- Follow naming: `V{number}__{description}.sql`

**Soft Deletes:**
- Core entities (Account, Transaction) use soft deletes
- Queries automatically filter deleted records via `@SQLRestriction`
- Use `deleted_at` timestamp field

**Security Context:**
- All endpoints require authentication except `/api/v1/auth/**`
- Use `@PreAuthorize("@securityService.isOwner...")` for ownership validation
- JWT secret currently hardcoded (known issue - use env var in production)

**Testing:**
- Use `@WithCustomMockUser` annotation for authenticated test contexts
- TestContainers available for integration tests
- Test classes exist for controllers, services, and parsers

## Known Issues (from CODE_REVIEW.md)

1. **Transfer Logic Inconsistency** - `TransactionType.TRANSFER` handled differently in service vs reporting layers
2. **Missing Optimistic Locking** - Account entity lacks `@Version` field for concurrent updates
3. **Dormant SnapshotService** - Reporting infrastructure exists but scheduling not enabled

## Configuration

- **Port:** 8080
- **Database:** PostgreSQL (env vars: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)
- **CORS:** Configured for localhost:4200 (Angular frontend)
- **API Docs:** Available at `/swagger-ui/` and `/v3/api-docs/`
