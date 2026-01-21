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
		// given
		String memberPublicId = "550e8400-e29b-41d4-a716-446655440000";
		String role = AuthRole.USER.name();

		when(jwtProvider.issueToken(eq(3600), argThat(body ->
			memberPublicId.equals(body.get("publicId"))
				&& role.equals(body.get("role"))
				&& body.size() == 2
		))).thenReturn("token");

		// when
		String token = authIssueTokenUseCase.issueToken(memberPublicId, role, true);

		// then
		assertThat(token).isEqualTo("token");
		verify(jwtProvider).issueToken(eq(3600), any(Map.class));
	}

	@Test
	@DisplayName("refresh 토큰 발급 시 멤버 publicId/typ과 refresh 만료시간을 전달한다.")
	void issueRefreshTokenPassesMemberClaimsAndExpireSeconds() {
		// given
		String memberPublicId = "1e2c1e52-7e77-4f5d-8c4f-1a2a12b7f9aa";
		String role = AuthRole.ADMIN.name();

		when(jwtProvider.issueToken(eq(7200), argThat(body ->
			memberPublicId.equals(body.get("publicId"))
				&& "REFRESH".equals(body.get("typ"))
				&& body.size() == 2
		))).thenReturn("refresh-token");

		// when
		String token = authIssueTokenUseCase.issueToken(memberPublicId, role, false);

		// then
		assertThat(token).isEqualTo("refresh-token");
		verify(jwtProvider).issueToken(eq(7200), any(Map.class));
	}

	@Test
	@DisplayName("refresh 토큰 발급 시 access 만료시간으로는 호출하지 않는다.")
	void issueRefreshTokenDoesNotUseAccessExpireSeconds() {
		// given
		String memberPublicId = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d";
		String role = AuthRole.USER.name();

		when(jwtProvider.issueToken(eq(7200), anyMap())).thenReturn("refresh-token");

		// when
		authIssueTokenUseCase.issueToken(memberPublicId, role, false);

		// then
		verify(jwtProvider).issueToken(eq(7200), any(Map.class));
		verify(jwtProvider, never()).issueToken(eq(3600), any(Map.class));
	}

	@Test
	@DisplayName("jwtProvider가 예외를 던지면 JWT_ISSUE_FAILED로 감싼다.")
	void issueTokenWrapsUnexpectedException() {
		// given
		String memberPublicId = "550e8400-e29b-41d4-a716-446655440000";
		String role = AuthRole.USER.name();

		when(jwtProvider.issueToken(anyInt(), anyMap())).thenThrow(new RuntimeException("boom"));

		// when
		Throwable thrown = catchThrowable(() -> authIssueTokenUseCase.issueToken(memberPublicId, role, true));

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.JWT_ISSUE_FAILED);
	}

	@Test
	@DisplayName("access 만료시간이 유효하지 않으면 JWT_EXPIRE_SECONDS_INVALID 예외가 발생한다.")
	void issueTokenFailsWhenAccessExpireSecondsInvalid() throws Exception {
		// given
		setField(authIssueTokenUseCase, "accessTokenExpireSeconds", 0);

		String memberPublicId = "550e8400-e29b-41d4-a716-446655440000";
		String role = AuthRole.USER.name();

		// when
		Throwable thrown = catchThrowable(() -> authIssueTokenUseCase.issueToken(memberPublicId, role, true));

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.JWT_EXPIRE_SECONDS_INVALID);
	}

	@Test
	@DisplayName("refresh 만료시간이 유효하지 않으면 JWT_EXPIRE_SECONDS_INVALID 예외가 발생한다.")
	void issueTokenFailsWhenRefreshExpireSecondsInvalid() throws Exception {
		// given
		setField(authIssueTokenUseCase, "refreshTokenExpireSeconds", -1);

		String memberPublicId = "550e8400-e29b-41d4-a716-446655440000";
		String role = AuthRole.USER.name();

		// when
		Throwable thrown = catchThrowable(() -> authIssueTokenUseCase.issueToken(memberPublicId, role, false));

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.JWT_EXPIRE_SECONDS_INVALID);
	}

	@Test
	@DisplayName("role이 null이면 INVALID_INPUT 예외가 발생한다.")
	void issueTokenFailsWhenRoleIsNull() {
		// given
		String memberPublicId = "550e8400-e29b-41d4-a716-446655440000";

		// when
		Throwable thrown = catchThrowable(() -> authIssueTokenUseCase.issueToken(memberPublicId, null, true));

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.INVALID_INPUT);
	}

	@Test
	@DisplayName("member publicId가 없으면 서버 내부에서 INVALID_INPUT 예외가 발생한다.")
	void issueTokenFailsWhenMemberPublicIdMissing() {
		// given
		String role = AuthRole.USER.name();

		// when
		Throwable thrown = catchThrowable(() -> authIssueTokenUseCase.issueToken(null, role, true));

		// then
		assertThat(thrown)
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
