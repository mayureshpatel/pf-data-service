package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationResponse;

/**
 * What a sign-in or refresh produces (PF-899): the JSON response for the body and the refresh token
 * for the cookie. Kept separate so the raw refresh token can never be serialized into a body.
 *
 * @param response     the access-token response returned as JSON
 * @param refreshToken the refresh token to set as a cookie; {@code null} when none could be issued
 */
public record AuthenticationResult(AuthenticationResponse response, IssuedRefreshToken refreshToken) {
}
