package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationRequest;
import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationResponse;
import com.mayureshpatel.pfdataservice.dto.user.RegistrationRequest;
import com.mayureshpatel.pfdataservice.security.RefreshCookieFactory;
import com.mayureshpatel.pfdataservice.service.AuthenticationResult;
import com.mayureshpatel.pfdataservice.service.AuthenticationService;
import com.mayureshpatel.pfdataservice.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Login, registration, session-renewal and logout endpoints (PF-899 added refresh and logout). All are public, unauthenticated endpoints -- self-service
 * registration is open to anyone, guarded by {@code RegistrationRequest}'s honeypot field and a
 * per-IP rate limit on {@code /register} specifically (see {@code RateLimitingFilter}).
 *
 * <p>The refresh token travels only in the {@code HttpOnly} refresh cookie, never in a JSON body. On
 * {@code /refresh} the cookie is the sole credential. CSRF exposure is limited: the cookie is {@code SameSite}
 * and scoped to this path, CSRF protection is off app-wide because every other endpoint uses a Bearer header,
 * the JSON access token in the response is not readable cross-site (explicit CORS origins), and the only
 * state change on {@code /refresh} is sliding the idle expiry.
 */
@Tag(name = "Authentication", description = "Login and self-service user registration")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;
    private final RegistrationService registrationService;
    private final RefreshCookieFactory cookieFactory;

    /**
     * Authenticates a username/password pair, issues a JWT and sets the refresh cookie.
     *
     * @param request the login credentials, with optional {@code rememberMe}
     * @return the issued token in the body, the refresh token only in {@code Set-Cookie}
     */
    @Operation(summary = "Authenticate", description = "Authenticates a username/password pair and issues a JWT")
    @ApiResponse(responseCode = "200", description = "Authenticated, token returned")
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        return withCookie(ResponseEntity.ok(), service.authenticate(request));
    }

    /**
     * Registers a new user. Public and self-service -- no authentication required to call it.
     *
     * @param request the new user's details
     * @return 201 with a token for the newly created user
     */
    @Operation(summary = "Register a user", description = "Creates a new user account. Public, self-service.")
    @ApiResponse(responseCode = "201", description = "User created, token returned")
    @ApiResponse(responseCode = "400", description = "Validation failed, or the honeypot field was filled")
    @ApiResponse(responseCode = "409", description = "Username or email already exists")
    @ApiResponse(responseCode = "429", description = "Too many registration attempts from this IP")
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegistrationRequest request
    ) {
        return withCookie(ResponseEntity.status(HttpStatus.CREATED), registrationService.register(request));
    }

    /**
     * Renews the session from the refresh cookie alone (no Bearer needed): returns a new access token and
     * re-sends the cookie with a slid lifetime. Any failure clears the cookie.
     *
     * @param refreshToken the raw refresh cookie value, absent if the browser sent none
     * @param request      the current request, used for the error body's instance
     * @return 200 with a new access token, or 401 with the standard error body and an expired cookie
     */
    @Operation(summary = "Refresh the session", description = "Exchanges the refresh cookie for a new access token")
    @ApiResponse(responseCode = "200", description = "Session renewed, new token returned")
    @ApiResponse(responseCode = "401", description = "Missing, invalid, expired or revoked refresh cookie, or the user no longer exists")
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(name = "${app.session.refresh.cookie-name:pf_refresh}", required = false) String refreshToken,
            HttpServletRequest request
    ) {
        return service.refresh(refreshToken)
                .<ResponseEntity<?>>map(result -> withCookie(ResponseEntity.ok(), result))
                .orElseGet(() -> {
                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                            HttpStatus.UNAUTHORIZED, "Your session has expired. Please sign in again.");
                    problem.setInstance(URI.create(request.getRequestURI()));
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                            .body(problem);
                });
    }

    /**
     * Ends the session: revokes the presented refresh token (if any) and clears the cookie. Idempotent.
     *
     * @param refreshToken the raw refresh cookie value, absent if the browser sent none
     * @return 204 always
     */
    @Operation(summary = "Sign out", description = "Revokes the refresh token and clears the cookie; always 204")
    @ApiResponse(responseCode = "204", description = "Signed out (also when there was no session)")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${app.session.refresh.cookie-name:pf_refresh}", required = false) String refreshToken
    ) {
        service.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .build();
    }

    /** attaches the refresh cookie (when one was issued) and puts only the access-token response in the body. */
    private ResponseEntity<AuthenticationResponse> withCookie(ResponseEntity.BodyBuilder builder, AuthenticationResult result) {
        if (result.refreshToken() != null) {
            builder.header(HttpHeaders.SET_COOKIE, cookieFactory.create(result.refreshToken()).toString());
        }
        return builder.body(result.response());
    }
}
