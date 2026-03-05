package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.auth.AuthenticationResponse;
import com.mayureshpatel.pfdataservice.dto.user.RegistrationRequest;
import com.mayureshpatel.pfdataservice.exception.UserAlreadyExistsException;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthenticationResponse register(RegistrationRequest request) {
        // Check if username already exists
        if (userService.isUserExistsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        // Check if email already exists
        if (userService.isUserExistsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        // Save user to database
        int userId = userService.insert(user);

        // Generate JWT token with userId and email claims
        User savedUser = user.toBuilder().id((long) userId).build();
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userDetails.getId());
        extraClaims.put("email", userDetails.getEmail());

        String jwtToken = jwtService.generateToken(extraClaims, userDetails);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}
