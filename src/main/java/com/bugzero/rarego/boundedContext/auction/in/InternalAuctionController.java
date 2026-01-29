package com.bugzero.rarego.boundedContext.auction.in;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.app.AuctionSettleAuctionFacade;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionAutoSettleResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/internal/auctions")
@RequiredArgsConstructor
@Tag(name = "Internal - Auction", description = "내부 경매 API (시스템 전용)")
@Hidden
public class InternalAuctionController {

    private final AuctionSettleAuctionFacade facade;
    private final AuctionFacade auctionFacade;


    @Operation(summary = "경매 정산", description = "종료된 경매를 정산합니다")
    @PostMapping("/settle")
    public SuccessResponseDto<AuctionAutoSettleResponseDto> settle() {
        return SuccessResponseDto.from(SuccessType.OK, facade.settle());
    }

    @Operation(summary = "진행 중인 입찰이 있는지 확인", description = "진행 중인 입찰이 있는지 확인합니다")
    @GetMapping("/members/{publicId}/bids/active")
    public SuccessResponseDto<Boolean> hasActiveBids(@PathVariable String publicId) {
        return SuccessResponseDto.from(SuccessType.OK, auctionFacade.hasActiveBids(publicId));
    }

    @Operation(summary = "진행 중인 판매가 있는지 확인", description = "진행 중인 판매가 있는지 확인합니다")
    @GetMapping("/members/{publicId}/sales/active")
    public SuccessResponseDto<Boolean> hasActiveSales(@PathVariable String publicId) {
        return SuccessResponseDto.from(SuccessType.OK, auctionFacade.hasActiveSales(publicId));
    }

    @GetMapping("/members/{publicId}/orders/processing")
    public SuccessResponseDto<Boolean> hasProcessingOrders(@PathVariable String publicId) {
        return SuccessResponseDto.from(SuccessType.OK, auctionFacade.hasProcessingOrders(publicId));
    }

    @Operation(summary = "경매정보 생성", description = "신규 상품 경매 정보를 생성합니다.")
    @PostMapping("/{productId}/{publicId}")
    public SuccessResponseDto<Long> createAuction (
        @PathVariable Long productId,
        @PathVariable String publicId,
        @Valid @RequestBody ProductAuctionRequestDto productAuctionRequestDto
    ) {
        return SuccessResponseDto.from(SuccessType.CREATED,
            auctionFacade.createAuction(productId, publicId, productAuctionRequestDto));
    }

    @Operation(summary = "경매정보 수정", description = "검수 확정 전인 경매 정보를 수정합니다.")
    @PatchMapping("/{publicId}")
    public SuccessResponseDto<Long> updateAuction (
        @PathVariable String publicId,
        @Valid @RequestBody ProductAuctionUpdateDto productAuctionUpdateDto
    ) {
        return SuccessResponseDto.from(SuccessType.OK,
            auctionFacade.updateAuction(publicId, productAuctionUpdateDto));
    }

    @Operation(summary = "경매정보 수정", description = "검수 확정 전인 경매 정보를 삭제합니다.")
    @DeleteMapping("/{productId}/{publicId}")
    public SuccessResponseDto<Void> deleteAuction (
        @PathVariable String publicId,
        @PathVariable Long productId
    ) {
        auctionFacade.deleteAuction(publicId, productId);
        return SuccessResponseDto.from(SuccessType.OK);
    }
}

