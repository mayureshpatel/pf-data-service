package filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("correlationId", correlationId);

        long startTime = System.currentTimeMillis();
        try {
            logRequest(request);
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logResponse(request, response, duration);
            MDC.clear();
        }
    }

    private void logRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryParams = request.getQueryString() != null ? "?" + request.getQueryString() : "";
        String contentType = request.getContentType();

        // avoid logging body for file uploads
        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            log.info("REQ: {} {}{} [Multipart File Upload]", method, uri, queryParams);
        } else {
            log.info("REQ: {} {}{}", method, uri, queryParams);
        }
    }

    private void logResponse(HttpServletRequest request, HttpServletResponse response, long duration) {
        int status = response.getStatus();
        String method = request.getMethod();
        String uri = request.getRequestURI();

        if (status >= 500) {
            log.error("RES: {} {} - Status: {} ({} ms)", method, uri, status, duration);
        } else if (status >= 400) {
            log.warn("RES: {} {} - Status: {} ({} ms)", method, uri, status, duration);
        } else {
            log.info("RES: {} {} - Status: {} ({} ms)", method, uri, status, duration);
        }
    }
}
