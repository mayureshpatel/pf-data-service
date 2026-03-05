//package com.mayureshpatel.pfdataservice.controller;
//
//import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationRequest;
//import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationResponse;
//import com.mayureshpatel.pfdataservice.dto.user.RegistrationRequest;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.http.MediaType;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@AutoConfigureMockMvc(addFilters = false) // Bypass security filters for auth endpoints to simplify testing logic
//@DisplayName("AuthenticationController Unit Tests")
//class AuthenticationControllerTest extends BaseControllerTest {
//
//    @Test
//    @DisplayName("POST /api/v1/auth/authenticate should return token")
//    void authenticate_shouldReturnToken() throws Exception {
//        // Arrange
//        AuthenticationRequest request = new AuthenticationRequest("user", "password");
//        AuthenticationResponse response = new AuthenticationResponse("token123");
//
//        when(authenticationService.authenticate(any(AuthenticationRequest.class))).thenReturn(response);
//
//        // Act & Assert
//        mockMvc.perform(post("/api/v1/auth/authenticate")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.token").value("token123"));
//
//        verify(authenticationService).authenticate(any(AuthenticationRequest.class));
//    }
//
//    @Test
//    @DisplayName("POST /api/v1/auth/register should return created token")
//    void register_shouldReturnCreatedToken() throws Exception {
//        // Arrange
//        RegistrationRequest request = new RegistrationRequest("user", "user@example.com", "Password123!");
//        AuthenticationResponse response = new AuthenticationResponse("token123");
//
//        when(registrationService.register(any(RegistrationRequest.class))).thenReturn(response);
//
//        // Act & Assert
//        mockMvc.perform(post("/api/v1/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.token").value("token123"));
//
//        verify(registrationService).register(any(RegistrationRequest.class));
//    }
//
//    @Test
//    @DisplayName("POST /api/v1/auth/register should return 400 when username is blank")
//    void register_blankUsername_returns400() throws Exception {
//        RegistrationRequest request = new RegistrationRequest("", "user@example.com", "P@ssword1");
//
//        mockMvc.perform(post("/api/v1/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest());
//    }
//}
