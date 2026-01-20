package com.bugzero.rarego.boundedContext.member.in;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.member.app.MemberFacade;
import com.bugzero.rarego.boundedContext.member.domain.MemberMeResponseDto;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateRequestDto;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateResponseDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.auction.dto.AuctionFilterType;
import com.bugzero.rarego.shared.auction.dto.MyBidResponseDto;
import com.bugzero.rarego.shared.auction.dto.MySaleResponseDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinRequestDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "Member", description = "회원 관련 API")
@RequiredArgsConstructor
public class MemberController {

    private final MemberFacade memberFacade;

	@GetMapping("/me/bids")
	@Operation(summary = "본인 입찰목록 조회", description = "본인의 입찰목록을 조회합니다. (이후 이동 예정)")
	public PagedResponseDto<MyBidResponseDto> getMyBids(
		@RequestParam(required = false) AuctionStatus auctionStatus,
		@AuthenticationPrincipal UserDetails userDetails,
		@PageableDefault(size = 20) Pageable pageable
	) {

		Long memberId = Long.valueOf(userDetails.getUsername());
		// 혹시나하는 테스트 편의를 위해 토큰이 없으면 2번 유저로 간주하는 임시 코드
		// Long memberId = (userDetails != null) ? Long.valueOf(userDetails.getUsername()) : 2L;

		return auctionFacade.getMyBids(memberId, auctionStatus, pageable);
	}

	@GetMapping("/me/sales")
	@PreAuthorize("hasRole('SELLER')") // SELLER 권한만 접근 가능
	@Operation(summary = "본인 판매목록 조회", description = "본인의 판매목록을 조회합니다. 판매자 권한만 가능합니다. (이후 이동 예정)")
	public PagedResponseDto<MySaleResponseDto> getMySales(
		@AuthenticationPrincipal MemberPrincipal principal,
		@RequestParam(required = false, defaultValue = "ALL") AuctionFilterType filter,
		@PageableDefault(size = 10) Pageable pageable
	) {
		Long memberId = Long.valueOf(principal.publicId());
		return auctionFacade.getMySales(memberId, filter, pageable);
	}

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

}
