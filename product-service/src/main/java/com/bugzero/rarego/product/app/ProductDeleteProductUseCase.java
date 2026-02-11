package com.bugzero.rarego.product.app;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.product.domain.Product;
import com.bugzero.rarego.product.domain.ProductImage;
import com.bugzero.rarego.product.domain.ProductMember;
import com.bugzero.rarego.shared.auction.type.AuctionProductEventType;
import com.bugzero.rarego.shared.product.event.S3ImageDeleteEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductDeleteProductUseCase {

	private final ProductSupport productSupport;
	private final ProductOutboxSupport productOutboxSupport;
	private final EventPublisher eventPublisher;

	@Transactional
	public void deleteProduct(String  publicId, Long productId) {
		//유효한 멤버인지 확인
		ProductMember seller = productSupport.verifyValidateMember(publicId);
		//유효한 상품인지 확인
		Product product = productSupport.findByIdWithImages(productId);
		//삭제 가능한 상품인지 확인
		productSupport.isAbleToDelete(seller, product);
		//상품 정보 소프트 삭제
		product.softDelete();
		//상품 이미지삭제 (S3에서도 삭제)
		List<String> pathToDelete = product.getImages().stream().map(ProductImage::getImageUrl).toList();
		eventPublisher.publish(new S3ImageDeleteEvent(pathToDelete));
		product.getImages().clear();

		//삭제 이벤트 아웃박스 저장
		productOutboxSupport.saveOutbox(productId, publicId, AuctionProductEventType.DELETE);
	}
}
