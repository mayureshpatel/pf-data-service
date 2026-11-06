package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationRequest;
import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationResponse;
import com.mayureshpatel.pfdataservice.dto.user.RegistrationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.mayureshpatel.pfdataservice.service.AuthenticationResult;
import com.mayureshpatel.pfdataservice.service.IssuedRefreshToken;
import jakarta.servlet.http.Cookie;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link AuthenticationController}.
 * Follows the Gold Standard for controller testing.
 */
@DisplayName("AuthenticationController Unit Tests")
class AuthenticationControllerTest extends BaseControllerTest {

    private static final String AUTH_TOKEN = "mocked-jwt-token";
    private static final String RAW_REFRESH = "raw-refresh-token";
    private static final String AUTHENTICATE_URI = "/api/v1/auth/authenticate";
    private static final String REGISTER_URI = "/api/v1/auth/register";
    private static final String VALID_PASSWORD = "Pass123!@";
    private static final String NEW_USERNAME = "new_user";
    private static final String NEW_EMAIL = "new@example.com";
    private static final String PATH_ATTRIBUTE = "Path=/api/v1/auth";
    private static final String HTTP_ONLY = "HttpOnly";
    private static final String MAX_AGE_ZERO = "Max-Age=0";
    private static final String REFRESH_URI = "/api/v1/auth/refresh";
    private static final String LOGOUT_URI = "/api/v1/auth/logout";
    private static final String COOKIE_NAME = "pf_refresh";

    /** {@code POST /authenticate} returns the issued token on valid credentials, and a 400 with field-level {@code validationErrors} on a missing username. */
    @Nested
    @DisplayName("authenticate")
    class AuthenticateTests {

