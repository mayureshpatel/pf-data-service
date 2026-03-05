# Spring Boot Controller Unit Testing Best Practices

Follow these patterns to ensure high-quality, maintainable, and robust controller unit tests.

## 1. Architectural Patterns
- **Inherit from `BaseControllerTest`**: Always extend a shared base class that defines common `@MockitoBean` objects. This allows Spring to reuse the `ApplicationContext` and significantly speeds up the test suite.
- **Use `@Nested` Classes**: Group tests by endpoint (e.g., `class GetAccountsTests`, `class CreateAccountTests`). This provides a clear structure and logical grouping.
- **AAA Pattern**: Every test method MUST follow the Arrange-Act-Assert pattern, with explicit comments (`// Arrange`, `// Act`, `// Assert`) for each section.
- **`@DisplayName`**: Use descriptive display names for all classes and test methods to ensure readable test reports.

## 2. Request/Response DTO Pattern
- **Dedicated Request Objects**: Use specialized DTOs for input (`CreateRequest`, `UpdateRequest`).
- **Response Objects**: Use DTO counterparts for output (e.g., `AccountDto`).
- **Data Model Alignment**: Request DTO constraints (e.g., `@Size(max=...)`) MUST match the column limits defined in `src/main/resources/db/migration/`. Always verify the latest migration scripts for each field.
- **Update ID in Body**: For `PUT` requests, the `id` should be part of the request body object, NOT a path variable.
- **Validation**: Ensure all request DTOs have appropriate Jakarta Validation annotations (`@NotBlank`, `@NotNull`, `@Positive`, etc.).

## 3. Web Layer Testing (MockMvc)
- **Use `MockMvc`**: Test the web layer without starting a full server.
- **`jsonPath` Assertions**: Use `jsonPath` to verify specific fields in the response body. Prefer Hamcrest matchers (e.g., `hasSize()`, `containsInAnyOrder()`) for complex assertions.
- **Content Type**: Always verify `content().contentType(MediaType.APPLICATION_JSON)`.
- **URL Versioning**: If multiple URLs map to the same endpoint, use `@ParameterizedTest` with `@ValueSource` to verify both.

## 4. Security & Authentication
- **`@WithCustomMockUser`**: Use this to simulate an authenticated user context. Verify that the user ID from the security context is correctly passed to service methods.
- **Security Config Import**: Ensure `BaseControllerTest` imports `SecurityConfig.class` and `JwtAuthenticationFilter.class` to respect real security rules (like `permitAll()` for auth).
- **CSRF Protection**: For state-changing operations (`POST`, `PUT`, `DELETE`), include `.with(csrf())` in the `MockMvc` request if CSRF is enabled.

## 5. Input Validation
- **Test Valid Input**: Verify that correct data returns the expected status.
- **Test Invalid Input**: Verify that missing or malformed fields return `400 Bad Request`.
- **Field Error Assertion**: Assert that the `ProblemDetail` response contains the correct field-level error messages.

## 6. Error Handling & 5xx Scenarios
- **Service Fault Injection**: Mock the service to throw `RuntimeException` and verify the controller returns `500 Internal Server Error`.
- **Not Found (404)**: Mock `ResourceNotFoundException` and verify the controller returns `404 Not Found`.
- **Access Denied (403)**: Mock `AccessDeniedException` and verify the controller returns `403 Forbidden`.

## 7. Service Verification
- **`verify()`**: Always verify that the service layer was called with the expected arguments using Mockito's `verify()`.
