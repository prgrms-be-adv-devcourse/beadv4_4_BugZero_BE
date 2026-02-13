package com.bugzero.rarego.support;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.test.context.support.WithSecurityContext;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockMemberPrincipalSecurityContextFactory.class)
public @interface WithMockMemberPrincipal {
	String publicId() default "test-public-id";

	String role() default "ROLE_USER";
}
