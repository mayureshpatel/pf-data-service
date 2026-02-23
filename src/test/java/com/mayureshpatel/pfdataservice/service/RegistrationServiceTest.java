package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationResponse;
import com.mayureshpatel.pfdataservice.dto.user.RegistrationRequest;
import com.mayureshpatel.pfdataservice.exception.UserAlreadyExistsException;
import com.mayureshpatel.pfdataservice.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationService unit tests")
class RegistrationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RegistrationService registrationService;

    private static final String USERNAME = "new_user";
    private static final String EMAIL = "new_user@example.com";
    private static final String RAW_PASSWORD = "Str0ng@Pass";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedHash";
    private static final String JWT_TOKEN = "eyJhbGciOiJIUzI1NiJ9.reg.token";
    private static final Long SAVED_USER_ID = 7L;

    private RegistrationRequest buildRequest(String username, String email, String password) {
        return RegistrationRequest.builder()
                .username(username)
                .email(email)
                .password(password)
                .build();
    }

    private User buildSavedUser(Long id, String username, String email, String encodedPassword) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(encodedPassword);
        return user;
    }

    @Nested
    @DisplayName("register() — happy path")
    class RegisterHappyPathTest {

        @Test
        @DisplayName("should return AuthenticationResponse containing the JWT token on successful registration")
        void register_newUsernameAndEmail_returnsResponseWithToken() {
            RegistrationRequest request = buildRequest(USERNAME, EMAIL, RAW_PASSWORD);
            User savedUser = buildSavedUser(SAVED_USER_ID, USERNAME, EMAIL, ENCODED_PASSWORD);

            when(userService.isUserExistsByUsername(USERNAME)).thenReturn(false);
            when(userService.isUserExistsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userService.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any(), any())).thenReturn(JWT_TOKEN);

            AuthenticationResponse response = registrationService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo(JWT_TOKEN);
        }

        @Test
        @DisplayName("should save a User entity with username, email, and encoded password")
        void register_newUser_savesEntityWithCorrectFields() {
            RegistrationRequest request = buildRequest(USERNAME, EMAIL, RAW_PASSWORD);
            User savedUser = buildSavedUser(SAVED_USER_ID, USERNAME, EMAIL, ENCODED_PASSWORD);

            when(userService.isUserExistsByUsername(USERNAME)).thenReturn(false);
            when(userService.isUserExistsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userService.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any(), any())).thenReturn(JWT_TOKEN);

            registrationService.register(request);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userService).save(userCaptor.capture());

            User captured = userCaptor.getValue();
            assertThat(captured.getUsername()).isEqualTo(USERNAME);
            assertThat(captured.getEmail()).isEqualTo(EMAIL);
            assertThat(captured.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("should encode the raw password and never store it in plain text")
        void register_newUser_encodesPasswordAndNeverStoresRawPassword() {
            RegistrationRequest request = buildRequest(USERNAME, EMAIL, RAW_PASSWORD);
            User savedUser = buildSavedUser(SAVED_USER_ID, USERNAME, EMAIL, ENCODED_PASSWORD);

            when(userService.isUserExistsByUsername(USERNAME)).thenReturn(false);
            when(userService.isUserExistsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userService.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any(), any())).thenReturn(JWT_TOKEN);

            registrationService.register(request);

            verify(passwordEncoder).encode(RAW_PASSWORD);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userService).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPasswordHash())
                    .isEqualTo(ENCODED_PASSWORD)
                    .isNotEqualTo(RAW_PASSWORD);
        }

        @Test
        @DisplayName("should include userId and email as extra claims in the JWT")
        void register_newUser_addsUserIdAndEmailToJwtClaims() {
            RegistrationRequest request = buildRequest(USERNAME, EMAIL, RAW_PASSWORD);
            User savedUser = buildSavedUser(SAVED_USER_ID, USERNAME, EMAIL, ENCODED_PASSWORD);

            when(userService.isUserExistsByUsername(USERNAME)).thenReturn(false);
            when(userService.isUserExistsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userService.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any(), any())).thenReturn(JWT_TOKEN);

            registrationService.register(request);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(jwtService).generateToken(claimsCaptor.capture(), any());

            Map<String, Object> capturedClaims = claimsCaptor.getValue();
            assertThat(capturedClaims).containsEntry("userId", SAVED_USER_ID);
            assertThat(capturedClaims).containsEntry("email", EMAIL);
            assertThat(capturedClaims).hasSize(2);
        }

        @Test
        @DisplayName("should use the saved user's id as the userId claim in the JWT")
        void register_newUser_userIdClaimMatchesSavedEntityId() {
            final Long distinctId = 555L;
            RegistrationRequest request = buildRequest(USERNAME, EMAIL, RAW_PASSWORD);
            User savedUser = buildSavedUser(distinctId, USERNAME, EMAIL, ENCODED_PASSWORD);

            when(userService.isUserExistsByUsername(USERNAME)).thenReturn(false);
            when(userService.isUserExistsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userService.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any(), any())).thenReturn(JWT_TOKEN);

            registrationService.register(request);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            verify(jwtService).generateToken(captor.capture(), any());

            assertThat(captor.getValue().get("userId")).isEqualTo(distinctId);
        }
    }

    @Nested
    @DisplayName("register() — username already exists")
    class RegisterUsernameAlreadyExistsTest {

        @Test
        @DisplayName("should throw UserAlreadyExistsException when username is already taken")
        void register_usernameExists_throwsUserAlreadyExistsException() {
            RegistrationRequest request = buildRequest(USERNAME, EMAIL, RAW_PASSWORD);

            when(userService.isUserExistsByUsername(USERNAME)).thenReturn(true);

            assertThatThrownBy(() -> registrationService.register(request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Username already exists");
        }

        @Test
        @DisplayName("should never check email when username is already taken")
        void register_usernameExists_emailCheckSkipped() {
            RegistrationRequest request = buildRequest(USERNAME, EMAIL, RAW_PASSWORD);

            when(userService.isUserExistsByUsername(USERNAME)).thenReturn(true);

            assertThatThrownBy(() -> registrationService.register(request))
                    .isInstanceOf(UserAlreadyExistsException.class);

            verify(userService, never()).isUserExistsByEmail(anyString());
        }

        @Test
        @DisplayName("should never encode password or save user when username is already taken")
        void register_usernameExists_noSaveOrEncodeOccurs() {
            RegistrationRequest request = buildRequest(USERNAME, EMAIL, RAW_PASSWORD);

            when(userService.isUserExistsByUsername(USERNAME)).thenReturn(true);

            assertThatThrownBy(() -> registrationService.register(request))
                    .isInstanceOf(UserAlreadyExistsException.class);

            verify(passwordEncoder, never()).encode(anyString());
            verify(userService, never()).save(any(User.class));
            verify(jwtService, never()).generateToken(any(), any());
        }
    }

    @Nested
    @DisplayName("register() — email already exists")
    class RegisterEmailAlreadyExistsTest {

        @Test
        @DisplayName("should throw UserAlreadyExistsException when email is already registered")
        void register_emailExists_throwsUserAlreadyExistsException() {
            RegistrationRequest request = buildRequest(USERNAME, EMAIL, RAW_PASSWORD);

            when(userService.isUserExistsByUsername(USERNAME)).thenReturn(false);
            when(userService.isUserExistsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> registrationService.register(request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Email already exists");
        }

        @Test
        @DisplayName("should never encode password or save user when email is already registered")
        void register_emailExists_noSaveOrEncodeOccurs() {
            RegistrationRequest request = buildRequest(USERNAME, EMAIL, RAW_PASSWORD);

            when(userService.isUserExistsByUsername(USERNAME)).thenReturn(false);
            when(userService.isUserExistsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> registrationService.register(request))
                    .isInstanceOf(UserAlreadyExistsException.class);

            verify(passwordEncoder, never()).encode(anyString());
            verify(userService, never()).save(any(User.class));
            verify(jwtService, never()).generateToken(any(), any());
        }

        @Test
        @DisplayName("should check email only after confirming username is available")
        void register_emailExists_usernameCheckedBeforeEmail() {
            RegistrationRequest request = buildRequest(USERNAME, EMAIL, RAW_PASSWORD);

            when(userService.isUserExistsByUsername(USERNAME)).thenReturn(false);
            when(userService.isUserExistsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> registrationService.register(request))
                    .isInstanceOf(UserAlreadyExistsException.class);

            verify(userService).isUserExistsByUsername(USERNAME);
            verify(userService).isUserExistsByEmail(EMAIL);
        }
    }

    @Nested
    @DisplayName("register() — both username and email already exist")
    class RegisterBothExistTest {

        @Test
        @DisplayName("should throw username exception first when both username and email exist")
        void register_bothExist_usernameExceptionThrownFirst() {
            RegistrationRequest request = buildRequest(USERNAME, EMAIL, RAW_PASSWORD);

            when(userService.isUserExistsByUsername(USERNAME)).thenReturn(true);

            assertThatThrownBy(() -> registrationService.register(request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Username already exists");

            verify(userService, never()).isUserExistsByEmail(anyString());
        }
    }
}
