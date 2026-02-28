package com.mayureshpatel.pfdataservice.dto.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Represents an authentication request.
 *
 * @param username the username
 * @param password the password
 */
@Builder
public record AuthenticationRequest(
        @NotBlank(message = "Username is required")
        @Max(value = 50, message = "Username must be less than 50 characters")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}
