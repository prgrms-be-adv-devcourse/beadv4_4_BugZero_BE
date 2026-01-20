package com.bugzero.rarego.boundedContext.member.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistListResponseDto;
import com.bugzero.rarego.boundedContext.member.app.MemberFacade;
import com.bugzero.rarego.boundedContext.member.domain.MemberMeResponseDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.member.domain.MemberJoinRequestDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "Member", description = "회원 관련 API")
@RequiredArgsConstructor
public class MemberController {

    private final MemberFacade memberFacade;
    private final AuctionFacade auctionFacade;

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

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "내 관심 경매 목록 조회", description = "내가 찜한 경매 목록을 조회합니다")
    @GetMapping("/me/bookmarks")
    public PagedResponseDto<WishlistListResponseDto> getMyBookmarks(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            Pageable pageable
    ) {
        return auctionFacade.getMyBookmarks(memberPrincipal.publicId(), pageable);
    }
}
