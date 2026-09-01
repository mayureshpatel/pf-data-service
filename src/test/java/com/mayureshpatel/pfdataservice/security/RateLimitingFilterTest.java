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

    @Test
    @DisplayName("should give /register its own, tighter bucket, independent of other auth endpoints")
    void registerEndpoint_hasIndependentTighterLimit() throws Exception {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);

        // exhaust /authenticate's 10/min bucket first
        when(request.getRequestURI()).thenReturn("/api/v1/auth/authenticate");
        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

        // /register from the same IP is still allowed -- independent bucket, not shared
        when(request.getRequestURI()).thenReturn("/api/v1/auth/register");
        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

        // register's bucket is tighter (5/min) -- the 6th request from the same IP is blocked
        filter.doFilterInternal(request, response, filterChain);
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    @DisplayName("should use remote address and ignore X-Forwarded-For header")
    void rateLimiter_usesRemoteAddr_ignoringXForwardedFor() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/authenticate");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);

        for (int i = 0; i < 10; i++) {
            when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0." + i);
            filter.doFilterInternal(request, response, filterChain);
        }
        
        // 11th request from same remote address but different X-Forwarded-For
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.11");
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(10)).doFilter(request, response);
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }
}
