package com.bugzero.rarego.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class InternalAuthenticationFilter extends OncePerRequestFilter {
	private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
	private static final String INTERNAL_PATH_PATTERN = "/internal/**";
	private static final AntPathMatcher pathMatcher = new AntPathMatcher();

	@Value("${spring.security.internal.secret}")
	private String internalSecret;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String requestPath = request.getRequestURI();

		if (pathMatcher.match(INTERNAL_PATH_PATTERN, requestPath)) {
			String secretHeader = request.getHeader(INTERNAL_SECRET_HEADER);

			if (secretHeader == null || secretHeader.isBlank() || !secretHeader.equals(internalSecret)) {
				log.warn(
						"[security] 인증되지 않은 internal endpoint: {} from IP: {}",
						requestPath,
						request.getRemoteAddr()
				);
				// 자동으로 CustomAuthenticationEntryPoint에서 401 오류
				throw new InsufficientAuthenticationException("Invalid internal secret");
			}

			log.debug("[security] 내부 API 허용: {}", requestPath);
		}
		filterChain.doFilter(request, response);
	}
}
