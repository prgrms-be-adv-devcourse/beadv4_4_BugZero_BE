package com.bugzero.rarego.support;

import com.bugzero.rarego.global.security.MemberPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithMockMemberPrincipalSecurityContextFactory
        implements WithSecurityContextFactory<WithMockMemberPrincipal> {

    @Override
    public SecurityContext createSecurityContext(WithMockMemberPrincipal annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        MemberPrincipal principal = new MemberPrincipal(
                annotation.publicId(),
                annotation.role()
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(annotation.role()))
        );

        context.setAuthentication(auth);
        return context;
    }
}
