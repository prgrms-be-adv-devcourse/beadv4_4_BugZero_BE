package com.bugzero.rarego.boundedContext.product.app;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import com.bugzero.rarego.shared.product.auction.out.AuctionApiClient;
import com.bugzero.rarego.shared.product.dto.ProductImageUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductUpdateResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductUpdateProductUseCase {
	private final ProductSupport productSupport;
	private final AuctionApiClient auctionApiClient;

	@Transactional
	public ProductUpdateResponseDto updateProduct(String  publicId, Long productId, ProductUpdateDto productUpdateDto) {
		//유효한 멤버인지 확인
		ProductMember seller = productSupport.verifyValidateMember(publicId);
		//유효한 상품인지 확인
		Product product = productSupport.verifyValidateProduct(productId);
		//수정 가능한 상품인지 확인
		productSupport.isAbleToChange(seller, product);
		//상품 이미지 순서 보장 정렬
		List<ProductImageUpdateDto> images = productSupport.normalizeUpdateImageOrder(
			productUpdateDto.productImageUpdateDtos()
		);
		//상품 정보 수정
		product.update(
			productUpdateDto.name(),
			productUpdateDto.category(),
			productUpdateDto.description(),
			images
		);

		//TODO경매 정보 수정 내부 api 호출하여 응답값을 성공적으로 받으면 커밋할 수 있도록 함.
		Long auctionId = auctionApiClient.updateAuction(publicId, productUpdateDto.productAuctionUpdateDto());

		return ProductUpdateResponseDto.builder()
			.productId(productId)
			.auctionId(auctionId)
			.build();
	}
}
