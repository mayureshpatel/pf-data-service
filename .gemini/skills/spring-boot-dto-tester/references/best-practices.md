# Spring Boot DTO Unit Testing Best Practices

DTOs (Data Transfer Objects) define the API contract. Request DTOs handle input validation, while Response DTOs (often records) handle output structure.

## 1. Core Principles
- **Schema Alignment**: Request DTO constraints (e.g., `@Size`, `@NotNull`) MUST match the database column definitions found in `src/main/resources/db/migration/`.
- **Validation Testing**: For Request DTOs, use `jakarta.validation.Validator` to verify that constraints correctly pass or fail.
- **POJO Testing**: Keep DTO tests as pure JUnit 5 tests. Avoid loading the full Spring context.
- **Completeness**: Test every field in the DTO, ensuring they are correctly populated via builders or constructors.

## 2. Testing Request DTOs (Validation)
- **Positive Test**: Create a valid DTO and assert that the validator returns zero violations.
- **Negative Tests**: For each field with validation annotations:
    - Test null values (if `@NotNull` or `@NotBlank` is present).
    - Test empty strings (if `@NotBlank` is present).
    - Test boundary limits (e.g., strings longer than `@Size(max=...)`).
    - Test range limits (e.g., numbers outside `@Min` or `@Max`).
- **Violation Message**: Assert that the violation message matches the one defined in the annotation.

## 3. Testing Response DTOs (Records/Classes)
- **Structure**: Verify that all fields are correctly returned via accessors.
- **Builders**: If using `@Builder`, verify that the builder correctly maps all fields.
- **JSON Serialization**: (Optional but recommended) Verify that the DTO serializes to the expected JSON format if custom `@JsonProperty` or naming strategies are used.

## 4. Organizational Structure
- **Group by Field**: Use `@Nested` classes to group validation tests for a specific field (e.g., `class NameValidationTests`).
- **Descriptive Names**: Use `@DisplayName` to explain the specific constraint being tested (e.g., "should fail when name exceeds 50 characters").
