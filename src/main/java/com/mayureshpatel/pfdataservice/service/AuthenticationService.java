package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationRequest;
import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationResponse;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Verifies login credentials and issues JWTs plus refresh tokens (PF-899), and renews a session from a refresh token. Distinct from {@link RegistrationService}, which
 * creates the user account this service later authenticates against.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Authenticates the user, generates a JWT access token and issues a refresh token.
     *
     * @param request the authentication request; a null {@code rememberMe} means false
     * @return the access-token response plus the refresh token for the cookie (null only for a principal without a user id)
     */
    public AuthenticationResult authenticate(AuthenticationRequest request) {
        // authenticate the user and load the user details
        this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(request.username());
        AuthenticationResponse response = buildResponse(userDetails);

        // only this codebase's own principal carries the user id a refresh token is bound to
        IssuedRefreshToken refreshToken = null;
        if (userDetails instanceof CustomUserDetails customUser) {
            refreshToken = refreshTokenService.issue(
                    User.builder().id(customUser.getId()).build(), Boolean.TRUE.equals(request.rememberMe()));
        }
        return new AuthenticationResult(response, refreshToken);
    }

    /**
     * Renews a session from the refresh cookie value: validates and slides the refresh token, then builds a
     * new access token for its user exactly as {@link #authenticate} does.
     *
     * @param rawRefreshToken the raw cookie value; may be null or blank
     * @return the new access token and the same refresh token with a slid cookie lifetime, or empty when the
     *         token is missing, unknown, revoked, expired or its user was deleted
     */
    public Optional<AuthenticationResult> refresh(String rawRefreshToken) {
        return refreshTokenService.renew(rawRefreshToken).map(renewed -> {
            AuthenticationResponse response = buildResponse(new CustomUserDetails(renewed.user()));
            return new AuthenticationResult(response,
                    new IssuedRefreshToken(rawRefreshToken, renewed.rememberMe(), renewed.maxAgeSeconds()));
        });
    }

    /**
     * Ends the session on the server. Idempotent: a missing or unknown token is a no-op.
     *
     * @param rawRefreshToken the raw cookie value; may be null or blank
     */
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthenticationResponse buildResponse(UserDetails userDetails) {
        // add custom claims to the token
        Map<String, Object> extraClaims = new HashMap<>();
        if (userDetails instanceof CustomUserDetails customUser) {
            extraClaims.put("userId", customUser.getId());
            extraClaims.put("email", customUser.getEmail());
        }

        // generate the token
        String jwtToken = this.jwtService.generateToken(extraClaims, userDetails);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}
