package com.mayureshpatel.pfdataservice.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithCustomMockUserSecurityContextFactory.class)
public @interface WithCustomMockUser {
    /** The id of the mock-authenticated {@code CustomUserDetails} principal. */
    long id() default 1L;

    /** The username of the mock-authenticated principal. */
    String username() default "john_doe";

    /** The email of the mock-authenticated principal. */
    String email() default "john@example.com";

    /** The role names granted to the principal, each wrapped as a {@code ROLE_}-prefixed {@code SimpleGrantedAuthority}. */
    String[] roles() default {"USER"};
}
