package com.mayureshpatel.pfdataservice.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PF-813: this filter (the surviving copy, after removing the orphaned {@code filter}-package
 * duplicate) had no dedicated unit test at all. Captures log output via a Logback
 * {@link ListAppender} rather than only asserting observable side effects, since the log message
 * shape itself -- including whether it carries the authenticated user id -- is exactly what
 * distinguished this filter from the orphaned one this ticket removed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RequestLoggingFilter Unit Tests")
class RequestLoggingFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private final RequestLoggingFilter filter = new RequestLoggingFilter();
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        logger.detachAppender(logAppender);
        MDC.clear();
    }

    @Nested
    @DisplayName("logging level by response status")
    class LogLevelTests {

        @Test
        @DisplayName("should log at INFO for a successful response")
        void shouldLogAtInfoForSuccess() throws ServletException, IOException {
            // arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/accounts");
            when(response.getStatus()).thenReturn(200);

            // act
            filter.doFilterInternal(request, response, filterChain);

            // assert & verify
            verify(filterChain).doFilter(request, response);
            assertThat(logAppender.list).hasSize(1);
            assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.INFO);
        }

        @Test
        @DisplayName("should log at WARN for a 4xx response")
        void shouldLogAtWarnForClientError() throws ServletException, IOException {
            // arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/accounts/999");
            when(response.getStatus()).thenReturn(404);

            // act
            filter.doFilterInternal(request, response, filterChain);

            // assert & verify
            assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.WARN);
        }

        @Test
        @DisplayName("should log at ERROR for a 5xx response")
        void shouldLogAtErrorForServerError() throws ServletException, IOException {
            // arrange
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/v1/transactions");
            when(response.getStatus()).thenReturn(500);

            // act
            filter.doFilterInternal(request, response, filterChain);

            // assert & verify
            assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
        }
    }

    @Nested
    @DisplayName("authenticated user id in the log message")
    class UserIdTests {

        @Test
        @DisplayName("should include the authenticated username when a real user is present")
        void shouldIncludeAuthenticatedUsername() throws ServletException, IOException {
            // arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/accounts");
            when(response.getStatus()).thenReturn(200);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("mayuresh", null, Collections.emptyList()));

            // act
            filter.doFilterInternal(request, response, filterChain);

            // assert & verify
            assertThat(logAppender.list.get(0).getFormattedMessage()).contains("User: mayuresh");
        }

        @Test
        @DisplayName("should log \"anonymous\" when there is no authenticated user")
        void shouldLogAnonymousWhenUnauthenticated() throws ServletException, IOException {
            // arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/accounts");
            when(response.getStatus()).thenReturn(200);

            // act
            filter.doFilterInternal(request, response, filterChain);

            // assert & verify
            assertThat(logAppender.list.get(0).getFormattedMessage()).contains("User: anonymous");
        }
    }

    @Nested
    @DisplayName("correlation id MDC handling")
    class CorrelationIdTests {

        @Test
        @DisplayName("should remove the correlationId MDC key after the request completes, not leak it")
        void shouldRemoveCorrelationIdAfterRequest() throws ServletException, IOException {
            // arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/accounts");
            when(response.getStatus()).thenReturn(200);

            // act
            filter.doFilterInternal(request, response, filterChain);

            // assert & verify
            assertThat(MDC.get("correlationId")).isNull();
        }

        @Test
        @DisplayName("should have a correlationId set in MDC while the downstream chain executes")
        void shouldSetCorrelationIdDuringChainExecution() throws ServletException, IOException {
            // arrange -- assertion runs inside the stubbed chain call, mid-act
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/accounts");
            when(response.getStatus()).thenReturn(200);
            doAnswer(invocation -> {
                assertThat(MDC.get("correlationId")).isNotNull();
                return null;
            }).when(filterChain).doFilter(request, response);

            // act
            filter.doFilterInternal(request, response, filterChain);
        }

        @Test
        @DisplayName("should remove correlationId even when the downstream chain throws")
        void shouldRemoveCorrelationIdEvenOnException() throws ServletException, IOException {
            // arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/accounts");
            when(response.getStatus()).thenReturn(500);
            doThrow(new IOException("boom")).when(filterChain).doFilter(request, response);

            // act & assert
            assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                    .isInstanceOf(IOException.class);
            assertThat(MDC.get("correlationId")).isNull();
        }
    }
}
