package com.bugzero.rarego.boundedContext.auth.in;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.auth.app.AuthService;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.global.security.MemberPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	// 테스트용 로그인 엔드포인트
	@PostMapping("/test/login/{memberId}")
	public SuccessResponseDto<String> login(@PathVariable Long memberId) {
		String accessToken = authService.testIssueAccessToken(memberId);
		return SuccessResponseDto.from(SuccessType.OK,accessToken);
	}

	// 테스트용 인증 확인 엔드포인트
	@GetMapping("/test/check")
	public SuccessResponseDto<String> login(@AuthenticationPrincipal MemberPrincipal memberPrincipal) {
		return SuccessResponseDto.from(SuccessType.OK, memberPrincipal.toString());
	}

}
