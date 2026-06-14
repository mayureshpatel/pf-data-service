# Spring Boot Exception Unit Testing Best Practices

Custom exceptions encapsulate error state and provide meaningful feedback. Follow these patterns to ensure they are robustly tested.

## 1. Core Principles
- **POJO Testing**: Exception unit tests should be fast and have zero external dependencies. Do NOT use Spring context.
- **AAA Pattern**: Every test method MUST follow the Arrange-Act-Assert pattern, with explicit comments (`// Arrange`, `// Act`, `// Assert`) for each section.
- **Exhaustive Constructor Coverage**: Test every constructor in the exception class to ensure messages and causes are correctly assigned.
- **100% Coverage**: Aim for 100% code and branch coverage for all custom exception classes.

## 2. Testing Logic
- **Simple Messages**: Verify that exceptions created with a simple string message correctly return that message.
- **Formatted Messages**: If the exception supports dynamic formatting (e.g., `Resource not found with id: 123`), verify the resulting message structure.
- **Root Cause Preservation**: Verify that exceptions created with a `Throwable cause` correctly preserve and return that cause.

## 3. Organizational Structure
- **Use `@Nested` Classes**: Group tests by constructor or logic area (e.g., `class ConstructorTests`). This ensures a clean, hierarchical structure.
- **Descriptive Names**: Use `@DisplayName` to explain exactly what scenario is being verified.
- **JUnit 5**: Use standard JUnit 5 assertions (`assertEquals`, `assertNotNull`, `assertNull`).
