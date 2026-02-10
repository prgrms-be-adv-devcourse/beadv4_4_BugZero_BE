package com.bugzero.rarego.in;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.app.AuthFacade;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinRequestDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/auth")
@RequiredArgsConstructor
@Tag(name = "Internal - Auth", description = "내부 인증 API (시스템 전용)")
@Hidden
public class InternalAuthController {
	private final AuthFacade authFacade;

	@Operation(summary = "판매자 승격", description = "회원 id를 받아 USER일때 role = SELLER로 승격")
	@PostMapping("/accounts/{publicId}")
	public void promoteSeller(@PathVariable String publicId) {
		authFacade.promoteSeller(publicId);
	}
}
