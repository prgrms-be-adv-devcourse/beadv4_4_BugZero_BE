package com.bugzero.rarego.global.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {
	private static final String ACCESS_TOKEN_ATTRIBUTE = "accessToken";
	private final ObjectMapper objectMapper;

	@Value("${custom.global.frontUrl}")
	private String frontUrl;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException {

		if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Authentication");
			return;
		}

		OAuth2User oauth2User = oauthToken.getPrincipal();
		String accessToken = (String)oauth2User.getAttributes().get(ACCESS_TOKEN_ATTRIBUTE);

		if (accessToken == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "AccessToken missing");
			return;
		}

		// TODO: 리프레시 토큰 쿠키에 저장

		// JSON 응답 대신 리다이렉트 수행
		String targetUrl = UriComponentsBuilder.fromUriString(frontUrl + "/auth/callback")
			.queryParam("accessToken", accessToken)
			.build().toUriString();

		response.sendRedirect(targetUrl);
	}
}
