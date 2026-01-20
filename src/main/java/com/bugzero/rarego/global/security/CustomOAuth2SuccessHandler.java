package com.bugzero.rarego.global.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {
	private static final String ACCESS_TOKEN_ATTRIBUTE = "accessToken";
	private static final String REFRESH_TOKEN_ATTRIBUTE = "refreshToken";
	private final ObjectMapper objectMapper;

	@Value("${jwt.refresh-token-expire-seconds}")
	private int refreshTokenExpireSeconds;

	@Value("${jwt.refresh-token-cookie-secure:false}")
	private boolean refreshTokenCookieSecure;

	@Value("${jwt.refresh-token-cookie-same-site:Lax}")
	private String refreshTokenCookieSameSite;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException {
		if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		OAuth2User oauth2User = oauthToken.getPrincipal();

		Object accessTokenValue = oauth2User.getAttributes().get(ACCESS_TOKEN_ATTRIBUTE);
		Object refreshTokenValue = oauth2User.getAttributes().get(REFRESH_TOKEN_ATTRIBUTE);
		if (!(accessTokenValue instanceof String accessToken)
			|| !(refreshTokenValue instanceof String refreshToken)) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_TOKEN_ATTRIBUTE, refreshToken)
			.httpOnly(true)
			.secure(refreshTokenCookieSecure)
			.path("/")
			.maxAge(Duration.ofSeconds(refreshTokenExpireSeconds))
			.sameSite(refreshTokenCookieSameSite)
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

		// 토큰을 SuccessResponseDto에 JSON 형태로 담아 응답해줌
		SuccessResponseDto<Map<String, String>> body = SuccessResponseDto.from(
			SuccessType.OK,
			Map.of("accessToken", accessToken)
		);
		response.setStatus(body.status());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
