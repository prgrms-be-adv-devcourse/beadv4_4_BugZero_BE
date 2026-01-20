package com.bugzero.rarego.boundedContext.member.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.member.app.MemberFacade;
import com.bugzero.rarego.boundedContext.member.domain.MemberMeResponseDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.auction.dto.AuctionFilterType;
import com.bugzero.rarego.shared.auction.dto.MyBidResponseDto;
import com.bugzero.rarego.shared.auction.dto.MySaleResponseDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.member.domain.MemberJoinRequestDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "Member", description = "회원 관련 API")
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

    @GetMapping("/me/sales")
    @PreAuthorize("hasRole('SELLER')") // SELLER 권한만 접근 가능
    public PagedResponseDto<MySaleResponseDto> getMySales(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(required = false, defaultValue = "ALL") AuctionFilterType filter,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Long memberId = Long.valueOf(principal.publicId());
        return auctionFacade.getMySales(memberId, filter, pageable);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/me")
    public SuccessResponseDto<MemberJoinResponseDto> join(@RequestBody MemberJoinRequestDto requestDto) {
        MemberJoinResponseDto responseDto = memberFacade.join(requestDto.email());
        return SuccessResponseDto.from(SuccessType.CREATED, responseDto);
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public SuccessResponseDto<MemberMeResponseDto> getMe(@AuthenticationPrincipal MemberPrincipal memberPrincipal) {
        MemberMeResponseDto responseDto = memberFacade.getMe(memberPrincipal.publicId(), memberPrincipal.role());
        return SuccessResponseDto.from(SuccessType.OK, responseDto);
    }
}
