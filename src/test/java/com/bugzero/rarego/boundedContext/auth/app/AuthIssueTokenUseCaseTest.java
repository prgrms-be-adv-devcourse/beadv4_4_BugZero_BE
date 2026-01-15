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

import com.bugzero.rarego.boundedContext.auth.domain.TokenIssueDto;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.security.JwtProvider;
import com.bugzero.rarego.shared.member.domain.MemberRole;

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
	@DisplayName("access 토큰 발급 시 멤버 id/role과 access 만료시간을 전달한다.")
	void issueAccessTokenPassesMemberClaimsAndExpireSeconds() {
		TokenIssueDto tokenIssueDto = new TokenIssueDto(1L, MemberRole.USER.name());

		when(jwtProvider.issueToken(eq(3600), argThat(body ->
			((Number) body.get("id")).longValue() == 1L
				&& MemberRole.USER.name().equals(body.get("role"))
				&& body.size() == 2
		))).thenReturn("token");

		String token = authIssueTokenUseCase.issueToken(tokenIssueDto, true);

		assertThat(token).isEqualTo("token");
		verify(jwtProvider).issueToken(eq(3600), any(Map.class));
	}

	@Test
	@DisplayName("refresh 토큰 발급 시 멤버 id/role과 refresh 만료시간을 전달한다.")
	void issueRefreshTokenPassesMemberClaimsAndExpireSeconds() {
		TokenIssueDto tokenIssueDto = new TokenIssueDto(2L, MemberRole.ADMIN.name());

		when(jwtProvider.issueToken(eq(7200), argThat(body ->
			((Number) body.get("id")).longValue() == 2L
				&& MemberRole.ADMIN.name().equals(body.get("role"))
				&& body.size() == 2
		))).thenReturn("refresh-token");

		String token = authIssueTokenUseCase.issueToken(tokenIssueDto, false);

		assertThat(token).isEqualTo("refresh-token");
		verify(jwtProvider).issueToken(eq(7200), any(Map.class));
	}

	@Test
	@DisplayName("refresh 토큰 발급 시 access 만료시간으로는 호출하지 않는다.")
	void issueRefreshTokenDoesNotUseAccessExpireSeconds() {
		TokenIssueDto tokenIssueDto = new TokenIssueDto(3L, MemberRole.USER.name());

		when(jwtProvider.issueToken(eq(7200), anyMap())).thenReturn("refresh-token");

		authIssueTokenUseCase.issueToken(tokenIssueDto, false);

		verify(jwtProvider).issueToken(eq(7200), any(Map.class));
		verify(jwtProvider, never()).issueToken(eq(3600), any(Map.class));
	}

	@Test
	@DisplayName("jwtProvider가 예외를 던지면 JWT_ISSUE_FAILED로 감싼다.")
	void issueTokenWrapsUnexpectedException() {
		TokenIssueDto tokenIssueDto = new TokenIssueDto(1L, MemberRole.USER.name());

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

		TokenIssueDto tokenIssueDto = new TokenIssueDto(1L, MemberRole.USER.name());

		assertThatThrownBy(() -> authIssueTokenUseCase.issueToken(tokenIssueDto, true))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.JWT_EXPIRE_SECONDS_INVALID);
	}

	@Test
	@DisplayName("refresh 만료시간이 유효하지 않으면 JWT_EXPIRE_SECONDS_INVALID 예외가 발생한다.")
	void issueTokenFailsWhenRefreshExpireSecondsInvalid() throws Exception {
		setField(authIssueTokenUseCase, "refreshTokenExpireSeconds", -1);

		TokenIssueDto tokenIssueDto = new TokenIssueDto(1L, MemberRole.USER.name());

		assertThatThrownBy(() -> authIssueTokenUseCase.issueToken(tokenIssueDto, false))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.JWT_EXPIRE_SECONDS_INVALID);
	}

	@Test
	@DisplayName("role이 null이면 AUTH_MEMBER_REQUIRED 예외가 발생한다.")
	void issueTokenFailsWhenRoleIsNull() {
		TokenIssueDto tokenIssueDto = new TokenIssueDto(1L, null);

		assertThatThrownBy(() -> authIssueTokenUseCase.issueToken(tokenIssueDto, true))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.INVALID_INPUT);
	}

	@Test
	@DisplayName("member id가 없으면 AUTH_MEMBER_ID_INVALID 예외가 발생한다.")
	void issueTokenFailsWhenMemberIdMissing() {
		TokenIssueDto tokenIssueDto = new TokenIssueDto(null, MemberRole.USER.name());

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
