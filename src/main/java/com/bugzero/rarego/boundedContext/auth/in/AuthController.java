package com.bugzero.rarego.boundedContext.auth.in;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.auth.app.AuthFacade;
import com.bugzero.rarego.boundedContext.auth.domain.TokenIssueDto;
import com.bugzero.rarego.boundedContext.auth.domain.TokenPairDto;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.global.security.MemberPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

	private static final String REFRESH_TOKEN_ATTRIBUTE = "refreshToken";
	private final AuthFacade authFacade;
	@Value("${jwt.refresh-token-expire-seconds}")
	private int refreshTokenExpireSeconds;

	@Value("${jwt.refresh-token-cookie-secure:false}")
	private boolean refreshTokenCookieSecure;

	@Value("${jwt.refresh-token-cookie-same-site:Lax}")
	private String refreshTokenCookieSameSite;

	// Bearer
	private static String resolveToken(String header) {
		if (header == null || (!header.startsWith("Bearer ") && !header.startsWith("bearer "))) {
			return null;
		}
		return header.substring(7);
	}

	// 테스트용 로그인 엔드포인트
	@Operation(summary = "테스트 로그인", description = "테스트용 JWT 토큰을 발급합니다")
	@PostMapping("/test/login")
	public SuccessResponseDto<String> login(@RequestBody TokenIssueDto requestDto) {
		String accessToken = authFacade.issueAccessToken(requestDto.memberPublicId(), requestDto.role());
		return SuccessResponseDto.from(SuccessType.OK, accessToken);
	}

	// 테스트용 인증 확인 엔드포인트
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "인증 확인", description = "현재 인증된 사용자 정보를 확인합니다")
	@GetMapping("/test/check")
	public SuccessResponseDto<String> login(@AuthenticationPrincipal MemberPrincipal memberPrincipal) {
		if (memberPrincipal == null) {
			throw new CustomException(ErrorType.AUTH_MEMBER_REQUIRED);
		}
		return SuccessResponseDto.from(SuccessType.OK, memberPrincipal.toString());
	}

	// 테스트용 인증 확인 엔드포인트
	// 오직 5번 멤버(관리자)만 접근 가능
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "관리자 확인", description = "관리자 권한을 확인합니다")
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/test/admin")
	public SuccessResponseDto<String> justAdmin(@AuthenticationPrincipal MemberPrincipal memberPrincipal) {
		if (memberPrincipal == null) {
			throw new CustomException(ErrorType.AUTH_MEMBER_REQUIRED);
		}
		return SuccessResponseDto.from(SuccessType.OK, memberPrincipal.toString());
	}

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 액세스 토큰을 재발급합니다")
	@PostMapping("/refresh")
	public SuccessResponseDto<Map<String, String>> refresh(
		@CookieValue(value = REFRESH_TOKEN_ATTRIBUTE, required = false) String refreshToken,
		@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
		HttpServletResponse response) {
		String accessToken = resolveToken(authorization);
		TokenPairDto tokenPair = authFacade.refresh(refreshToken, accessToken);

		// 본문에 accessToken, 쿠키에 refreshToken
		ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_TOKEN_ATTRIBUTE, tokenPair.refreshToken())
			.httpOnly(true)
			.secure(refreshTokenCookieSecure)
			.path("/")
			.maxAge(Duration.ofSeconds(refreshTokenExpireSeconds))
			.sameSite(refreshTokenCookieSameSite)
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

		return SuccessResponseDto.from(
			SuccessType.OK,
			Map.of("accessToken", tokenPair.accessToken())
		);
	}
}
