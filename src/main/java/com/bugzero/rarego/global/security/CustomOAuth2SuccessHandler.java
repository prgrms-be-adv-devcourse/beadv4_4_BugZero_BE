package com.bugzero.rarego.global.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.boundedContext.auth.app.AuthService;
import com.bugzero.rarego.boundedContext.auth.domain.AccountDto;
import com.bugzero.rarego.boundedContext.auth.domain.OAuth2AttributeMapper;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {
	private final AuthService authService;
	private final ClientRegistrationRepository clientRegistrationRepository;
	private final ObjectMapper objectMapper;

	public CustomOAuth2SuccessHandler(AuthService authService,
		ClientRegistrationRepository clientRegistrationRepository,
		ObjectMapper objectMapper) {
		this.authService = authService;
		this.clientRegistrationRepository = clientRegistrationRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException {
		if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		String registrationId = oauthToken.getAuthorizedClientRegistrationId();
		ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(registrationId);
		if (registration == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		String userNameAttributeName = registration.getProviderDetails()
			.getUserInfoEndpoint().getUserNameAttributeName();
		OAuth2User oauth2User = oauthToken.getPrincipal();

		AccountDto accountDto = OAuth2AttributeMapper.toAccountDto(
			registrationId, userNameAttributeName, oauth2User.getAttributes());
		String accessToken = authService.login(accountDto);

		SuccessResponseDto<String> body = SuccessResponseDto.from(SuccessType.OK, accessToken);
		response.setStatus(body.status());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
