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

@ExtendWith(MockitoExtension.class)
class AuthIssueAccessTokenUseCaseTest {
	@Mock
	private JwtProvider jwtProvider;

	@InjectMocks
	private AuthIssueAccessTokenUseCase authIssueAccessTokenUseCase;

	@BeforeEach
	void setUp() throws Exception {
		setField(authIssueAccessTokenUseCase, "accessTokenExpireSeconds", 3600);
	}

	@Test
	@DisplayName("issueToken은 멤버 id/nickname을 담아 jwtProvider에 전달한다.")
	void issueTokenPassesMemberClaims() throws Exception {

		AuthMember member = AuthMember.builder()
			.id(1L)
			.nickname("친절한 옥수수")
			.build();

		when(jwtProvider.issueToken(eq(3600), argThat(body ->
			((Number) body.get("id")).longValue() == 1L
				&& "친절한 옥수수".equals(body.get("nickname"))
		))).thenReturn("token");

		String token = authIssueAccessTokenUseCase.issueToken(member);

		assertThat(token).isEqualTo("token");
		verify(jwtProvider).issueToken(eq(3600), any(Map.class));
	}


	@Test
	@DisplayName("jwtProvider가 예외를 던지면 JWT_ISSUE_FAILED로 감싼다.")
	void issueTokenWrapsUnexpectedException() throws Exception {
		AuthMember member = AuthMember.builder()
			.id(1L)
			.nickname("친절한 옥수수")
			.build();

		when(jwtProvider.issueToken(anyInt(), anyMap())).thenThrow(new RuntimeException("boom"));

		assertThatThrownBy(() -> authIssueAccessTokenUseCase.issueToken(member))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.JWT_ISSUE_FAILED);
	}

	@Test
	@DisplayName("member가 null이면 AUTH_MEMBER_REQUIRED 예외가 발생한다.")
	void issueTokenFailsWhenMemberIsNull() throws Exception {

		assertThatThrownBy(() -> authIssueAccessTokenUseCase.issueToken(null))
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
			.build();

		assertThatThrownBy(() -> authIssueAccessTokenUseCase.issueToken(member))
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
			.build();

		assertThatThrownBy(() -> authIssueAccessTokenUseCase.issueToken(member))
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
