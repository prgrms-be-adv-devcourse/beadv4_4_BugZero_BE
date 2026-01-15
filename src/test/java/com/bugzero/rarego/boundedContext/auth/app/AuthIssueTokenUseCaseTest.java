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

import com.bugzero.rarego.boundedContext.auth.domain.AuthMember;
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
	@DisplayName("access 토큰 발급 시 멤버 id/nickname/role과 access 만료시간을 전달한다.")
	void issueAccessTokenPassesMemberClaimsAndExpireSeconds() throws Exception {
		AuthMember member = AuthMember.builder()
			.id(1L)
			.nickname("친절한 옥수수")
			.role(MemberRole.USER)
			.build();

		when(jwtProvider.issueToken(eq(3600), argThat(body ->
			((Number) body.get("id")).longValue() == 1L
				&& "친절한 옥수수".equals(body.get("nickname"))
				&& MemberRole.USER.name().equals(body.get("role"))
		))).thenReturn("token");

		String token = authIssueTokenUseCase.issueToken(member, true);

		assertThat(token).isEqualTo("token");
		verify(jwtProvider).issueToken(eq(3600), any(Map.class));
	}

	@Test
	@DisplayName("refresh 토큰 발급 시 멤버 id/nickname/role과 refresh 만료시간을 전달한다.")
	void issueRefreshTokenPassesMemberClaimsAndExpireSeconds() throws Exception {
		AuthMember member = AuthMember.builder()
			.id(2L)
			.nickname("상냥한 포도")
			.role(MemberRole.ADMIN)
			.build();

		when(jwtProvider.issueToken(eq(7200), argThat(body ->
			((Number) body.get("id")).longValue() == 2L
				&& "상냥한 포도".equals(body.get("nickname"))
				&& MemberRole.ADMIN.name().equals(body.get("role"))
				&& body.size() == 3
		))).thenReturn("refresh-token");

		String token = authIssueTokenUseCase.issueToken(member, false);

		assertThat(token).isEqualTo("refresh-token");
		verify(jwtProvider).issueToken(eq(7200), any(Map.class));
	}

	@Test
	@DisplayName("refresh 토큰 발급 시 access 만료시간으로는 호출하지 않는다.")
	void issueRefreshTokenDoesNotUseAccessExpireSeconds() throws Exception {
		AuthMember member = AuthMember.builder()
			.id(3L)
			.nickname("친절한 자두")
			.role(MemberRole.USER)
			.build();

		when(jwtProvider.issueToken(eq(7200), anyMap())).thenReturn("refresh-token");

		authIssueTokenUseCase.issueToken(member, false);

		verify(jwtProvider).issueToken(eq(7200), any(Map.class));
		verify(jwtProvider, never()).issueToken(eq(3600), any(Map.class));
	}

	@Test
	@DisplayName("jwtProvider가 예외를 던지면 JWT_ISSUE_FAILED로 감싼다.")
	void issueTokenWrapsUnexpectedException() throws Exception {
		AuthMember member = AuthMember.builder()
			.id(1L)
			.nickname("친절한 옥수수")
			.role(MemberRole.USER)
			.build();

		when(jwtProvider.issueToken(anyInt(), anyMap())).thenThrow(new RuntimeException("boom"));

		assertThatThrownBy(() -> authIssueTokenUseCase.issueToken(member, true))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.JWT_ISSUE_FAILED);
	}

	@Test
	@DisplayName("access 만료시간이 유효하지 않으면 JWT_EXPIRE_SECONDS_INVALID 예외가 발생한다.")
	void issueTokenFailsWhenAccessExpireSecondsInvalid() throws Exception {
		setField(authIssueTokenUseCase, "accessTokenExpireSeconds", 0);

		AuthMember member = AuthMember.builder()
			.id(1L)
			.nickname("친절한 옥수수")
			.role(MemberRole.USER)
			.build();

		assertThatThrownBy(() -> authIssueTokenUseCase.issueToken(member, true))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.JWT_EXPIRE_SECONDS_INVALID);
	}

	@Test
	@DisplayName("refresh 만료시간이 유효하지 않으면 JWT_EXPIRE_SECONDS_INVALID 예외가 발생한다.")
	void issueTokenFailsWhenRefreshExpireSecondsInvalid() throws Exception {
		setField(authIssueTokenUseCase, "refreshTokenExpireSeconds", -1);

		AuthMember member = AuthMember.builder()
			.id(1L)
			.nickname("친절한 옥수수")
			.role(MemberRole.USER)
			.build();

		assertThatThrownBy(() -> authIssueTokenUseCase.issueToken(member, false))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.JWT_EXPIRE_SECONDS_INVALID);
	}

	@Test
	@DisplayName("member가 null이면 AUTH_MEMBER_REQUIRED 예외가 발생한다.")
	void issueTokenFailsWhenMemberIsNull() throws Exception {

		assertThatThrownBy(() -> authIssueTokenUseCase.issueToken(null, true))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_MEMBER_REQUIRED);
	}

	@Test
	@DisplayName("member id가 없거나 유효하지 않으면 AUTH_MEMBER_ID_INVALID 예외가 발생한다.")
	void issueTokenFailsWhenMemberIdInvalid() throws Exception {

		AuthMember member = AuthMember.builder()
			.id(0L)
			.nickname("친절한 옥수수")
			.role(MemberRole.USER)
			.build();

		assertThatThrownBy(() -> authIssueTokenUseCase.issueToken(member, true))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_MEMBER_ID_INVALID);
	}

	@Test
	@DisplayName("member nickname이 비어 있으면 AUTH_MEMBER_NICKNAME_REQUIRED 예외가 발생한다.")
	void issueTokenFailsWhenMemberNicknameMissing() throws Exception {
		AuthMember member = AuthMember.builder()
			.id(1L)
			.nickname(" ")
			.role(MemberRole.USER)
			.build();

		assertThatThrownBy(() -> authIssueTokenUseCase.issueToken(member, true))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_MEMBER_NICKNAME_REQUIRED);
	}

	private static void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
