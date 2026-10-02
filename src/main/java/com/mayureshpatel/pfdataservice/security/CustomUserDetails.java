package com.mayureshpatel.pfdataservice.security;

import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Adapts this application's own {@link User} domain object into the Spring Security
 * {@link UserDetails} contract -- every user currently gets exactly one role-derived authority
 * ({@code ROLE_<role>}, defaulting to {@code ROLE_USER}), not a richer permission set.
 */
public class CustomUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    @Getter
    private final Long id;
    private final String username;
    private final String password;
    @Getter
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;

    /** @param user the domain user to adapt */
    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.email = user.getEmail();
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + (user.getRole() != null ? user.getRole() : "USER"))
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
