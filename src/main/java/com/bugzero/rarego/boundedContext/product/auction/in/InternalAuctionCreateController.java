package com.bugzero.rarego.boundedContext.product.auction.in;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.product.auction.app.AuctionCreateAuctionUseCase;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class InternalAuctionCreateController {

	private final AuctionCreateAuctionUseCase auctionCreateAuctionUseCase;

	@PostMapping("/{productId}/auctions/{sellerUUID}")
	public SuccessResponseDto<Long> createAuction (
		@PathVariable Long productId,
		@PathVariable String sellerUUID,
		@Valid @RequestBody ProductAuctionRequestDto productAuctionRequestDto
	) {
		return SuccessResponseDto.from(SuccessType.CREATED,
			auctionCreateAuctionUseCase.createAuction(productId, sellerUUID, productAuctionRequestDto));
	}
}
