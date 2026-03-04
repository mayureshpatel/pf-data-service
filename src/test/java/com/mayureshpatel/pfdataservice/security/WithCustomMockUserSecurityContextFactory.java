package com.mayureshpatel.pfdataservice.security;

import com.mayureshpatel.pfdataservice.domain.user.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

public class WithCustomMockUserSecurityContextFactory implements WithSecurityContextFactory<WithCustomMockUser> {

    public static RequestPostProcessor customMockUser(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), authorities);
        return authentication(auth);
    }

    @Override
    public SecurityContext createSecurityContext(WithCustomMockUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        User user = new User();
        user.setId(annotation.id());
        user.setUsername(annotation.username());
        user.setEmail(annotation.email());
        user.setPasswordHash("dummy-password");

        CustomUserDetails principal = new CustomUserDetails(user);

        List<SimpleGrantedAuthority> authorities = Arrays.stream(annotation.roles())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        Authentication auth = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), authorities);
        context.setAuthentication(auth);
        return context;
    }
}
