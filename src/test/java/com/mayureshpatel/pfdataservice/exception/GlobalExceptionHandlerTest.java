package com.mayureshpatel.pfdataservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Test
    @DisplayName("handleEntityNotFound should return 404")
    void handleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        when(request.getRequestURI()).thenReturn("/api/test");

        ProblemDetail result = exceptionHandler.handleEntityNotFound(ex, request);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getDetail()).isEqualTo("Not found");
    }

    @Test
    @DisplayName("handleUserAlreadyExists should return 409")
    void handleUserAlreadyExists() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException("Already exists");
        when(request.getRequestURI()).thenReturn("/api/test");

        ProblemDetail result = exceptionHandler.handleUserAlreadyExists(ex, request);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getDetail()).isEqualTo("Already exists");
    }

    @Test
    @DisplayName("handleDuplicateImportException should return 409")
    void handleDuplicateImport() {
        DuplicateImportException ex = new DuplicateImportException("Duplicate");
        when(request.getRequestURI()).thenReturn("/api/test");

        ProblemDetail result = exceptionHandler.handleDuplicateImportException(ex, request);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getDetail()).isEqualTo("Duplicate");
    }

    @Test
    @DisplayName("handleIllegalArgument should return 400")
    void handleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid arg");
        when(request.getRequestURI()).thenReturn("/api/test");

        ProblemDetail result = exceptionHandler.handleIllegalArgument(ex, request);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getDetail()).isEqualTo("Invalid arg");
    }

    @Test
    @DisplayName("handleAccessDenied should return 403")
    void handleAccessDenied() {
        org.springframework.security.access.AccessDeniedException ex = new org.springframework.security.access.AccessDeniedException("Denied");
        when(request.getRequestURI()).thenReturn("/api/test");

        ProblemDetail result = exceptionHandler.handleAccessDenied(ex, request);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(result.getDetail()).isEqualTo("Denied");
    }
}
