# Code Review: pf-data-service

**Date:** January 21, 2026
**Reviewer:** Senior Software Engineer (Architecture & Data Analysis)
**Scope:** Full Codebase Audit - Architecture, Security, Data Modeling, Performance

---

## Executive Summary

The pf-data-service is a well-structured Spring Boot 3.5 REST API for personal finance management. The codebase demonstrates solid architectural patterns (layered architecture, factory pattern, specification pattern) and good JPA practices. However, there are **critical security vulnerabilities** that must be addressed before any deployment, along with several data integrity concerns and opportunities for optimization.

**Overall Assessment:** Good foundation for an MVP, but requires security hardening and data model refinements before production use.

---

## 1. Critical Issues (Must Fix)

### 1.1 Security - Authentication Bypass Backdoor (CRITICAL)

**Location:** `JwtAuthenticationFilter.java:46-50`

```java
if ("mock-dev-token".equals(jwt)) {
    authenticateMockUser(request);
    filterChain.doFilter(request, response);
    return;
}
```

**Risk:** Anyone with network access can bypass authentication by using `Authorization: Bearer mock-dev-token`. This authenticates as the first user found in the database (preferring "admin").

**Impact:** Complete authentication bypass - an attacker gains full access to any user's financial data.

**Recommendation:** Remove this code entirely, or gate it behind a profile check:
```java
@Value("${app.security.mock-auth-enabled:false}")
private boolean mockAuthEnabled;
```

### 1.2 Security - Hardcoded JWT Secret (HIGH)

**Location:** `application.yml:37` and `JwtService.java:20-21`

The JWT secret is hardcoded in configuration and source code as a default value:
```yaml
secret-key: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
```

**Impact:** If the codebase is public or leaked, tokens can be forged by anyone.

**Recommendation:**
1. Remove the default value entirely
2. Require `JWT_SECRET` environment variable
3. Fail startup if not provided in production profiles

### 1.3 Bug - TagRepository Interface Broken

**Location:** `TagRepository.java`

```java
@Repository
public interface TagRepository {
    List<Tag> findByUserId(Long userId);
    // ... methods declared but no implementation
}
```

**Problem:** TagRepository does NOT extend `JpaRepository<Tag, Long>`. This interface cannot be instantiated by Spring Data JPA and will cause runtime errors if injected.

**Fix:** Add `extends JpaRepository<Tag, Long>` to the interface declaration.

---

## 2. Data Integrity Issues (High Priority)

### 2.1 Missing Optimistic Locking on Account

**Location:** `Account.java`

**Problem:** Account entity lacks a `@Version` field. In concurrent scenarios (e.g., two CSV imports to the same account), balance updates can be lost due to race conditions.

**Scenario:**
1. Thread A reads account (balance: $1000)
2. Thread B reads account (balance: $1000)
3. Thread A applies +$100, saves (balance: $1100)
4. Thread B applies -$50, saves (balance: $950) ← Thread A's change is lost!

**Recommendation:**
```java
@Version
private Long version;
```

### 2.2 CategoryRule References Category by Name (String)

**Location:** `CategoryRule.java:24-25`

```java
@Column(name = "category_name", nullable = false, length = 50)
private String categoryName;
```

**Problem:** CategoryRule stores category as a string instead of a foreign key relationship. This creates:
- No referential integrity (rules can reference non-existent categories)
- No cascade on category rename/delete (orphaned rules)
- No ability to link to category metadata (color, parent, etc.)

**Recommendation:** Either add a `@ManyToOne` relationship to Category, or implement application-level validation that category exists when rule is created/used.

### 2.3 Missing Unique Constraints

**Missing constraints identified:**

| Entity | Recommended Constraint |
|--------|----------------------|
| Category | `UNIQUE(user_id, name)` |
| Tag | `UNIQUE(user_id, name)` |
| CategoryRule | `UNIQUE(user_id, keyword)` |
| VendorRule | `UNIQUE(user_id, keyword)` |

**Impact:** Users can create duplicate categories/tags with the same name, causing confusion in queries and reports.

### 2.4 Inconsistent Soft Delete Implementation

