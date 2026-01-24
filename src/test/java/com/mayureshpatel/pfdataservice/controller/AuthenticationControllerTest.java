package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.dto.AuthenticationRequest;
import com.mayureshpatel.pfdataservice.dto.AuthenticationResponse;
import com.mayureshpatel.pfdataservice.dto.RegistrationRequest;
import com.mayureshpatel.pfdataservice.exception.UserAlreadyExistsException;
import com.mayureshpatel.pfdataservice.security.CustomUserDetailsService;
import com.mayureshpatel.pfdataservice.security.JwtService;
import com.mayureshpatel.pfdataservice.service.AuthenticationService;
import com.mayureshpatel.pfdataservice.service.RegistrationService;
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

    @MockitoBean
    private RegistrationService registrationService;

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

    @Test
    void register_ValidRequest_ShouldReturnCreatedWithToken() throws Exception {
        RegistrationRequest request = RegistrationRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("Password123!")
                .build();
        AuthenticationResponse response = new AuthenticationResponse("jwt-token");

        when(registrationService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void register_InvalidUsername_ShouldReturnBadRequest() throws Exception {
        RegistrationRequest request = RegistrationRequest.builder()
                .username("ab") // Too short
                .email("newuser@example.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_InvalidEmail_ShouldReturnBadRequest() throws Exception {
        RegistrationRequest request = RegistrationRequest.builder()
                .username("newuser")
                .email("invalid-email") // Invalid format
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_InvalidPassword_ShouldReturnBadRequest() throws Exception {
        RegistrationRequest request = RegistrationRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("weak") // Too short and missing complexity
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_DuplicateUsername_ShouldReturnConflict() throws Exception {
        RegistrationRequest request = RegistrationRequest.builder()
                .username("existinguser")
                .email("newuser@example.com")
                .password("Password123!")
                .build();

        when(registrationService.register(any())).thenThrow(new UserAlreadyExistsException("Username already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_DuplicateEmail_ShouldReturnConflict() throws Exception {
        RegistrationRequest request = RegistrationRequest.builder()
                .username("newuser")
                .email("existing@example.com")
                .password("Password123!")
                .build();

        when(registrationService.register(any())).thenThrow(new UserAlreadyExistsException("Email already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
