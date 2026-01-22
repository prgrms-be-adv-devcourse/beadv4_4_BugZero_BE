package com.bugzero.rarego.boundedContext.product.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import com.bugzero.rarego.shared.auction.out.AuctionApiClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductDeleteProductUseCase {

	private final ProductSupport productSupport;
	private final AuctionApiClient auctionApiClient;

	@Transactional
	public void deleteProduct(String  publicId, Long productId) {
		//유효한 멤버인지 확인
		ProductMember seller = productSupport.verifyValidateMember(publicId);
		//유효한 상품인지 확인
		Product product = productSupport.verifyValidateProduct(productId);
		//삭제 가능한 상품인지 확인
		productSupport.isAbleToDelete(seller, product);
		//상품 정보 소프트 삭제
		product.softDelete();
		//경매 정보 삭제 api 호출
		auctionApiClient.deleteAuction(publicId, productId);
	}
}
