package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithCustomMockUser.Factory.class)
public @interface WithCustomMockUser {
    long id() default 1L;
    String username() default "user";

    class Factory implements WithSecurityContextFactory<WithCustomMockUser> {
        @Override
        public SecurityContext createSecurityContext(WithCustomMockUser annotation) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            
            User user = new User();
            user.setId(annotation.id());
            user.setUsername(annotation.username());
            user.setPasswordHash("password");
            user.setEmail("test@test.com");
            user.setLastUpdatedBy("system");
            
            // CustomUserDetails needs a User object
            CustomUserDetails principal = new CustomUserDetails(user);
            
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities());
            context.setAuthentication(auth);
            return context;
        }
    }
}
