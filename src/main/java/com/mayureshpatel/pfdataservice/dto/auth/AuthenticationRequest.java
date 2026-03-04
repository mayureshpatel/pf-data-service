package com.mayureshpatel.pfdataservice.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
        @Size(max = 50, message = "Username must be less than 50 characters")
        String username,

        // todo: do we need to add check for password size?
        // todo: is this the password hash or plain text?
        @NotBlank(message = "Password is required")
        String password
) {
}
