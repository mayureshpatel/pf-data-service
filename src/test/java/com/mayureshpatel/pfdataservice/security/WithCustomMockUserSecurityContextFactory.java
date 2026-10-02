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

/** Builds the mock {@link SecurityContext} that {@link WithCustomMockUser} installs for an annotated test, wrapping the annotation's id/username/email/roles in a {@link CustomUserDetails} principal. */
public class WithCustomMockUserSecurityContextFactory implements WithSecurityContextFactory<WithCustomMockUser> {

    /**
     * An alternate entry point for tests that already have a real {@link User} domain object on
     * hand (rather than the id/username/email fields {@link WithCustomMockUser} declares) and want
     * a matching {@link RequestPostProcessor} for a {@code MockMvc} request -- always grants a
     * fixed single {@code ROLE_USER} authority, unlike {@link #createSecurityContext}'s
     * annotation-driven roles.
     *
     * @param user the domain user to authenticate as
     * @return a request post-processor that authenticates the request as this user
     */
    public static RequestPostProcessor customMockUser(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), authorities);
        return authentication(auth);
    }

    @Override
    public SecurityContext createSecurityContext(WithCustomMockUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        User user = User.builder()
                .id(annotation.id())
                .username(annotation.username())
                .email(annotation.email())
                .passwordHash("dummy-password")
                .build();

        CustomUserDetails principal = new CustomUserDetails(user);

        List<SimpleGrantedAuthority> authorities = Arrays.stream(annotation.roles())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        Authentication auth = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), authorities);
        context.setAuthentication(auth);
        return context;
    }
}
