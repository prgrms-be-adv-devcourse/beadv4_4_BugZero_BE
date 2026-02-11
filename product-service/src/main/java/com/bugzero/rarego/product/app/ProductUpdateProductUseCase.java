package com.bugzero.rarego.product.app;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.product.domain.Product;
import com.bugzero.rarego.product.domain.ProductMember;
import com.bugzero.rarego.product.domain.dto.ProductUpdateResponseDto;
import com.bugzero.rarego.shared.auction.type.AuctionProductEventType;
import com.bugzero.rarego.shared.product.dto.ProductImageUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductUpdateDto;
import com.bugzero.rarego.shared.product.event.S3ImageConfirmEvent;
import com.bugzero.rarego.shared.product.event.S3ImageDeleteEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductUpdateProductUseCase {
	private final ProductSupport productSupport;
	private final ProductOutboxSupport productOutboxSupport;
	private final EventPublisher eventPublisher;

	@Transactional
	public ProductUpdateResponseDto updateProduct(String  publicId, Long productId, ProductUpdateDto dto) {
		//유효한 멤버인지 확인
		ProductMember seller = productSupport.verifyValidateMember(publicId);
		//유효한 상품인지 확인
		Product product = productSupport.verifyValidateProduct(productId);
		//수정 가능한 상품인지 확인
		productSupport.isAbleToChange(seller, product);
		//상품 이미지 순서 보장 정렬
		List<ProductImageUpdateDto> images = productSupport.normalizeUpdateImageOrder(
			dto.productImageUpdateDtos()
		);
		//상품 기본 정보 수정
		product.updateBasicInfo(
			dto.name(),
			dto.category(),
			dto.description()
		);
		//수정 중 삭제되는 이미지가 있다면 반환
		List<String> pathToDelete = product.removeOldImages(images);
		//수정 중 새롭게 등록되는 이미지가 있다면 반환
		List<String> pathToUpdate = product.processNewImages(images);
		//S3삭제 이벤트 발행
		eventPublisher.publish(new S3ImageDeleteEvent(pathToDelete));
		//S3등록 이벤트 발행
		eventPublisher.publish(new S3ImageConfirmEvent(pathToUpdate));

		//아웃박스에 저장
		productOutboxSupport.saveOutbox(productId, publicId,
			AuctionProductEventType.UPDATE, dto.productAuctionUpdateDto());

		return ProductUpdateResponseDto.builder()
			.productId(productId)
			.build();
	}
}
