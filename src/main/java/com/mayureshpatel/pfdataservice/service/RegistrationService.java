package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.AuthenticationResponse;
import com.mayureshpatel.pfdataservice.dto.RegistrationRequest;
import com.mayureshpatel.pfdataservice.exception.UserAlreadyExistsException;
import com.mayureshpatel.pfdataservice.model.User;
import com.mayureshpatel.pfdataservice.repository.UserRepository;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthenticationResponse register(RegistrationRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setLastUpdatedBy(request.username()); // Self-registration

        // Save user to database
        user = userRepository.save(user);

        // Generate JWT token with userId and email claims
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userDetails.getId());
        extraClaims.put("email", userDetails.getEmail());

        String jwtToken = jwtService.generateToken(extraClaims, userDetails);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}
