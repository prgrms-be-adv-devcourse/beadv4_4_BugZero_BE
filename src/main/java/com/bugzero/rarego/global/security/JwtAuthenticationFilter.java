package com.bugzero.rarego.global.security;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtParser jwtParser;

	public JwtAuthenticationFilter(JwtParser jwtParser) {
		this.jwtParser = jwtParser;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		String token = resolveToken(request);
		if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {

			// 토큰이 유효한지 검사, 유효하지 않으면 null 반환
			MemberPrincipal principal = jwtParser.parsePrincipal(token);
			if (principal != null) {
				//	Security에서 권한은 GrantedAuthority 리스트 형태
				List<GrantedAuthority> authorities = toAuthorities(principal.role());
				// “로그인 성공한 사용자”를 표현하는 Security 표준 객체 생성
				Authentication authentication = new UsernamePasswordAuthenticationToken(
					principal,
					null,
					authorities
				);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		}

		filterChain.doFilter(request, response);
	}

	private static String resolveToken(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (header == null || !header.startsWith("Bearer "))
			return null;
		return header.substring(7);
	}

	private static List<GrantedAuthority> toAuthorities(String role) {
		if (role == null || role.isBlank())
			return Collections.emptyList();
		return List.of(new SimpleGrantedAuthority("ROLE_" + role));
	}
}
