package com.bugzero.rarego.global.security;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
	@DisplayName("parsePrincipal은 id/nickname을 가진 MemberPrincipal을 반환한다.")
	void parsePrincipalReturnsMemberPrincipal() {
		String jwt = jwtProvider.issueToken(
			60 * 60,
			Map.of("id", 1L, "nickname", "친절한 옥수수")
		);

		MemberPrincipal principal = jwtParser.parsePrincipal(jwt);

		assertThat(principal).isNotNull();
		assertThat(principal.id()).isEqualTo(1L);
		assertThat(principal.nickname()).isEqualTo("친절한 옥수수");
	}

	@Test
	@DisplayName("parsePrincipal은 id가 Integer여도 Long으로 변환한다.")
	void parsePrincipalConvertsIntegerIdToLong() {
		String jwt = jwtProvider.issueToken(
			60 * 60,
			Map.of("id", 1, "nickname", "친절한 옥수수")
		);

		MemberPrincipal principal = jwtParser.parsePrincipal(jwt);

		assertThat(principal).isNotNull();
		assertThat(principal.id()).isEqualTo(1L);
		assertThat(principal.nickname()).isEqualTo("친절한 옥수수");
	}

	@Test
	@DisplayName("parsePrincipal은 유효한 토큰이지만 클레임이 없으면 null 값을 담아 반환한다.")
	void parsePrincipalReturnsPrincipalWithNullFieldsWhenClaimsMissing() {
		String jwt = jwtProvider.issueToken(60 * 60, Map.of());

		MemberPrincipal principal = jwtParser.parsePrincipal(jwt);

		assertThat(principal).isNotNull();
		assertThat(principal.id()).isNull();
		assertThat(principal.nickname()).isNull();
	}

	@Test
	@DisplayName("parsePrincipal은 유효하지 않은 토큰이면 null을 반환한다.")
	void parsePrincipalReturnsNullWhenInvalid() {
		MemberPrincipal principal = jwtParser.parsePrincipal("invalid.jwt.token");

		assertThat(principal).isNull();
	}
}
