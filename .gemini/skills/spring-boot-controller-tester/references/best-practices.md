# Spring Boot Controller Unit Testing Best Practices

Follow these patterns to ensure high-quality, maintainable, and robust controller unit tests.

## 1. Architectural Patterns
- **Inherit from `BaseControllerTest`**: Always extend a shared base class that defines common `@MockitoBean` objects. This allows Spring to reuse the `ApplicationContext` and significantly speeds up the test suite.
- **Use `@Nested` Classes**: Group tests by endpoint (e.g., `class GetAccountsTests`, `class CreateAccountTests`). This provides a clear structure and logical grouping.
- **`@DisplayName`**: Use descriptive display names for all classes and test methods to ensure readable test reports.

## 2. Web Layer Testing (MockMvc)
- **Use `MockMvc`**: Test the web layer without starting a full server.
- **`jsonPath` Assertions**: Use `jsonPath` to verify specific fields in the response body. Prefer Hamcrest matchers (e.g., `hasSize()`, `containsInAnyOrder()`) for complex assertions.
- **Content Type**: Always verify `content().contentType(MediaType.APPLICATION_JSON)`.
- **URL Versioning**: If multiple URLs map to the same endpoint, use `@ParameterizedTest` with `@ValueSource` to verify both.

## 3. Security & Authentication
- **`@WithCustomMockUser`**: Use this (or similar) to simulate an authenticated user context. Verify that the user ID from the security context is correctly passed to service methods.
- **CSRF Protection**: For state-changing operations (`POST`, `PUT`, `DELETE`), include `.with(csrf())` in the `MockMvc` request.
- **CSRF Negative Test**: Include a test case that omits the CSRF token to verify it returns `403 Forbidden`.

## 4. Input Validation
- **Test Valid Input**: Verify that correct data returns the expected status (e.g., `201 Created` or `200 OK`).
- **Test Invalid Input**: Verify that missing or malformed fields return `400 Bad Request`.
- **Field Error Assertion**: Assert that the `ProblemDetail` response contains the correct field-level error messages.

## 5. Error Handling & 5xx Scenarios
- **Service Fault Injection**: Mock the service to throw `RuntimeException` and verify the controller returns `500 Internal Server Error`.
- **Not Found (404)**: Mock `ResourceNotFoundException` and verify the controller returns `404 Not Found`.
- **Access Denied (403)**: Mock `AccessDeniedException` and verify the controller returns `403 Forbidden`.

## 6. Service Verification
- **`verify()`**: Always verify that the service layer was called with the expected arguments using Mockito's `verify()`.
- **Argument Capture/Matchers**: Use `eq()`, `any()`, or `ArgumentCaptor` as needed for precise verification.