| Entity | Has Soft Delete |
|--------|----------------|
| User | ✅ |
| Account | ✅ |
| Transaction | ✅ |
| Category | ❌ |
| Tag | ❌ |
| CategoryRule | ❌ |
| VendorRule | ❌ |

**Problem:** Deleting a Category that has transactions referencing it will either fail (FK constraint) or orphan the category_id on transactions.

---

## 3. Architecture & Design Issues

### 3.1 Inconsistent Exception Handling

**Problem:** Services use different exception types for the same semantic:

| Service | Exception Used |
|---------|---------------|
| TransactionService | `AccessDeniedException` ✅ |
| VendorRuleService | `AccessDeniedException` ✅ |
| AccountService | `RuntimeException("Access denied")` ❌ |
| CategoryService | `RuntimeException("Access denied")` ❌ |

**Impact:** GlobalExceptionHandler doesn't catch generic RuntimeException with custom message, resulting in 500 errors instead of 403.

**Recommendation:** Use `org.springframework.security.access.AccessDeniedException` consistently.

### 3.2 Repeated DTO Mapping Code

**Problem:** Every service has its own `mapToDto()` method with similar boilerplate:

- `TransactionService.mapToDto()`
- `AccountService.mapToDto()`
- `CategoryService.mapToDto()`
- `VendorRuleService.mapToDto()`

**Recommendation:** Consider MapStruct for centralized, type-safe mapping. Example:

```java
@Mapper(componentModel = "spring")
public interface TransactionMapper {
    TransactionDto toDto(Transaction entity);
    Transaction toEntity(TransactionDto dto);
}
```

### 3.3 Category Lookup Inefficiency

**Location:** `TransactionService.java:93-97`

```java
categoryRepository.findByUserId(userId).stream()
    .filter(c -> c.getName().equalsIgnoreCase(dto.categoryName()))
    .findFirst()
    .ifPresent(transaction::setCategory);
```

**Problem:** Loads ALL categories for user, then filters in Java. With many categories, this is wasteful.

**Recommendation:** Add repository method:
```java
Optional<Category> findByUserIdAndNameIgnoreCase(Long userId, String name);
```

### 3.4 Inconsistent API Path Patterns

**Location:** `AccountController.java` and `CategoryController.java`

```java
@RequestMapping({"/api/v1/accounts", "/api/accounts"})
```

**Problem:** Mapping to both versioned and unversioned paths creates API ambiguity. Clients may use inconsistent paths.

**Recommendation:** Pick one pattern and stick with it. Versioned (`/api/v1/`) is preferred for future compatibility.

### 3.5 CSV Parser Code Duplication

**Problem:** All four parsers (Standard, CapitalOne, Discover, Synovus) have nearly identical:
- Resource cleanup logic in `onClose()`
- Exception handling patterns
- `parseAmount()` implementations

**Recommendation:** Create an `AbstractCsvParser` base class:
```java
public abstract class AbstractCsvParser implements TransactionParser {
    protected BigDecimal parseAmount(String value) { /* shared logic */ }
    protected Stream<Transaction> createParsedStream(CSVParser parser, Function<CSVRecord, Optional<Transaction>> mapper) { /* shared cleanup */ }
}
```

---

## 4. Performance Considerations

### 4.1 N+1 Query in deleteTransactions (Improved but Not Optimal)

**Location:** `TransactionService.java:46-60`

Current implementation:
```java
long ownedCount = transactionRepository.countByIdInAndAccount_User_Id(transactionIds, userId);
// ... then
List<Transaction> transactions = transactionRepository.findAllById(transactionIds);
for (Transaction t : transactions) {
    t.getAccount().undoTransaction(t); // Could trigger lazy load
}
```

**Analysis:** The ownership check is now O(1), which is good. However, `undoTransaction()` accesses `t.getAccount()`, which may not be eagerly loaded by `findAllById()`.

**Recommendation:** Use `findAllByIdWithAccountAndUser()` (which exists) instead of `findAllById()`:
```java
List<Transaction> transactions = transactionRepository.findAllByIdWithAccountAndUser(transactionIds);
```

### 4.2 Missing Database Index for Common Queries

Based on repository methods, these indices would help (some may already exist in migrations):

