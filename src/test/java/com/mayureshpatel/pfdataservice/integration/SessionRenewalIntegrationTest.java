package com.mayureshpatel.pfdataservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end session renewal (PF-899) against a real database: remember-me login, silent refresh whose new
 * access token reaches a protected endpoint, logout ending the session, and the idle and absolute windows,
 * which are driven by swapping in a movable {@link Clock} (the JWT itself keeps using real time).
 */
@AutoConfigureMockMvc
@Import(SessionRenewalIntegrationTest.MovableClockConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionRenewalIntegrationTest extends BaseIntegrationTest {

    private static final String AUTH = "/api/v1/auth/";
    private static final String REFRESH_URI = "/api/v1/auth/refresh";
    private static final String MAX_AGE = "Max-Age=";
    private static final String MAX_AGE_NAME = "Max-Age";
    private static final String COOKIE = "pf_refresh";
    private static final String PASSWORD = "Pass123!@";
    private static final Instant START = Instant.parse("2026-11-05T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MovableClock clock;

    private String username;

    /** A {@link Clock} that tests can advance; the refresh-token service reads time only through it. */
    static class MovableClock extends Clock {
        private Instant now = START;

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        void reset() {
            now = START;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    /** Replaces the system clock bean with the movable one for this test's context only. */
    @TestConfiguration(proxyBeanMethods = false)
    static class MovableClockConfig {
        @Bean
        @Primary
        MovableClock movableClock() {
            return new MovableClock();
        }
    }

    @BeforeAll
    void registerUser() throws Exception {
        username = "renew_" + Long.toHexString(System.nanoTime());
        mockMvc.perform(post(AUTH + "register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"email\":\"" + username + "@example.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
    }

    @BeforeEach
    void resetClock() {
        clock.reset();
    }

    private MvcResult login(boolean rememberMe) throws Exception {
        return mockMvc.perform(post(AUTH + "authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\",\"rememberMe\":" + rememberMe + "}"))
                .andExpect(status().isOk()).andReturn();
    }

    private static String cookieValue(MvcResult result) {
        String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(header).startsWith(COOKIE + "=");
        return header.substring(COOKIE.length() + 1, header.indexOf(';'));
    }

    private static Cookie cookieOf(String value) {
        return new Cookie(COOKIE, value);
    }

    private MvcResult refresh(String cookie) throws Exception {
        return mockMvc.perform(post(REFRESH_URI).cookie(cookieOf(cookie))).andReturn();
    }

    @Test
    @DisplayName("remember-me login, then refresh returns a working access token and a slid cookie")
    void loginThenRefresh() throws Exception {
        // arrange
        MvcResult login = login(true);
        String cookie = cookieValue(login);
        assertThat(login.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains(MAX_AGE + Duration.ofDays(14).toSeconds());
        assertThat(login.getResponse().getContentAsString()).doesNotContain(cookie);
        clock.advance(Duration.ofDays(3));

        // act
        MvcResult refreshed = refresh(cookie);

        // assert & verify
        assertThat(refreshed.getResponse().getStatus()).isEqualTo(200);
        assertThat(refreshed.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains(MAX_AGE + Duration.ofDays(14).toSeconds());
        JsonNode body = objectMapper.readTree(refreshed.getResponse().getContentAsString());
        mockMvc.perform(get("/api/v1/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer " + body.get("token").asText()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("logout revokes the session so a later refresh gets 401 and a cleared cookie")
    void logoutThenRefresh() throws Exception {
        // arrange
        String cookie = cookieValue(login(true));

        // act
        mockMvc.perform(post(AUTH + "logout").cookie(cookieOf(cookie))).andExpect(status().isNoContent());
        MvcResult refreshed = refresh(cookie);

        // assert & verify
        assertThat(refreshed.getResponse().getStatus()).isEqualTo(401);
        assertThat(refreshed.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains(MAX_AGE+"0");
    }

    @Test
    @DisplayName("an idle remember-me session is rejected after 14 days but survives while used")
    void idleWindow() throws Exception {
        // arrange
        String cookie = cookieValue(login(true));

        // act & assert: used every 10 days it stays alive past the 14-day idle window...
        clock.advance(Duration.ofDays(10));
        assertThat(refresh(cookie).getResponse().getStatus()).isEqualTo(200);
        clock.advance(Duration.ofDays(10));
        assertThat(refresh(cookie).getResponse().getStatus()).isEqualTo(200);
        // ...but 15 idle days end it
        clock.advance(Duration.ofDays(15));
        assertThat(refresh(cookie).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("the 60-day absolute window ends even a constantly used remember-me session")
    void absoluteWindow() throws Exception {
        // arrange
        String cookie = cookieValue(login(true));

        // act & assert
        for (int day = 9; day < 60; day += 9) {
            clock.advance(Duration.ofDays(9));
            assertThat(refresh(cookie).getResponse().getStatus()).isEqualTo(200);
        }
        clock.advance(Duration.ofDays(9));
        assertThat(refresh(cookie).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("a session-only session gets a cookie without Max-Age and ends after 8 idle hours")
    void sessionOnly() throws Exception {
        // arrange
        MvcResult login = login(false);
        assertThat(login.getResponse().getHeader(HttpHeaders.SET_COOKIE)).doesNotContain(MAX_AGE_NAME);
        String cookie = cookieValue(login);

        // act & assert
        clock.advance(Duration.ofHours(7));
        MvcResult refreshed = refresh(cookie);
        assertThat(refreshed.getResponse().getStatus()).isEqualTo(200);
        assertThat(refreshed.getResponse().getHeader(HttpHeaders.SET_COOKIE)).doesNotContain(MAX_AGE_NAME);
        clock.advance(Duration.ofHours(9));
        assertThat(refresh(cookie).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("refresh and logout are reachable without a Bearer token, and a bad cookie gets 401")
    void unauthenticatedAccess() throws Exception {
        // act & assert
        assertThat(refresh("garbage").getResponse().getStatus()).isEqualTo(401);
        mockMvc.perform(post(AUTH + "logout")).andExpect(status().isNoContent());
        mockMvc.perform(post(REFRESH_URI)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CORS allows credentialed calls from the configured origin only, echoing it rather than using a wildcard")
    void corsCredentials() throws Exception {
        // act & assert
        mockMvc.perform(options(REFRESH_URI).header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
        mockMvc.perform(options(REFRESH_URI).header(HttpHeaders.ORIGIN, "http://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
