package com.bugzero.rarego.boundedContext.member.in;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.auction.dto.MyBidResponseDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

	private final AuctionFacade auctionFacade;
	private final MemberRepository memberRepository;

	@GetMapping("/me/bids")
	public PagedResponseDto<MyBidResponseDto> getMyBids(
		@RequestParam(required = false) AuctionStatus auctionStatus,
		@AuthenticationPrincipal MemberPrincipal memberPrincipal,
		@PageableDefault(size = 20) Pageable pageable
	) {

		Member member = memberRepository.findByPublicId(memberPrincipal.publicId());
		Long memberId = (member != null) ? member.getId() : 2L;
		// 혹시나하는 테스트 편의를 위해 토큰이 없으면 2번 유저로 간주하는 임시 코드
		// Long memberId = (memberPrincipal != null) ? Long.valueOf(userDetails.getUsername()) : 2L;

		return auctionFacade.getMyBids(memberId, auctionStatus, pageable);
	}

}
