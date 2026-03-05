package com.mayureshpatel.pfdataservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("should allow requests under the limit for auth endpoints")
    void shouldAllowUnderLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/authenticate");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(10)).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    @DisplayName("should block requests over the limit for auth endpoints")
    void shouldBlockOverLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/authenticate");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);

        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        
        // 11th request
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(10)).doFilter(request, response);
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    @DisplayName("should not rate limit non-auth endpoints")
    void shouldNotLimitNonAuth() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/accounts");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        for (int i = 0; i < 15; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(15)).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }
}
