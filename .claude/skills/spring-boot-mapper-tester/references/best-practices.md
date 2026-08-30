# Spring Boot Mapper Unit Testing Best Practices

Mappers bridge domain objects and DTOs. Follow these patterns to ensure they are robustly tested.

## 1. Core Principles
- **POJO Testing**: Mapper unit tests should be fast and have zero external dependencies. Do NOT use Spring context.
- **AAA Pattern**: Every test method MUST follow the Arrange-Act-Assert pattern, with explicit comments (`// Arrange`, `// Act`, `// Assert`) for each section.
- **Exhaustive Mapping Coverage**: Test every branch in the mapper (e.g., null checks for optional fields).
- **100% Coverage**: Aim for 100% code and branch coverage for all mapper classes.
- **Utility Class Testing**: Verify that the utility class has a private constructor and can handle `null` inputs gracefully.

## 2. Testing Logic
- **Null Input**: Verify that mapping a `null` object returns `null`.
- **Full Mapping**: Verify that a fully populated source object correctly maps all fields to the target object.
- **Partial Mapping**: Verify that source objects with `null` optional fields are handled correctly without throwing exceptions.
- **Private Constructor**: Use reflection to test the private constructor if 100% code coverage is required.

## 3. Logical Organization
- **Use `@Nested` Classes**: Group tests by mapping method or logic area (e.g., `class ToDtoMappingTests`).
- **Descriptive Names**: Use `@DisplayName` to explain exactly what scenario is being verified.
- **JUnit 5**: Use standard JUnit 5 assertions (`assertEquals`, `assertNotNull`, `assertNull`).
