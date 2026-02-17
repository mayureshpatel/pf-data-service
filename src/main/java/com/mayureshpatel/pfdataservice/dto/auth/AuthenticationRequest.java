package com.mayureshpatel.pfdataservice.dto.auth;

import lombok.Builder;

/**
 * Represents an authentication request.
 * @param username the username
 * @param password the password
 */
@Builder
public record AuthenticationRequest(String username, String password) {
}
