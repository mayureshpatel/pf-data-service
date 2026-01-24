package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.dto.AuthenticationRequest;
import com.mayureshpatel.pfdataservice.dto.AuthenticationResponse;
import com.mayureshpatel.pfdataservice.security.CustomUserDetailsService;
import com.mayureshpatel.pfdataservice.security.JwtService;
import com.mayureshpatel.pfdataservice.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    // Security Mocks required for context load
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private com.mayureshpatel.pfdataservice.repository.UserRepository userRepository;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public CustomUserDetailsService customUserDetailsService() {
            return mock(CustomUserDetailsService.class);
        }
    }

    @Test
    void authenticate_ShouldReturnToken() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest("user", "password");
        AuthenticationResponse response = new AuthenticationResponse("jwt-token");

        when(authenticationService.authenticate(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }
}
