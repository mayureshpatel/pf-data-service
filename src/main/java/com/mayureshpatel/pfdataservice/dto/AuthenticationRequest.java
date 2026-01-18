package com.mayureshpatel.pfdataservice.dto;

import lombok.Builder;

@Builder
public record AuthenticationRequest(String username, String password) {
}
