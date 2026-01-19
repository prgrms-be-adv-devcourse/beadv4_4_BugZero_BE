package com.bugzero.rarego.boundedContext.member.in;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.member.app.MemberFacade;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.auction.dto.MyBidResponseDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinRequestDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

	private final AuctionFacade auctionFacade;
	private final MemberFacade memberFacade;

	@GetMapping("/me/bids")
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

	@PostMapping("/me")
	public SuccessResponseDto<MemberJoinResponseDto> join(@RequestBody MemberJoinRequestDto requestDto) {
		MemberJoinResponseDto responseDto = memberFacade.join(requestDto.email());
		return SuccessResponseDto.from(SuccessType.CREATED, responseDto);
	}

}
