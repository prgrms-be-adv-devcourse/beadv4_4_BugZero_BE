package com.bugzero.rarego.global.security;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bugzero.rarego.boundedContext.auth.domain.AuthRole;

class JwtParserTest {
	private static final String SECRET_KEY = "abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890";
	private JwtProvider jwtProvider;
	private JwtParser jwtParser;

	@BeforeEach
	void setUp() {
		jwtProvider = new JwtProvider(SECRET_KEY);
		jwtParser = new JwtParser(SECRET_KEY);
	}

	@Test
	@DisplayName("parsePrincipal은 id 가진 MemberPrincipal을 반환한다.")
	void parsePrincipalReturnsMemberPrincipal() {
		String jwt = jwtProvider.issueToken(
			60 * 60,
			Map.of("id", 1L, "role", AuthRole.USER.name())
		);

		MemberPrincipal principal = jwtParser.parsePrincipal(jwt);

		assertThat(principal).isNotNull();
		assertThat(principal.id()).isEqualTo(1L);
		assertThat(principal.role()).isEqualTo(AuthRole.USER.name());
	}

	@Test
	@DisplayName("parsePrincipal은 id가 Integer여도 Long으로 변환한다.")
	void parsePrincipalConvertsIntegerIdToLong() {
		String jwt = jwtProvider.issueToken(
			60 * 60,
			Map.of("id", 1, "role", AuthRole.USER.name())
		);

		MemberPrincipal principal = jwtParser.parsePrincipal(jwt);

		assertThat(principal).isNotNull();
		assertThat(principal.id()).isEqualTo(1L);
		assertThat(principal.role()).isEqualTo(AuthRole.USER.name());
	}

	@Test
	@DisplayName("parsePrincipal은 유효한 토큰이더라도 필수 클레임(id, role)이 없으면 null을 반환한다.")
	void parsePrincipalReturnsNullWhenClaimsMissing() {
		String jwt = jwtProvider.issueToken(60 * 60, Map.of());

		MemberPrincipal principal = jwtParser.parsePrincipal(jwt);

		assertThat(principal).isNull();
	}

	@Test
	@DisplayName("parsePrincipal은 유효하지 않은 토큰이면 null을 반환한다.")
	void parsePrincipalReturnsNullWhenInvalid() {
		MemberPrincipal principal = jwtParser.parsePrincipal("invalid.jwt.token");

		assertThat(principal).isNull();
	}
}
