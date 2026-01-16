package com.bugzero.rarego.boundedContext.auth.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.boundedContext.auth.domain.AuthRole;
import com.bugzero.rarego.boundedContext.auth.domain.TokenIssueDto;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.security.JwtProvider;

@ExtendWith(MockitoExtension.class)
class AuthIssueTokenUseCaseTest {
	@Mock
	private JwtProvider jwtProvider;

	@InjectMocks
	private AuthIssueTokenUseCase authIssueTokenUseCase;

	@BeforeEach
	void setUp() throws Exception {
		setField(authIssueTokenUseCase, "accessTokenExpireSeconds", 3600);
		setField(authIssueTokenUseCase, "refreshTokenExpireSeconds", 7200);
	}

	@Test
	@DisplayName("access 토큰 발급 시 멤버 publicId/role과 access 만료시간을 전달한다.")
	void issueAccessTokenPassesMemberClaimsAndExpireSeconds() {
		TokenIssueDto tokenIssueDto = new TokenIssueDto("550e8400-e29b-41d4-a716-446655440000", AuthRole.USER.name());

		when(jwtProvider.issueToken(eq(3600), argThat(body ->
			"550e8400-e29b-41d4-a716-446655440000".equals(body.get("publicId"))
				&& AuthRole.USER.name().equals(body.get("role"))
				&& body.size() == 2
		))).thenReturn("token");

		String token = authIssueTokenUseCase.issueToken(tokenIssueDto, true);

		assertThat(token).isEqualTo("token");
		verify(jwtProvider).issueToken(eq(3600), any(Map.class));
	}

	@Test
	@DisplayName("refresh 토큰 발급 시 멤버 publicId/role과 refresh 만료시간을 전달한다.")
	void issueRefreshTokenPassesMemberClaimsAndExpireSeconds() {
		TokenIssueDto tokenIssueDto = new TokenIssueDto("1e2c1e52-7e77-4f5d-8c4f-1a2a12b7f9aa", AuthRole.ADMIN.name());

		when(jwtProvider.issueToken(eq(7200), argThat(body ->
			"1e2c1e52-7e77-4f5d-8c4f-1a2a12b7f9aa".equals(body.get("publicId"))
				&& AuthRole.ADMIN.name().equals(body.get("role"))
				&& body.size() == 2
		))).thenReturn("refresh-token");

		String token = authIssueTokenUseCase.issueToken(tokenIssueDto, false);

		assertThat(token).isEqualTo("refresh-token");
		verify(jwtProvider).issueToken(eq(7200), any(Map.class));
	}

	@Test
	@DisplayName("refresh 토큰 발급 시 access 만료시간으로는 호출하지 않는다.")
	void issueRefreshTokenDoesNotUseAccessExpireSeconds() {
		TokenIssueDto tokenIssueDto = new TokenIssueDto("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d", AuthRole.USER.name());

		when(jwtProvider.issueToken(eq(7200), anyMap())).thenReturn("refresh-token");

		authIssueTokenUseCase.issueToken(tokenIssueDto, false);

		verify(jwtProvider).issueToken(eq(7200), any(Map.class));
		verify(jwtProvider, never()).issueToken(eq(3600), any(Map.class));
	}

	@Test
	@DisplayName("jwtProvider가 예외를 던지면 JWT_ISSUE_FAILED로 감싼다.")
	void issueTokenWrapsUnexpectedException() {
		TokenIssueDto tokenIssueDto = new TokenIssueDto("550e8400-e29b-41d4-a716-446655440000", AuthRole.USER.name());

		when(jwtProvider.issueToken(anyInt(), anyMap())).thenThrow(new RuntimeException("boom"));

		assertThatThrownBy(() -> authIssueTokenUseCase.issueToken(tokenIssueDto, true))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.JWT_ISSUE_FAILED);
	}

	@Test
	@DisplayName("access 만료시간이 유효하지 않으면 JWT_EXPIRE_SECONDS_INVALID 예외가 발생한다.")
	void issueTokenFailsWhenAccessExpireSecondsInvalid() throws Exception {
		setField(authIssueTokenUseCase, "accessTokenExpireSeconds", 0);

		TokenIssueDto tokenIssueDto = new TokenIssueDto("550e8400-e29b-41d4-a716-446655440000", AuthRole.USER.name());

		assertThatThrownBy(() -> authIssueTokenUseCase.issueToken(tokenIssueDto, true))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.JWT_EXPIRE_SECONDS_INVALID);
	}

	@Test
	@DisplayName("refresh 만료시간이 유효하지 않으면 JWT_EXPIRE_SECONDS_INVALID 예외가 발생한다.")
	void issueTokenFailsWhenRefreshExpireSecondsInvalid() throws Exception {
		setField(authIssueTokenUseCase, "refreshTokenExpireSeconds", -1);

		TokenIssueDto tokenIssueDto = new TokenIssueDto("550e8400-e29b-41d4-a716-446655440000", AuthRole.USER.name());

		assertThatThrownBy(() -> authIssueTokenUseCase.issueToken(tokenIssueDto, false))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.JWT_EXPIRE_SECONDS_INVALID);
	}

	@Test
	@DisplayName("role이 null이면 AUTH_MEMBER_REQUIRED 예외가 발생한다.")
	void issueTokenFailsWhenRoleIsNull() {
		TokenIssueDto tokenIssueDto = new TokenIssueDto("550e8400-e29b-41d4-a716-446655440000", null);

		assertThatThrownBy(() -> authIssueTokenUseCase.issueToken(tokenIssueDto, true))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.INVALID_INPUT);
	}

	@Test
	@DisplayName("member publicId가 없으면 INVALID_INPUT 예외가 발생한다.")
	void issueTokenFailsWhenMemberPublicIdMissing() {
		TokenIssueDto tokenIssueDto = new TokenIssueDto(null, AuthRole.USER.name());

		assertThatThrownBy(() -> authIssueTokenUseCase.issueToken(tokenIssueDto, true))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.INVALID_INPUT);
	}

	private static void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
