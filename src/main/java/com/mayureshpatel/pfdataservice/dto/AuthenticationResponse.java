package com.mayureshpatel.pfdataservice.dto;

import lombok.Builder;

/**
 * Represents an authentication response.
 * @param token the authentication token
 */
@Builder
public record AuthenticationResponse(String token) {
}
