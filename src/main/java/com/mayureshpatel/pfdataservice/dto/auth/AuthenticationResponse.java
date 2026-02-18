package com.mayureshpatel.pfdataservice.dto.auth;

import lombok.Builder;

/**
 * Represents an authentication response.
 *
 * @param token the authentication token
 */
@Builder
public record AuthenticationResponse(
        String token
) {
}
