package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.AuthenticationRequest;
import com.mayureshpatel.pfdataservice.dto.AuthenticationResponse;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());

        Map<String, Object> extraClaims = new HashMap<>();
        if (userDetails instanceof CustomUserDetails customUser) {
            extraClaims.put("userId", customUser.getId());
            extraClaims.put("email", customUser.getEmail());
        }

        var jwtToken = jwtService.generateToken(extraClaims, userDetails);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}
