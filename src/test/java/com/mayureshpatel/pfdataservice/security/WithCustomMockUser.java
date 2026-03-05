package com.mayureshpatel.pfdataservice.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithCustomMockUserSecurityContextFactory.class)
public @interface WithCustomMockUser {
    long id() default 1L;

    String username() default "john_doe";

    String email() default "john@example.com";

    String[] roles() default {"USER"};
}
