package com.mayureshpatel.pfdataservice.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
            // Arrange
            ResourceNotFoundException ex = new ResourceNotFoundException("Not found");

            // Act
            ProblemDetail detail = handler.handleEntityNotFound(ex, request);

            // Assert
            assertEquals(HttpStatus.NOT_FOUND.value(), detail.getStatus());
            assertEquals("Not found", detail.getDetail());
            assertEquals("/api/test", detail.getInstance().toString());
        }

        @Test
        @DisplayName("should handle CsvParsingException")
        void handleCsvParsingException() {
            // Act
            ProblemDetail detail = handler.handleCsvParsingException(new CsvParsingException("Bad CSV"), request);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
            assertEquals("Failed to parse the provided CSV file. Please check the file format and try again.", detail.getDetail());
        }

        @Test
        @DisplayName("should handle DuplicateImportException")
        void handleDuplicateImport() {
            // Act
            ProblemDetail detail = handler.handleDuplicateImportException(new DuplicateImportException("Exists"), request);

            // Assert
            assertEquals(HttpStatus.CONFLICT.value(), detail.getStatus());
            assertEquals("Exists", detail.getDetail());
        }

        @Test
        @DisplayName("should handle UserAlreadyExistsException")
        void handleUserAlreadyExists() {
            // Act
            ProblemDetail detail = handler.handleUserAlreadyExists(new UserAlreadyExistsException("User exists"), request);

            // Assert
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
            // Act
            ProblemDetail detail = handler.handleIllegalArgument(new IllegalArgumentException("Illegal"), request);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
            assertEquals("Illegal", detail.getDetail());
        }

        @Test
        @DisplayName("should handle MethodArgumentNotValidException with field errors")
        void handleValidationErrors() {
            // Arrange
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "test");
            bindingResult.addError(new FieldError("test", "name", "Required"));
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

            // Act
            ProblemDetail detail = handler.handleValidationErrors(ex, request);

            // Assert
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
            // Act
            ProblemDetail detail = handler.handleMaxSizeException(new MaxUploadSizeExceededException(100L), request);

            // Assert
            assertEquals(HttpStatus.PAYLOAD_TOO_LARGE.value(), detail.getStatus());
            assertTrue(detail.getDetail().contains("File too large"));
        }

        @Test
        @DisplayName("should handle NoResourceFoundException")
        void handleNoResource() {
            // Act
            ProblemDetail detail = handler.handleNoResourceFound(new NoResourceFoundException(null, null), request);

            // Assert
            assertEquals(HttpStatus.NOT_FOUND.value(), detail.getStatus());
        }

        @Test
        @DisplayName("should handle MethodArgumentTypeMismatchException")
        void handleTypeMismatch() {
            // Arrange
            MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException("val", Integer.class, "id", null, null);

            // Act
            ProblemDetail detail = handler.handleTypeMismatch(ex, request);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
            assertTrue(detail.getDetail().contains("Parameter 'id' should be of type 'Integer'"));
        }

        @Test
        @DisplayName("should handle AccessDeniedException")
        void handleAccessDenied() {
            // Act
            ProblemDetail detail = handler.handleAccessDenied(new AccessDeniedException("Forbidden"), request);

            // Assert
            assertEquals(HttpStatus.FORBIDDEN.value(), detail.getStatus());
            assertEquals("Forbidden", detail.getDetail());
        }

        @Test
        @DisplayName("should handle AccessDeniedException with null message")
        void handleAccessDeniedNullMessage() {
            // Act
            ProblemDetail detail = handler.handleAccessDenied(new AccessDeniedException(null), request);

            // Assert
            assertEquals(HttpStatus.FORBIDDEN.value(), detail.getStatus());
            assertEquals("You do not have permission to access this resource.", detail.getDetail());
        }

        @Test
        @DisplayName("should handle DataIntegrityViolationException")
        void handleDataIntegrity() {
            // Act
            ProblemDetail detail = handler.handleDataIntegrityViolation(new DataIntegrityViolationException("Violation"), request);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
            assertTrue(detail.getDetail().contains("Database constraint violation"));
        }

        @Test
        @DisplayName("should handle generic Exception as 500")
        void handleGenericException() {
            // Act
            ProblemDetail detail = handler.handleRuntimeException(new RuntimeException("Crash"), request);

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), detail.getStatus());
            assertTrue(detail.getDetail().contains("unexpected internal error"));
        }
    }
}