        @Test
        @DisplayName("POST /authenticate should return token on valid credentials")
        void authenticate_shouldReturnToken() throws Exception {
            // arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .username("john_doe")
                    .password("secure_password")
                    .build();

            AuthenticationResponse response = AuthenticationResponse.builder()
                    .token(AUTH_TOKEN)
                    .build();

            // Note: BaseControllerTest has authenticationService mocked as 'authenticationService'
            when(authenticationService.authenticate(any(AuthenticationRequest.class)))
                    .thenReturn(new AuthenticationResult(response, new IssuedRefreshToken(RAW_REFRESH, false, null)));

            // act & assert
            mockMvc.perform(post(AUTHENTICATE_URI)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.token").value(AUTH_TOKEN));

            verify(authenticationService).authenticate(any(AuthenticationRequest.class));
        }

        @Test
        @DisplayName("POST /authenticate should return 400 Bad Request when validation fails (missing username)")
        void authenticate_shouldReturn400WhenValidationFails() throws Exception {
            // arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .password("secure_password")
                    .build();

            // act & assert
            mockMvc.perform(post(AUTHENTICATE_URI)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[0].field").value("username"));
        }
    }

    /** {@code POST /register} succeeds for a genuinely anonymous, unauthenticated caller (PF-183 -- this endpoint deliberately has no principal to authorize against), returns a 400 with field-level {@code validationErrors} on an invalid email, and returns a 400 (not a 201) when the service rejects the request for the anti-bot honeypot field being filled. */
    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("POST /register should succeed for an anonymous, unauthenticated caller (PF-183)")
        void register_anonymousCallerSucceeds() throws Exception {
            // arrange -- no @WithCustomMockUser here: this is the whole point of PF-183, an
            // anonymous caller with no principal at all must be able to register.
            RegistrationRequest request = RegistrationRequest.builder()
                    .username(NEW_USERNAME)
                    .email(NEW_EMAIL)
                    .password(VALID_PASSWORD)
                    .build();

            AuthenticationResponse response = AuthenticationResponse.builder()
                    .token(AUTH_TOKEN)
                    .build();

            when(registrationService.register(any(RegistrationRequest.class)))
                    .thenReturn(new AuthenticationResult(response, new IssuedRefreshToken(RAW_REFRESH, false, null)));

            // act & assert
            mockMvc.perform(post(REGISTER_URI)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.token").value(AUTH_TOKEN));

            verify(registrationService).register(any(RegistrationRequest.class));
        }

        @Test
        @DisplayName("POST /register should return 400 Bad Request on invalid email")
        void register_shouldReturn400OnInvalidEmail() throws Exception {
            // arrange
            RegistrationRequest request = RegistrationRequest.builder()
                    .username(NEW_USERNAME)
                    .email("invalid-email")
                    .password(VALID_PASSWORD)
                    .build();

            // act & assert
            mockMvc.perform(post(REGISTER_URI)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[0].field").value("email"));
        }

        @Test
        @DisplayName("POST /register should return 400 Bad Request when the honeypot field is filled")
        void register_shouldReturn400OnHoneypotFilled() throws Exception {
            // arrange
            RegistrationRequest request = RegistrationRequest.builder()
                    .username(NEW_USERNAME)
                    .email(NEW_EMAIL)
                    .password(VALID_PASSWORD)
                    .website("http://spam.example.com")
                    .build();

            when(registrationService.register(any(RegistrationRequest.class)))
                    .thenThrow(new IllegalArgumentException("Registration failed. Please try again."));

            // act & assert
            mockMvc.perform(post(REGISTER_URI)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Registration failed. Please try again."));
        }
    }

    /** The refresh cookie set by {@code /authenticate} and {@code /register}: attributes follow the defaults (Secure, Strict), Max-Age appears only for remember-me, and the raw token never reaches the JSON body. */
    @Nested
    @DisplayName("refresh cookie on sign-in")
    class SignInCookieTests {

        private String login(IssuedRefreshToken issued) throws Exception {
            when(authenticationService.authenticate(any(AuthenticationRequest.class))).thenReturn(
                    new AuthenticationResult(AuthenticationResponse.builder().token(AUTH_TOKEN).build(), issued));
            return mockMvc.perform(post(AUTHENTICATE_URI)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"john\",\"password\":\"pw\",\"rememberMe\":true}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(RAW_REFRESH))))
                    .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        }

        @Test
        @DisplayName("remember-me login sets an HttpOnly, Secure, Strict cookie on the auth path with Max-Age")
        void rememberMeCookie() throws Exception {
            // act
            String cookie = login(new IssuedRefreshToken(RAW_REFRESH, true, 1209600L));

            // assert & verify
            assertThat(cookie).startsWith(COOKIE_NAME + "=" + RAW_REFRESH)
                    .contains(HTTP_ONLY, "Secure", "SameSite=Strict", PATH_ATTRIBUTE, "Max-Age=1209600");
        }

        @Test
        @DisplayName("session-only login sets a cookie without Max-Age")
        void sessionCookie() throws Exception {
            // act
            String cookie = login(new IssuedRefreshToken(RAW_REFRESH, false, null));

            // assert & verify
            assertThat(cookie).contains(HTTP_ONLY, PATH_ATTRIBUTE).doesNotContain("Max-Age", "Expires");
        }

        @Test
        @DisplayName("register sets the session cookie and keeps the token out of the body")
        void registerCookie() throws Exception {
            // arrange
            when(registrationService.register(any(RegistrationRequest.class))).thenReturn(new AuthenticationResult(
                    AuthenticationResponse.builder().token(AUTH_TOKEN).build(), new IssuedRefreshToken(RAW_REFRESH, false, null)));
            RegistrationRequest request = RegistrationRequest.builder()
                    .username(NEW_USERNAME).email(NEW_EMAIL).password(VALID_PASSWORD).build();

            // act
            String cookie = mockMvc.perform(post(REGISTER_URI)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(RAW_REFRESH))))
                    .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);

            // assert & verify
            assertThat(cookie).startsWith(COOKIE_NAME + "=" + RAW_REFRESH).contains(HTTP_ONLY).doesNotContain("Max-Age");
        }
    }

    /** {@code POST /refresh} authenticates by the cookie alone (no Bearer, no CSRF token): 200 with a new token and a re-sent cookie, or 401 with the standard error body and an expired cookie. */
    @Nested
    @DisplayName("refresh")
    class RefreshTests {

        @Test
        @DisplayName("should return a new access token and re-send the cookie with the slid Max-Age")
        void refresh_ok() throws Exception {
            // arrange
            when(authenticationService.refresh(RAW_REFRESH)).thenReturn(Optional.of(new AuthenticationResult(
                    AuthenticationResponse.builder().token(AUTH_TOKEN).build(), new IssuedRefreshToken(RAW_REFRESH, true, 1200000L))));

            // act & assert
            mockMvc.perform(post(REFRESH_URI).cookie(new Cookie(COOKIE_NAME, RAW_REFRESH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(AUTH_TOKEN))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                            org.hamcrest.Matchers.startsWith(COOKIE_NAME + "=" + RAW_REFRESH),
                            org.hamcrest.Matchers.containsString("Max-Age=1200000"),
                            org.hamcrest.Matchers.containsString(HTTP_ONLY))));
        }

        @Test
        @DisplayName("should return 401 with the error body and clear the cookie when the token is rejected")
        void refresh_rejected() throws Exception {
            // arrange
            when(authenticationService.refresh(RAW_REFRESH)).thenReturn(Optional.empty());

            // act & assert
            mockMvc.perform(post(REFRESH_URI).cookie(new Cookie(COOKIE_NAME, RAW_REFRESH)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.instance").value(REFRESH_URI))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                            org.hamcrest.Matchers.startsWith(COOKIE_NAME + "=;"),
                            org.hamcrest.Matchers.containsString(MAX_AGE_ZERO),
                            org.hamcrest.Matchers.containsString(PATH_ATTRIBUTE))));
        }

