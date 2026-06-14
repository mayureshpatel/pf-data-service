# Spring Boot Service Unit Testing Best Practices

Service layers contain the core business logic and orchestration of the application. Follow these patterns to ensure they are robustly tested.

## 1. Core Principles
- **Isolation via Mocking**: Use `@ExtendWith(MockitoExtension.class)` and `@Mock` to isolate the service from its dependencies (repositories, other services).
- **AAA Pattern**: Every test method MUST follow the Arrange-Act-Assert pattern, with explicit comments (`// Arrange`, `// Act`, `// Assert`) for each section.
- **Exhaustive Branch Coverage**: Services often have complex conditional logic (e.g., resolving categories, checking ownership). Test every path, including error scenarios.
- **Transactional Verification**: While unit tests don't run in a real transaction, verify that the service calls the repository methods that modify state (insert, update, delete).

## 2. Handling Ambiguity
- **Explicit Matchers**: If a repository has overloaded methods (e.g., `insert(T)` and `insert(Request)`), use explicit casting like `any(Transaction.class)` or `(Transaction) argThat(...)` to avoid compiler ambiguity.

## 3. Testing Logic & Security
- **Ownership Checks**: Always test the "Mismatched Owner" scenario. Verify that `AccessDeniedException` is thrown if a user tries to modify a resource they don't own.
- **Validation**: Test logic that enforces business rules (e.g., "Only subcategories can be assigned").
- **Bulk Operations**: Verify that bulk methods handle null/empty inputs and correctly aggregate results.

## 4. Logical Organization
- **Use `@Nested` Classes**: Group tests by public method name.
- **Descriptive Names**: Use `@DisplayName` to explain the specific business rule or branch being verified.
