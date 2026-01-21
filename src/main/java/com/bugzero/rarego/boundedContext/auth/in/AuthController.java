package com.bugzero.rarego.boundedContext.auth.in;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.auth.app.AuthFacade;
import com.bugzero.rarego.boundedContext.auth.domain.TokenIssueDto;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.global.security.MemberPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

	private final AuthFacade authFacade;

	// 테스트용 로그인 엔드포인트
	@Operation(summary = "테스트 로그인", description = "테스트용 JWT 토큰을 발급합니다")
	@PostMapping("/test/login")
	public SuccessResponseDto<String> login(@RequestBody TokenIssueDto tokenIssueDto) {
		String accessToken = authFacade.issueAccessToken(tokenIssueDto);
		return SuccessResponseDto.from(SuccessType.OK,accessToken);
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

}
