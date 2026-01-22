package com.bugzero.rarego.boundedContext.member.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.member.app.MemberFacade;
import com.bugzero.rarego.boundedContext.member.domain.MemberMeResponseDto;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateIdentityRequestDto;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateRequestDto;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.member.domain.MemberJoinRequestDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "Member", description = "회원 관련 API")
@RequiredArgsConstructor
public class MemberController {

  private final MemberFacade memberFacade;
  private final AuctionFacade auctionFacade;

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "소셜 로그인 이후 Member 생성(회원 가입)", description = "소셜 로그인 결과(email/provider)를 받아 회원가입 처리합니다.")
	@PostMapping("/me")
	public SuccessResponseDto<MemberJoinResponseDto> join(@RequestBody MemberJoinRequestDto requestDto) {
		MemberJoinResponseDto responseDto = memberFacade.join(requestDto.email());
		return SuccessResponseDto.from(SuccessType.CREATED, responseDto);
	}

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "회원 본인 조회", description = "본인의 정보를 조회합니다.")
	@GetMapping("/me")
	public SuccessResponseDto<MemberMeResponseDto> getMe(@AuthenticationPrincipal MemberPrincipal memberPrincipal) {
		MemberMeResponseDto responseDto = memberFacade.getMe(memberPrincipal.publicId(), memberPrincipal.role());
		return SuccessResponseDto.from(SuccessType.OK, responseDto);
	}

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "회원 본인 수정", description = "본인의 정보를 수정합니다.")
	@PatchMapping("/me")
	public SuccessResponseDto<MemberUpdateResponseDto> updateMe(
		@AuthenticationPrincipal MemberPrincipal memberPrincipal,
		@RequestBody MemberUpdateRequestDto requestDto
	) {
		return SuccessResponseDto.from(SuccessType.OK,
			memberFacade.updateMe(memberPrincipal.publicId(), memberPrincipal.role(), requestDto)
		);
	}

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "회원 본인인증 정보 수정", description = "본인의 연락처/실명 정보를 수정합니다.")
	@PatchMapping("/me/identity")
	public SuccessResponseDto<MemberUpdateResponseDto> updateIdentity(
		@AuthenticationPrincipal MemberPrincipal memberPrincipal,
		@RequestBody MemberUpdateIdentityRequestDto requestDto
	) {
		return SuccessResponseDto.from(SuccessType.OK,
			memberFacade.updateIdentity(memberPrincipal.publicId(), requestDto)
		);
	}

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "회원 판매자 인증", description = "저장된 정보를 기반으로 SELLER로 업데이트 합니다")
	@PostMapping("/me/seller")
	public SuccessResponseDto<Void> promoteSeller(@AuthenticationPrincipal MemberPrincipal memberPrincipal) {
		memberFacade.promoteSeller(memberPrincipal.publicId(), memberPrincipal.role());
		return SuccessResponseDto.from(SuccessType.OK);
	}
}