        @Test
        @DisplayName("should return 401 and clear the cookie when no cookie is sent")
        void refresh_noCookie() throws Exception {
            // arrange
            when(authenticationService.refresh(null)).thenReturn(Optional.empty());

            // act & assert
            mockMvc.perform(post(REFRESH_URI))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().exists(HttpHeaders.SET_COOKIE));
        }

        @Test
        @DisplayName("should ignore an expired Bearer token: the cookie is the only credential")
        void refresh_ignoresExpiredBearer() throws Exception {
            // arrange
            when(authenticationService.refresh(RAW_REFRESH)).thenReturn(Optional.of(new AuthenticationResult(
                    AuthenticationResponse.builder().token(AUTH_TOKEN).build(), new IssuedRefreshToken(RAW_REFRESH, false, null))));
            when(jwtService.extractUsername("expired")).thenThrow(new io.jsonwebtoken.ExpiredJwtException(null, null, "expired"));

            // act & assert
            mockMvc.perform(post(REFRESH_URI)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer expired")
                            .cookie(new Cookie(COOKIE_NAME, RAW_REFRESH)))
                    .andExpect(status().isOk());
        }
    }

    /** {@code POST /logout} is always 204 and always clears the cookie; it revokes only when a cookie was presented. */
    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("should revoke the presented token and clear the cookie")
        void logout_withCookie() throws Exception {
            // act & assert
            mockMvc.perform(post(LOGOUT_URI).cookie(new Cookie(COOKIE_NAME, RAW_REFRESH)))
                    .andExpect(status().isNoContent())
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString(MAX_AGE_ZERO)));

            verify(authenticationService).logout(RAW_REFRESH);
        }

        @Test
        @DisplayName("should still return 204 and clear the cookie when no cookie is sent")
        void logout_noCookie() throws Exception {
            // act & assert
            mockMvc.perform(post(LOGOUT_URI))
                    .andExpect(status().isNoContent())
                    .andExpect(header().exists(HttpHeaders.SET_COOKIE));

            verify(authenticationService).logout(null);
        }
    }

    /** An unexpected exception from the authentication service surfaces as a generic 500 via {@code GlobalExceptionHandler}'s catch-all, not the underlying exception's own message. */
    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("POST /authenticate should return 500 when service fails unexpectedly")
        void authenticate_shouldReturn500WhenServiceFails() throws Exception {
            // arrange
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .username("john_doe")
                    .password("secure_password")
                    .build();

            when(authenticationService.authenticate(any(AuthenticationRequest.class)))
                    .thenThrow(new RuntimeException("Authentication backend down"));

            // act & assert
            mockMvc.perform(post(AUTHENTICATE_URI)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.detail").value("An unexpected internal error occurred. Please contact support."));
        }
    }
}
