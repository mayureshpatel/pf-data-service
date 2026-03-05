//package com.mayureshpatel.pfdataservice.service;
//
//import com.mayureshpatel.pfdataservice.domain.user.User;
//import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationRequest;
//import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationResponse;
//import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
//import com.mayureshpatel.pfdataservice.security.JwtService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//
//import java.util.Map;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("AuthenticationService unit tests")
//class AuthenticationServiceTest {
//
//    @Mock
//    private AuthenticationManager authenticationManager;
//
//    @Mock
//    private UserDetailsService userDetailsService;
//
//    @Mock
//    private JwtService jwtService;
//
//    @InjectMocks
//    private AuthenticationService authenticationService;
//
//    private static final String USERNAME = "john_doe";
//    private static final String PASSWORD = "S3cur3P@ss!";
//    private static final String JWT_TOKEN = "eyJhbGciOiJIUzI1NiJ9.test.token";
//    private static final Long USER_ID = 42L;
//    private static final String EMAIL = "john@example.com";
//
//    private AuthenticationRequest buildRequest(String username, String password) {
//        return AuthenticationRequest.builder()
//                .username(username)
//                .password(password)
//                .build();
//    }
//
//    private CustomUserDetails buildCustomUserDetails(Long id, String username, String email) {
//        User user = new User();
//        user.setId(id);
//        user.setUsername(username);
//        user.setEmail(email);
//        user.setPasswordHash("$2a$10$hashedpassword");
//        return new CustomUserDetails(user);
//    }
//
//    @Nested
//    @DisplayName("authenticate() — happy path with CustomUserDetails")
//    class AuthenticateWithCustomUserDetailsTest {
//
//        @Test
//        @DisplayName("should call authenticationManager with correct UsernamePasswordAuthenticationToken")
//        void authenticate_customUserDetails_passesCorrectTokenToAuthenticationManager() {
//            AuthenticationRequest request = buildRequest(USERNAME, PASSWORD);
//            CustomUserDetails userDetails = buildCustomUserDetails(USER_ID, USERNAME, EMAIL);
//
//            when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
//            when(jwtService.generateToken(any(), eq(userDetails))).thenReturn(JWT_TOKEN);
//
//            authenticationService.authenticate(request);
//
//            ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
//            verify(authenticationManager).authenticate(authCaptor.capture());
//
//            Authentication captured = authCaptor.getValue();
//            assertThat(captured).isInstanceOf(UsernamePasswordAuthenticationToken.class);
//            assertThat(captured.getPrincipal()).isEqualTo(USERNAME);
//            assertThat(captured.getCredentials()).isEqualTo(PASSWORD);
//        }
//
//        @Test
//        @DisplayName("should load user details using the username from the request")
//        void authenticate_customUserDetails_loadsUserByCorrectUsername() {
//            AuthenticationRequest request = buildRequest(USERNAME, PASSWORD);
//            CustomUserDetails userDetails = buildCustomUserDetails(USER_ID, USERNAME, EMAIL);
//
//            when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
//            when(jwtService.generateToken(any(), eq(userDetails))).thenReturn(JWT_TOKEN);
//
//            authenticationService.authenticate(request);
//
//            verify(userDetailsService).loadUserByUsername(USERNAME);
//        }
//
//        @Test
//        @DisplayName("should include userId and email as extra claims when UserDetails is CustomUserDetails")
//        void authenticate_customUserDetails_addsUserIdAndEmailToExtraClaims() {
//            AuthenticationRequest request = buildRequest(USERNAME, PASSWORD);
//            CustomUserDetails userDetails = buildCustomUserDetails(USER_ID, USERNAME, EMAIL);
//
//            when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
//            when(jwtService.generateToken(any(), eq(userDetails))).thenReturn(JWT_TOKEN);
//
//            authenticationService.authenticate(request);
//
//            @SuppressWarnings("unchecked")
//            ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
//            verify(jwtService).generateToken(claimsCaptor.capture(), eq(userDetails));
//
//            Map<String, Object> capturedClaims = claimsCaptor.getValue();
//            assertThat(capturedClaims).containsEntry("userId", USER_ID);
//            assertThat(capturedClaims).containsEntry("email", EMAIL);
//            assertThat(capturedClaims).hasSize(2);
//        }
//
//        @Test
//        @DisplayName("should return AuthenticationResponse containing the token produced by JwtService")
//        void authenticate_customUserDetails_returnsResponseWithCorrectToken() {
//            AuthenticationRequest request = buildRequest(USERNAME, PASSWORD);
//            CustomUserDetails userDetails = buildCustomUserDetails(USER_ID, USERNAME, EMAIL);
//
//            when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
//            when(jwtService.generateToken(any(), eq(userDetails))).thenReturn(JWT_TOKEN);
//
//            AuthenticationResponse response = authenticationService.authenticate(request);
//
//            assertThat(response).isNotNull();
//            assertThat(response.token()).isEqualTo(JWT_TOKEN);
//        }
//
//        @Test
//        @DisplayName("should populate userId claim with exact Long value from CustomUserDetails")
//        void authenticate_customUserDetails_userIdClaimMatchesExactValue() {
//            final Long expectedUserId = 999L;
//            AuthenticationRequest request = buildRequest(USERNAME, PASSWORD);
//            CustomUserDetails userDetails = buildCustomUserDetails(expectedUserId, USERNAME, EMAIL);
//
//            when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
//            when(jwtService.generateToken(any(), eq(userDetails))).thenReturn(JWT_TOKEN);
//
//            authenticationService.authenticate(request);
//
//            @SuppressWarnings("unchecked")
//            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
//            verify(jwtService).generateToken(captor.capture(), eq(userDetails));
//
//            assertThat(captor.getValue().get("userId")).isEqualTo(expectedUserId);
//        }
//
//        @Test
//        @DisplayName("should populate email claim with exact email string from CustomUserDetails")
//        void authenticate_customUserDetails_emailClaimMatchesExactValue() {
//            final String expectedEmail = "specific.address@domain.org";
//            AuthenticationRequest request = buildRequest(USERNAME, PASSWORD);
//            CustomUserDetails userDetails = buildCustomUserDetails(USER_ID, USERNAME, expectedEmail);
//
//            when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
//            when(jwtService.generateToken(any(), eq(userDetails))).thenReturn(JWT_TOKEN);
//
//            authenticationService.authenticate(request);
//
//            @SuppressWarnings("unchecked")
//            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
//            verify(jwtService).generateToken(captor.capture(), eq(userDetails));
//
//            assertThat(captor.getValue().get("email")).isEqualTo(expectedEmail);
//        }
//    }
//
//    @Nested
//    @DisplayName("authenticate() — UserDetails is NOT a CustomUserDetails instance")
//    class AuthenticateWithPlainUserDetailsTest {
//
//        @Test
//        @DisplayName("should pass empty extra-claims map to JwtService when UserDetails is not CustomUserDetails")
//        void authenticate_plainUserDetails_passesEmptyExtraClaimsToJwtService() {
//            AuthenticationRequest request = buildRequest(USERNAME, PASSWORD);
//            UserDetails plainUser = org.springframework.security.core.userdetails.User
//                    .withUsername(USERNAME)
//                    .password(PASSWORD)
//                    .roles("USER")
//                    .build();
//
//            when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(plainUser);
//            when(jwtService.generateToken(any(), eq(plainUser))).thenReturn(JWT_TOKEN);
//
//            authenticationService.authenticate(request);
//
//            @SuppressWarnings("unchecked")
//            ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
//            verify(jwtService).generateToken(claimsCaptor.capture(), eq(plainUser));
//
//            assertThat(claimsCaptor.getValue()).isEmpty();
//        }
//
//        @Test
//        @DisplayName("should return AuthenticationResponse containing the token when UserDetails is not CustomUserDetails")
//        void authenticate_plainUserDetails_returnsResponseWithCorrectToken() {
//            AuthenticationRequest request = buildRequest(USERNAME, PASSWORD);
//            UserDetails plainUser = org.springframework.security.core.userdetails.User
//                    .withUsername(USERNAME)
//                    .password(PASSWORD)
//                    .roles("USER")
//                    .build();
//
//            when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(plainUser);
//            when(jwtService.generateToken(any(), eq(plainUser))).thenReturn(JWT_TOKEN);
//
//            AuthenticationResponse response = authenticationService.authenticate(request);
//
//            assertThat(response).isNotNull();
//            assertThat(response.token()).isEqualTo(JWT_TOKEN);
//        }
//    }
//
//    @Nested
//    @DisplayName("authenticate() — AuthenticationManager failure")
//    class AuthenticateManagerFailureTest {
//
//        @Test
//        @DisplayName("should propagate BadCredentialsException when AuthenticationManager rejects credentials")
//        void authenticate_badCredentials_throwsBadCredentialsException() {
//            AuthenticationRequest request = buildRequest(USERNAME, "wr0ngP@ss!");
//
//            when(authenticationManager.authenticate(any(Authentication.class)))
//                    .thenThrow(new BadCredentialsException("Bad credentials"));
//
//            assertThatThrownBy(() -> authenticationService.authenticate(request))
//                    .isInstanceOf(BadCredentialsException.class)
//                    .hasMessageContaining("Bad credentials");
//
//            verify(userDetailsService, never()).loadUserByUsername(any());
//            verify(jwtService, never()).generateToken(any(), any(UserDetails.class));
//        }
//
//        @Test
//        @DisplayName("should not load user details or generate a token when authentication manager throws")
//        void authenticate_authenticationManagerThrows_noDownstreamCallsOccur() {
//            AuthenticationRequest request = buildRequest(USERNAME, PASSWORD);
//
//            when(authenticationManager.authenticate(any(Authentication.class)))
//                    .thenThrow(new BadCredentialsException("Locked account"));
//
//            assertThatThrownBy(() -> authenticationService.authenticate(request))
//                    .isInstanceOf(BadCredentialsException.class);
//
//            verify(userDetailsService, never()).loadUserByUsername(any());
//            verify(jwtService, never()).generateToken(any(), any(UserDetails.class));
//        }
//    }
//
//    @Nested
//    @DisplayName("authenticate() — UserDetailsService failure")
//    class AuthenticateUserDetailsServiceFailureTest {
//
//        @Test
//        @DisplayName("should propagate UsernameNotFoundException when user cannot be found")
//        void authenticate_userNotFound_throwsUsernameNotFoundException() {
//            AuthenticationRequest request = buildRequest(USERNAME, PASSWORD);
//
//            when(userDetailsService.loadUserByUsername(USERNAME))
//                    .thenThrow(new UsernameNotFoundException("User not found: " + USERNAME));
//
//            assertThatThrownBy(() -> authenticationService.authenticate(request))
//                    .isInstanceOf(UsernameNotFoundException.class)
//                    .hasMessageContaining(USERNAME);
//
//            verify(jwtService, never()).generateToken(any(), any(UserDetails.class));
//        }
//
//        @Test
//        @DisplayName("should not generate a token when UserDetailsService throws UsernameNotFoundException")
//        void authenticate_userDetailsServiceThrows_jwtServiceNotCalled() {
//            AuthenticationRequest request = buildRequest("ghost_user", PASSWORD);
//
//            when(userDetailsService.loadUserByUsername("ghost_user"))
//                    .thenThrow(new UsernameNotFoundException("User not found: ghost_user"));
//
//            assertThatThrownBy(() -> authenticationService.authenticate(request))
//                    .isInstanceOf(UsernameNotFoundException.class);
//
//            verify(jwtService, never()).generateToken(any(), any(UserDetails.class));
//        }
//    }
//}
