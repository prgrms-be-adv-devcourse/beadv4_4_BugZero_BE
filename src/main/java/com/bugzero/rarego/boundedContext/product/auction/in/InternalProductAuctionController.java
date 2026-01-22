package com.bugzero.rarego.boundedContext.product.auction.in;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.product.auction.app.AuctionCreateAuctionUseCase;
import com.bugzero.rarego.boundedContext.product.auction.app.AuctionUpdateAuctionUseCase;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/auctions")
@RequiredArgsConstructor
public class InternalProductAuctionController {

	private final AuctionCreateAuctionUseCase auctionCreateAuctionUseCase;
	private final AuctionUpdateAuctionUseCase auctionUpdateAuctionUseCase;

	@PostMapping("/{productId}/{publicId}")
	public SuccessResponseDto<Long> createAuction (
		@PathVariable Long productId,
		@PathVariable String publicId,
		@Valid @RequestBody ProductAuctionRequestDto productAuctionRequestDto
	) {
		return SuccessResponseDto.from(SuccessType.CREATED,
			auctionCreateAuctionUseCase.createAuction(productId, publicId, productAuctionRequestDto));
	}

	@PatchMapping("/{publicId}")
	public SuccessResponseDto<Long> updateAuction (
		@PathVariable String publicId,
		@Valid @RequestBody ProductAuctionUpdateDto productAuctionUpdateDto
	) {
		return SuccessResponseDto.from(SuccessType.OK,
			auctionUpdateAuctionUseCase.updateAuction(publicId, productAuctionUpdateDto));
	}
}
