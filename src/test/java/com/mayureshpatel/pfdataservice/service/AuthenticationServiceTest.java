package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.AuthenticationRequest;
import com.mayureshpatel.pfdataservice.dto.AuthenticationResponse;
import com.mayureshpatel.pfdataservice.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void authenticate_ShouldReturnToken() {
        AuthenticationRequest request = new AuthenticationRequest("user", "password");
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("user")
                .password("password")
                .authorities("ROLE_USER")
                .build();

        when(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails);
        when(jwtService.generateToken(anyMap(), any(UserDetails.class))).thenReturn("jwt-token");

        AuthenticationResponse response = authenticationService.authenticate(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
