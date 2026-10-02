package com.mayureshpatel.pfdataservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Central {@code @ControllerAdvice} that converts every handled exception type into a
 * consistent RFC 7807 {@link ProblemDetail} response, so API callers get the same JSON shape
 * ({@code type}/{@code title}/{@code status}/{@code detail}/{@code instance}, plus an added
 * {@code validationErrors} property where field-level detail applies) no matter which layer threw.
 * Per this project's logging convention, every handler below logs at {@code WARN} -- these are all
 * expected, client-attributable failure conditions -- except the catch-all
 * {@link #handleRuntimeException}, which logs at {@code ERROR} with the full stack trace; keeping
 * {@code ERROR} confined to the one handler that can't attribute the failure to a recognized cause
 * avoids logging the same root exception twice at two different levels.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * @param ex the exception describing which resource wasn't found
     * @param request the failed request
     * @return a 404 problem detail using {@code ex}'s own message
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleEntityNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Entity Not Found: {} at {}", ex.getMessage(), request.getRequestURI());
        return createProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /**
     * @param ex the exception describing the invalid argument
     * @param request the failed request
     * @return a 400 problem detail using {@code ex}'s own message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal Argument: {} at {}", ex.getMessage(), request.getRequestURI());
        return createProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Mapped to 409 rather than 400 because an {@link IllegalStateException} in this codebase
     * signals an operation that conflicts with the current state of the data (e.g. an invalid
     * transition), not malformed input.
     *
     * @param ex the exception describing the invalid state
     * @param request the failed request
     * @return a 409 problem detail using {@code ex}'s own message
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        log.warn("Illegal State: {} at {}", ex.getMessage(), request.getRequestURI());
        return createProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /**
     * @param ex the parsing failure (its own message is logged but not returned to the caller)
     * @param request the failed request
     * @return a 400 problem detail with a generic, user-facing message
     */
    @ExceptionHandler(CsvParsingException.class)
    public ProblemDetail handleCsvParsingException(CsvParsingException ex, HttpServletRequest request) {
        log.warn("CSV Parsing Error: {} at {}", ex.getMessage(), request.getRequestURI());
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Failed to parse the provided CSV file. Please check the file format and try again.", request);
    }

    /**
     * @param ex the exception describing which import was a duplicate
     * @param request the failed request
     * @return a 409 problem detail using {@code ex}'s own message
     */
    @ExceptionHandler(DuplicateImportException.class)
    public ProblemDetail handleDuplicateImportException(DuplicateImportException ex, HttpServletRequest request) {
        log.warn("Duplicate Import: {} at {}", ex.getMessage(), request.getRequestURI());
        return createProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /**
     * @param ex the exception describing which field (username or email) already exists
     * @param request the failed request
     * @return a 409 problem detail using {@code ex}'s own message
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("User Already Exists: {} at {}", ex.getMessage(), request.getRequestURI());
        return createProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /**
     * Handles {@code @Valid}-triggered bean validation failures on request bodies, attaching the
     * per-field errors as a {@code validationErrors} list (each entry a {@code field}/{@code
     * message} pair) rather than collapsing them into a single string.
     *
     * @param ex the validation failure, carrying the per-field binding errors
     * @param request the failed request
     * @return a 400 problem detail with a {@code validationErrors} property attached
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation Failed at {}: {}", request.getRequestURI(), ex.getBindingResult());

        List<Map<String, String>> validationErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> Map.of("field", error.getField(), "message", error.getDefaultMessage()))
                .toList();

        ProblemDetail problemDetail = createProblemDetail(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields", request);
        problemDetail.setProperty("validationErrors", validationErrors);
        return problemDetail;
    }

    /**
     * @param ex the exception, thrown by the multipart parser before any handler method runs
     * @param request the failed request
     * @return a 413 problem detail with a generic, user-facing message
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxSizeException(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Upload Size Exceeded at {}", request.getRequestURI());
        return createProblemDetail(HttpStatus.PAYLOAD_TOO_LARGE, "File too large. Please upload a file smaller than the configured limit.", request);
    }

    /**
     * Handles Spring's own "no handler/static resource matched this path" exception -- distinct
     * from {@link #handleEntityNotFound}, which is this application's own domain-level not-found.
     *
     * @param ex the exception (its own message is logged but not returned to the caller)
     * @param request the failed request
     * @return a 404 problem detail with a generic message
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("No Resource Found: {} at {}", ex.getMessage(), request.getRequestURI());
        return createProblemDetail(HttpStatus.NOT_FOUND, "The requested resource was not found.", request);
    }

    /**
     * @param ex the exception, naming the offending parameter and its expected type
     * @param request the failed request
     * @return a 400 problem detail naming the offending parameter and its expected type
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type Mismatch: {} at {}", ex.getMessage(), request.getRequestURI());
        String detail = String.format("Parameter '%s' should be of type '%s'", ex.getName(), ex.getRequiredType().getSimpleName());
        return createProblemDetail(HttpStatus.BAD_REQUEST, detail, request);
    }

    /**
     * Handles both Spring Security exception types under one 403 response, since which of the two
     * is thrown depends on where authorization is enforced (method security vs. the filter chain)
     * rather than anything the caller needs to distinguish between.
     *
     * @param ex the access-denied exception
     * @param request the failed request
     * @return a 403 problem detail using {@code ex}'s own message, or a generic fallback if it has
     *     none
     */
    @ExceptionHandler({
            org.springframework.security.access.AccessDeniedException.class,
            org.springframework.security.authorization.AuthorizationDeniedException.class
    })
    public ProblemDetail handleAccessDenied(Exception ex, HttpServletRequest request) {
        log.warn("Access Denied: {} at {}", ex.getMessage(), request.getRequestURI());
        String message = ex.getMessage() != null ? ex.getMessage() : "You do not have permission to access this resource.";
        return createProblemDetail(HttpStatus.FORBIDDEN, message, request);
    }

    /**
     * @param ex the constraint violation (its own message is logged but not returned to the
     *     caller, since it can include raw schema/constraint details)
     * @param request the failed request
     * @return a 400 problem detail with a generic message
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data Integrity Violation: {} at {}", ex.getMessage(), request.getRequestURI());
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Database constraint violation. Please check your input data.", request);
    }

    /**
     * Deliberately returns the same generic message whether the username is unknown or the
     * password is wrong, so the response itself can't be used to enumerate valid usernames.
     *
     * @param ex the authentication failure
     * @param request the failed request
     * @return a 401 problem detail with a generic message
     */
    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(org.springframework.security.authentication.BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad Credentials at {}", request.getRequestURI());
        return createProblemDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password.", request);
    }

    /**
     * @param ex the exception, thrown when a row's version no longer matches what was read
     * @param request the failed request
     * @return a 409 problem detail telling the caller to refresh and retry
     */
    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailure(org.springframework.dao.OptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Optimistic Locking Failure: {} at {}", ex.getMessage(), request.getRequestURI());
        return createProblemDetail(HttpStatus.CONFLICT, "This record was modified by another request. Please refresh and try again.", request);
    }

    /**
     * Handles {@code jakarta.validation} constraint violations raised outside a {@code @Valid}
     * method argument (e.g. {@code @Validated} path/query parameters) -- same
     * {@code validationErrors} response shape as {@link #handleValidationErrors}, just sourced
     * from a different validation mechanism.
     *
     * @param ex the constraint violation failure
     * @param request the failed request
     * @return a 400 problem detail with a {@code validationErrors} property attached
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(jakarta.validation.ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint Violation at {}: {}", request.getRequestURI(), ex.getMessage());

        List<Map<String, String>> validationErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> Map.of(
                        "field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()))
                .toList();

        ProblemDetail problemDetail = createProblemDetail(HttpStatus.BAD_REQUEST, "Validation failed for one or more parameters", request);
        problemDetail.setProperty("validationErrors", validationErrors);
        return problemDetail;
    }

    /**
     * @param ex the exception (its own message is logged but not returned to the caller)
     * @param request the failed request
     * @return a 400 problem detail with a generic message
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed Request Body at {}: {}", request.getRequestURI(), ex.getMessage());
        return createProblemDetail(HttpStatus.BAD_REQUEST, "The request body is missing or malformed. Please check your JSON syntax.", request);
    }

    /**
     * @param ex the exception, naming the missing parameter
     * @param request the failed request
     * @return a 400 problem detail naming the missing parameter
     */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingRequestParameter(org.springframework.web.bind.MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing Request Parameter at {}: {}", request.getRequestURI(), ex.getMessage());
        String detail = String.format("Required parameter '%s' is missing", ex.getParameterName());
        return createProblemDetail(HttpStatus.BAD_REQUEST, detail, request);
    }

    /**
     * @param ex the exception, naming the attempted method and the methods actually supported
     * @param request the failed request
     * @return a 405 problem detail listing the supported methods
     */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Method Not Supported at {}: {}", request.getRequestURI(), ex.getMessage());
        String supported = ex.getSupportedMethods() != null ? String.join(", ", ex.getSupportedMethods()) : "none";
        String detail = String.format("Method '%s' is not supported for this endpoint. Supported methods: %s", ex.getMethod(), supported);
        return createProblemDetail(HttpStatus.METHOD_NOT_ALLOWED, detail, request);
    }

    /**
     * Catch-all for any exception not matched by a more specific handler above. Logs at
     * {@code ERROR} with the full stack trace -- the one deliberate exception to this class's
     * otherwise-{@code WARN}-only logging, since an unrecognized exception is exactly the case
     * where the stack trace is needed to diagnose what happened. The response deliberately omits
     * {@code ex}'s own message to avoid leaking internal details to the caller.
     *
     * @param ex the unhandled exception
     * @param request the failed request
     * @return a 500 problem detail with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleRuntimeException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: ", request.getRequestURI(), ex);
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred. Please contact support.", request);
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }
}