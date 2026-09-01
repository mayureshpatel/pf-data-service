package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationRequest;
import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationResponse;
import com.mayureshpatel.pfdataservice.dto.user.RegistrationRequest;
import com.mayureshpatel.pfdataservice.service.AuthenticationService;
import com.mayureshpatel.pfdataservice.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login and user-registration endpoints. Registration is admin-only ({@code #register} requires
 * {@code ROLE_ADMIN}) -- there is no public self-service sign-up flow.
 */
@Tag(name = "Authentication", description = "Login and admin-only user registration")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;
    private final RegistrationService registrationService;

    /**
     * Authenticates a username/password pair and issues a JWT.
     *
     * @param request the login credentials
     * @return the issued token
     */
    @Operation(summary = "Authenticate", description = "Authenticates a username/password pair and issues a JWT")
    @ApiResponse(responseCode = "200", description = "Authenticated, token returned")
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    /**
     * Registers a new user. Restricted to administrators -- there is no self-service registration.
     *
     * @param request the new user's details
     * @return 201 with a token for the newly created user
     */
    @Operation(summary = "Register a user", description = "Creates a new user account. Admin-only.")
    @ApiResponse(responseCode = "201", description = "User created, token returned")
    @ApiResponse(responseCode = "403", description = "Forbidden -- caller is not an admin")
    @ApiResponse(responseCode = "409", description = "Username or email already exists")
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegistrationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
    }
}
