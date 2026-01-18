package com.mayureshpatel.pfdataservice.dto;

import lombok.Builder;

@Builder
public record AuthenticationResponse(String token) {
}
