package com.mayureshpatel.pfdataservice.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
    }

    @Nested
    @DisplayName("Custom Domain Exceptions")
    class CustomExceptionTests {

        @Test
        @DisplayName("should handle ResourceNotFoundException")
        void handleResourceNotFound() {
            // arrange
            ResourceNotFoundException ex = new ResourceNotFoundException("Not found");

            // act
            ProblemDetail detail = handler.handleEntityNotFound(ex, request);

            // assert & verify
            assertEquals(HttpStatus.NOT_FOUND.value(), detail.getStatus());
            assertEquals("Not found", detail.getDetail());
            assertEquals("/api/test", detail.getInstance().toString());
        }

        @Test
        @DisplayName("should handle CsvParsingException")
        void handleCsvParsingException() {
            // act
            ProblemDetail detail = handler.handleCsvParsingException(new CsvParsingException("Bad CSV"), request);

            // assert & verify
            assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
            assertEquals("Failed to parse the provided CSV file. Please check the file format and try again.", detail.getDetail());
        }

        @Test
        @DisplayName("should handle DuplicateImportException")
        void handleDuplicateImport() {
            // act
            ProblemDetail detail = handler.handleDuplicateImportException(new DuplicateImportException("Exists"), request);

            // assert & verify
            assertEquals(HttpStatus.CONFLICT.value(), detail.getStatus());
            assertEquals("Exists", detail.getDetail());
        }

        @Test
        @DisplayName("should handle UserAlreadyExistsException")
        void handleUserAlreadyExists() {
            // act
            ProblemDetail detail = handler.handleUserAlreadyExists(new UserAlreadyExistsException("User exists"), request);

            // assert & verify
            assertEquals(HttpStatus.CONFLICT.value(), detail.getStatus());
            assertEquals("User exists", detail.getDetail());
        }
    }

    @Nested
    @DisplayName("Standard Spring Exceptions")
    class StandardExceptionTests {

        @Test
        @DisplayName("should handle IllegalArgumentException")
        void handleIllegalArgument() {
            // act
            ProblemDetail detail = handler.handleIllegalArgument(new IllegalArgumentException("Illegal"), request);

            // assert & verify
            assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
            assertEquals("Illegal", detail.getDetail());
        }

        @Test
        @DisplayName("should handle IllegalStateException as 409, not the generic 500 (PF-193)")
        void handleIllegalState() {
            // act
            ProblemDetail detail = handler.handleIllegalState(
                    new IllegalStateException("Cannot delete account with existing transactions. Please delete or move the 3 transaction(s) first."), request);

            // assert & verify
            assertEquals(HttpStatus.CONFLICT.value(), detail.getStatus());
            assertEquals("Cannot delete account with existing transactions. Please delete or move the 3 transaction(s) first.", detail.getDetail());
        }

        @Test
        @DisplayName("should handle MethodArgumentNotValidException with field errors")
        void handleValidationErrors() {
            // arrange
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "test");
            bindingResult.addError(new FieldError("test", "name", "Required"));
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

            // act
            ProblemDetail detail = handler.handleValidationErrors(ex, request);

            // assert & verify
            assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
            assertEquals("Validation failed for one or more fields", detail.getDetail());

            List<Map<String, String>> errors = (List<Map<String, String>>) detail.getProperties().get("validationErrors");
            assertNotNull(errors);
            assertEquals(1, errors.size());
            assertEquals("name", errors.get(0).get("field"));
            assertEquals("Required", errors.get(0).get("message"));
        }

        @Test
        @DisplayName("should handle MaxUploadSizeExceededException")
        void handleMaxSize() {
            // act
            ProblemDetail detail = handler.handleMaxSizeException(new MaxUploadSizeExceededException(100L), request);

            // assert & verify
            assertEquals(HttpStatus.PAYLOAD_TOO_LARGE.value(), detail.getStatus());
            assertTrue(detail.getDetail().contains("File too large"));
        }

        @Test
        @DisplayName("should handle NoResourceFoundException")
        void handleNoResource() {
            // act
            ProblemDetail detail = handler.handleNoResourceFound(new NoResourceFoundException(null, null), request);

            // assert & verify
            assertEquals(HttpStatus.NOT_FOUND.value(), detail.getStatus());
        }

        @Test
        @DisplayName("should handle MethodArgumentTypeMismatchException")
        void handleTypeMismatch() {
            // arrange
            MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException("val", Integer.class, "id", null, null);

            // act
            ProblemDetail detail = handler.handleTypeMismatch(ex, request);

            // assert & verify
            assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
            assertTrue(detail.getDetail().contains("Parameter 'id' should be of type 'Integer'"));
        }

        @Test
        @DisplayName("should handle AccessDeniedException")
        void handleAccessDenied() {
            // act
            ProblemDetail detail = handler.handleAccessDenied(new AccessDeniedException("Forbidden"), request);

            // assert & verify
            assertEquals(HttpStatus.FORBIDDEN.value(), detail.getStatus());
            assertEquals("Forbidden", detail.getDetail());
        }

        @Test
        @DisplayName("should handle AccessDeniedException with null message")
        void handleAccessDeniedNullMessage() {
            // act
            ProblemDetail detail = handler.handleAccessDenied(new AccessDeniedException(null), request);

            // assert & verify
            assertEquals(HttpStatus.FORBIDDEN.value(), detail.getStatus());
            assertEquals("You do not have permission to access this resource.", detail.getDetail());
        }

        @Test
        @DisplayName("should handle DataIntegrityViolationException")
        void handleDataIntegrity() {
            // act
            ProblemDetail detail = handler.handleDataIntegrityViolation(new DataIntegrityViolationException("Violation"), request);

            // assert & verify
            assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
            assertTrue(detail.getDetail().contains("Database constraint violation"));
        }

        @Test
        @DisplayName("should handle BadCredentialsException as 401, not the generic 500")
        void handleBadCredentials() {
            // act
            ProblemDetail detail = handler.handleBadCredentials(
                    new org.springframework.security.authentication.BadCredentialsException("Bad credentials"), request);

            // assert & verify
            assertEquals(HttpStatus.UNAUTHORIZED.value(), detail.getStatus());
            assertEquals("Invalid username or password.", detail.getDetail());
        }

        @Test
        @DisplayName("should handle OptimisticLockingFailureException")
        void handleOptimisticLockingFailure() {
            // act
            ProblemDetail detail = handler.handleOptimisticLockingFailure(
                    new org.springframework.dao.OptimisticLockingFailureException("Version mismatch"), request);

            // assert & verify
            assertEquals(HttpStatus.CONFLICT.value(), detail.getStatus());
            assertTrue(detail.getDetail().contains("modified by another request"));
        }

        @Test
        @DisplayName("should handle ConstraintViolationException with field errors")
        void handleConstraintViolation() {
            // arrange
            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            Path path = mock(Path.class);
            when(path.toString()).thenReturn("month");
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getMessage()).thenReturn("must be less than or equal to 12");
            ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

            // act
            ProblemDetail detail = handler.handleConstraintViolation(ex, request);

            // assert & verify
            assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
            assertEquals("Validation failed for one or more parameters", detail.getDetail());

            List<Map<String, String>> errors = (List<Map<String, String>>) detail.getProperties().get("validationErrors");
            assertNotNull(errors);
            assertEquals(1, errors.size());
            assertEquals("month", errors.get(0).get("field"));
            assertEquals("must be less than or equal to 12", errors.get(0).get("message"));
        }

        @Test
        @DisplayName("should handle HttpMessageNotReadableException")
        void handleMessageNotReadable() {
            // act
            ProblemDetail detail = handler.handleMessageNotReadable(new HttpMessageNotReadableException("bad json"), request);

            // assert & verify
            assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
            assertTrue(detail.getDetail().contains("malformed"));
        }

        @Test
        @DisplayName("should handle MissingServletRequestParameterException")
        void handleMissingRequestParameter() {
            // arrange
            MissingServletRequestParameterException ex = new MissingServletRequestParameterException("bankName", "String");

            // act
            ProblemDetail detail = handler.handleMissingRequestParameter(ex, request);

            // assert & verify
            assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
            assertTrue(detail.getDetail().contains("'bankName'"));
        }

        @Test
        @DisplayName("should handle HttpRequestMethodNotSupportedException")
        void handleMethodNotSupported() {
            // arrange
            HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST", List.of("GET", "PUT"));

            // act
            ProblemDetail detail = handler.handleMethodNotSupported(ex, request);

            // assert & verify
            assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), detail.getStatus());
            assertTrue(detail.getDetail().contains("'POST'"));
            assertTrue(detail.getDetail().contains("GET, PUT"));
        }

        @Test
        @DisplayName("should handle generic Exception as 500")
        void handleGenericException() {
            // act
            ProblemDetail detail = handler.handleRuntimeException(new RuntimeException("Crash"), request);

            // assert & verify
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), detail.getStatus());
            assertTrue(detail.getDetail().contains("unexpected internal error"));
        }
    }
}
