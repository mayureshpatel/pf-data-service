# Spring Boot Security Unit Testing Best Practices

Security components (services, filters, details) are critical for application safety. Follow these patterns to ensure they are robustly tested.

## 1. Core Principles
- **POJO vs. Context**: 
    - For high-logic security services (e.g., `JwtService`), use **POJO testing** (no Spring context) for speed and isolation. Use `ReflectionTestUtils` to set `@Value` fields.
    - For components interacting with repositories (e.g., `SecurityService`), use **Mockito** to mock the repository layer.
- **AAA Pattern**: Every test method MUST follow the Arrange-Act-Assert pattern, with explicit comments (`// Arrange`, `// Act`, `// Assert`) for each section.
- **100% Coverage**: Aim for 100% code and branch coverage. Note that some branches in library calls (like JWT parsing) may throw exceptions instead of returning false.

## 2. Testing Security Services
- **Token Logic**: Test generation, parsing, and validation. Explicitly test expiration scenarios (e.g., by setting a negative expiration time).
- **Ownership Logic**: Verify that ownership checks (e.g., `isAccountOwner`) correctly handle positive cases, negative cases (different user), and edge cases (null inputs, missing records).
- **Custom User Details**: Verify that your `UserDetails` implementation correctly maps domain user fields to security authorities and identifiers.

## 3. Logical Organization
- **Use `@Nested` Classes**: Group tests by logic area (e.g., `class TokenGenerationTests`).
- **Descriptive Names**: Use `@DisplayName` to explain the security rule being verified.
- **JUnit 5**: Use standard JUnit 5 assertions.