```sql
-- For getTransactions filtered by category name
CREATE INDEX idx_transactions_category ON transactions(category_id);

-- For vendor/category rule lookups
CREATE INDEX idx_vendor_rules_user_priority ON vendor_rules(user_id, priority DESC);
CREATE INDEX idx_category_rules_user_priority ON category_rules(user_id, priority DESC);
```

### 4.3 Dashboard Service - Daily Net Worth Calculation

**Location:** `DashboardService.java:49-76`

**Current Approach:** Calculates 90-day history by:
1. Getting current total balance
2. Fetching all daily net flows for 90 days
3. Iterating backwards, subtracting each day's flow

**Analysis:** This is actually a reasonable approach for an MVP. The calculation is O(n) where n = days. For 90 days, this is fine.

**Future Consideration:** For larger date ranges, pre-computed snapshots (via `SnapshotService`) would be more efficient.

---

## 5. Testing Gaps

### 5.1 Missing Test Coverage

| Component | Test Exists | Coverage Quality |
|-----------|-------------|------------------|
| TransactionService | ✅ | Good basic coverage |
| TransactionImportService | ✅ | Exists |
| DashboardService | ✅ | Basic |
| TransactionSpecification | ❌ | **No tests** |
| SecurityService | ❌ | **No tests** |
| SnapshotService | ✅ | Basic |
| CSV Parsers | ✅ | Good |

### 5.2 Missing Integration Tests

No `@DataJpaTest` or `@SpringBootTest` tests found for:
- Repository custom queries (JPQL correctness)
- End-to-end transaction import flow
- Security authorization rules

**Recommendation:** Add integration tests for critical paths:
```java
@DataJpaTest
class TransactionRepositoryIntegrationTest {
    @Test
    void getDailyNetFlows_shouldCalculateCorrectly() { /* ... */ }
}
```

---

## 6. Positive Observations

### 6.1 Good Practices Found

1. **Soft Delete Pattern:** Excellent implementation using `@SQLDelete` and `@SQLRestriction`
2. **JPA equals/hashCode:** Proper implementation handling Hibernate proxies
3. **Specification Pattern:** `TransactionSpecification` provides flexible, composable queries
4. **Factory Pattern:** `TransactionParserFactory` cleanly abstracts bank-specific parsing
5. **ProblemDetail Responses:** Modern Spring 6 error responses with proper RFC 7807 format
6. **Batch Configuration:** `hibernate.jdbc.batch_size: 50` with `order_inserts/updates` enabled
7. **Connection Pool:** HikariCP properly configured with reasonable defaults
8. **Custom @WithCustomMockUser:** Excellent testing utility for authenticated tests

### 6.2 Architecture Strengths

- Clean layered architecture (Controller → Service → Repository)
- Proper separation of concerns
- DTOs separate from entities
- Validation on DTOs using Jakarta Validation
- CORS properly configured

---

## 7. Summary of Findings by Severity

### CRITICAL (Fix Immediately)
1. Mock token authentication bypass
2. Hardcoded JWT secret
3. TagRepository missing JpaRepository extends

### HIGH (Fix Before Production)
4. Missing @Version on Account
5. Inconsistent AccessDeniedException usage
6. Missing unique constraints on Category/Tag names

### MEDIUM (Technical Debt)
7. CategoryRule string reference to category
8. Inconsistent soft delete coverage
9. Code duplication in mapToDto methods
10. Code duplication in CSV parsers
11. API path inconsistency

### LOW (Nice to Have)
12. Category lookup inefficiency
13. MapStruct integration
14. Additional test coverage

---

## Appendix: File Reference

| Issue | Primary File Location |
|-------|----------------------|
| Mock Token | `security/JwtAuthenticationFilter.java:46` |
| JWT Secret | `application.yml:37`, `security/JwtService.java:20` |
| TagRepository | `repository/TagRepository.java` |
| Account @Version | `model/Account.java` |
| CategoryRule | `model/CategoryRule.java:24` |
| AccessDeniedException | `service/AccountService.java:49`, `service/CategoryService.java:48` |
| API Paths | `controller/AccountController.java:15` |
