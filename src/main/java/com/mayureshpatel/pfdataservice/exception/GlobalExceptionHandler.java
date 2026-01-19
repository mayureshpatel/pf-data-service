package com.mayureshpatel.pfdataservice.exception;

import jakarta.persistence.EntityNotFoundException;
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

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("Entity Not Found: {} at {}", ex.getMessage(), request.getRequestURI());
        return createProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal Argument: {} at {}", ex.getMessage(), request.getRequestURI());
        return createProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(CsvParsingException.class)
    public ProblemDetail handleCsvParsingException(CsvParsingException ex, HttpServletRequest request) {
        log.warn("CSV Parsing Error: {} at {}", ex.getMessage(), request.getRequestURI());
        return createProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateImportException.class)
    public ProblemDetail handleDuplicateImportException(DuplicateImportException ex, HttpServletRequest request) {
        log.warn("Duplicate Import: {} at {}", ex.getMessage(), request.getRequestURI());
        return createProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxSizeException(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Upload Size Exceeded at {}", request.getRequestURI());
        return createProblemDetail(HttpStatus.PAYLOAD_TOO_LARGE, "File too large. Please upload a file smaller than the configured limit.", request);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("No Resource Found: {} at {}", ex.getMessage(), request.getRequestURI());
        return createProblemDetail(HttpStatus.NOT_FOUND, "The requested resource was not found.", request);
    }

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