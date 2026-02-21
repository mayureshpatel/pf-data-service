package com.mayureshpatel.pfdataservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String userId = getAuthenticatedUserId();
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();

            String logMessage = String.format("User: %s | %s %s - Status: %d (%d ms)",
                    userId, method, uri, status, duration);

            if (status >= 500) {
                log.error(logMessage);
            } else if (status >= 400) {
                log.warn(logMessage);
            } else {
                log.info(logMessage);
            }
            MDC.remove("correlationId");
        }
    }

    /**
     * Retrieves the user ID from the current security context.
     *
     * @return The user ID if authenticated, otherwise "anonymous".
     */
    private String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return auth.getName();
        }
        return "anonymous";
    }
}
