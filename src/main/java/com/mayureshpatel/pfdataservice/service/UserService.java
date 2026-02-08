package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.jdbc.repository.UserRepository;
import com.mayureshpatel.pfdataservice.model.User;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository repository;

    public boolean isUserExistsByUsername(String username) {
        return this.repository.existsByUsername(username);
    }

    public boolean isUserExistsByEmail(String email) {
        return this.repository.existsByEmail(email);
    }

    public User save(User user) {
        return this.repository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return this.repository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return this.repository.findByEmail(email);
    }

    public List<User> findAll() {
        return this.repository.findAll();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new CustomUserDetails(user);
    }
}
