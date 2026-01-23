package com.bugzero.rarego.boundedContext.member.in;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.member.app.MemberFacade;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.member.domain.MemberWithdrawRequestDto;
import com.bugzero.rarego.shared.member.domain.MemberWithdrawResponseDto;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/members")
@RequiredArgsConstructor
@Tag(name = "Internal - Member", description = "내부 회원 API (시스템 전용)")
@Hidden
public class InternalMemberController {
	private final MemberFacade memberFacade;

	@Operation(summary = "회원 탈퇴", description = "내부 시스템에서 회원을 소프트 삭제합니다")
	@PostMapping("/withdraw")
	public SuccessResponseDto<MemberWithdrawResponseDto> withdraw(
		@RequestBody MemberWithdrawRequestDto requestDto
	) {
		String publicId = memberFacade.withdraw(requestDto.publicId());
		return SuccessResponseDto.from(SuccessType.OK, new MemberWithdrawResponseDto(publicId));
	}
}
