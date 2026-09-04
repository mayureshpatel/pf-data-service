package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Core user lookups and mutations, and the {@link UserDetailsService} implementation Spring
 * Security uses to load a principal by username during authentication.
 */
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository repository;

    /**
     * Checks whether a username is already taken.
     *
     * @param username the username to check
     * @return true if a user with that username exists
     */
    public boolean isUserExistsByUsername(String username) {
        return this.repository.existsByUsername(username);
    }

    /**
     * Checks whether an email is already taken.
     *
     * @param email the email to check
     * @return true if a user with that email exists
     */
    public boolean isUserExistsByEmail(String email) {
        return this.repository.existsByEmail(email);
    }

    /**
     * Persists a new user.
     *
     * @param user the user to create
     * @return the new user's generated id
     */
    public int insert(User user) {
        return this.repository.insert(user);
    }

    /**
     * Updates the authenticated user's own username and email.
     *
     * @param authenticatedUserId the user id
     * @param username            the new username
     * @param email               the new email
     * @return the number of rows updated
     * @throws ResourceNotFoundException if no user with that id exists
     */
    public int updateProfile(Long authenticatedUserId, String username, String email) {
        User existing = repository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User updated = existing.toBuilder()
                .username(username)
                .email(email)
                .build();
        return repository.update(updated);
    }

    /**
     * Persists changes to an existing user.
     *
     * @param user the user to update
     * @return the number of rows updated
     */
    private int update(User user) {
        return this.repository.update(user);
    }

    /**
     * Finds a user by username.
     *
     * @param username the username to look up
     * @return the matching user, if any
     */
    public Optional<User> findByUsername(String username) {
        return this.repository.findByUsername(username);
    }

    /**
     * Finds a user by email.
     *
     * @param email the email to look up
     * @return the matching user, if any
     */
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    /**
     * Finds a user by id.
     *
     * @param id the user id
     * @return the matching user, if any
     */
    public Optional<User> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * Checks whether a user id exists.
     *
     * @param id the user id
     * @return true if a user with that id exists
     */
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    /**
     * Returns every user in the system. Admin-only.
     *
     * @return all users
     */
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> findAll() {
        return this.repository.findAll();
    }

    /**
     * Loads a user by username for Spring Security's authentication flow.
     *
     * @param username the username to load
     * @return the matching user, wrapped as {@link CustomUserDetails}
     * @throws UsernameNotFoundException if no user with that username exists
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new CustomUserDetails(user);
    }
}
