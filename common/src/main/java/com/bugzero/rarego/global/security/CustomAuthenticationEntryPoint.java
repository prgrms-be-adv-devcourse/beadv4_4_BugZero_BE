package com.bugzero.rarego.global.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.ExceptionResponseDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
	private final ObjectMapper objectMapper;

	public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException) throws IOException {
		// 내부 인증 오류
		if (isInternalPath(request)) {
			log.warn("[common] 내부 API 인증에 실패했습니다. method={}, uri={}",
				request.getMethod(),
				request.getRequestURI()
			);
		}

		// 개발용 오류 세부 내용 로그
		Throwable cause = authException.getCause();
		log.debug("[common/디버그용] entrypoint 오류 적용. method={}, uri={}, ex={}, msg={}, cause={}",
			request.getMethod(),
			request.getRequestURI(),
			authException.getClass().getName(),
			authException.getMessage(),
			cause == null ? "null" : cause.getClass().getName());

		// 인증 오류
		ExceptionResponseDto body = ExceptionResponseDto.from(ErrorType.AUTH_UNAUTHORIZED);
		response.setStatus(body.status());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), body);
	}

	private boolean isInternalPath(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri != null && uri.startsWith("/api/v1/internal/");
	}
}
