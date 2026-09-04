package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationResponse;
import com.mayureshpatel.pfdataservice.dto.user.RegistrationRequest;
import com.mayureshpatel.pfdataservice.exception.UserAlreadyExistsException;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates new user accounts. This is a public, self-service sign-up flow -- anyone can call it
 * without authentication, guarded by {@link RegistrationRequest}'s honeypot field and
 * {@code RateLimitingFilter}'s per-IP limit on {@code /register} specifically.
 */
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Registers a new user: rejects the request if the honeypot field is filled (an
     * unsophisticated bot, not a real user -- the real form never shows or fills it), then
     * checks the username and email are both unused, hashes the password, persists the user,
     * and returns a JWT for the newly created account.
     *
     * @param request the new user's details
     * @return a token for the newly created user
     * @throws UserAlreadyExistsException if the username or email is already taken
     * @throws IllegalArgumentException   if the honeypot field is non-blank
     */
    @Transactional
    public AuthenticationResponse register(RegistrationRequest request) {
        if (request.getWebsite() != null && !request.getWebsite().isBlank()) {
            throw new IllegalArgumentException("Registration failed. Please try again.");
        }

        // check if username already exists
        if (userService.isUserExistsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        // check if email already exists
        if (userService.isUserExistsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        // create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        // save user to database
        int userId = userService.insert(user);

        // generate jwt token with userId and email claims
        User savedUser = user.toBuilder().id((long) userId).build();
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userDetails.getId());
        extraClaims.put("email", userDetails.getEmail());

        String jwtToken = jwtService.generateToken(extraClaims, userDetails);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}
