package com.bugzero.rarego.boundedContext.product.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import com.bugzero.rarego.boundedContext.product.out.ProductMemberRepository;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductSupport {
	private final ProductMemberRepository productMemberRepository;
	private final ProductRepository productRepository;

	public ProductMember verifyValidateMember (String memberPublicId) {
		return productMemberRepository.findByPublicIdAndDeletedIsFalse(memberPublicId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));
	}

	public ProductMember verifyValidateMember (Long memberId) {
		return productMemberRepository.findById(memberId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));
	}

	public Product verifyValidateProduct (Long productId) {
		return productRepository.findByIdAndDeletedIsFalse(productId)
			.orElseThrow(() -> new CustomException(ErrorType.PRODUCT_NOT_FOUND));
	}

	//TODO 리팩토링 시 sellerId -> ProductMember seller로 변경
	public void isAbleToChange(ProductMember seller, Product product) {
		//해당 멤버의 상품인지 확인
		if (!product.isSeller(seller.getId())) {
			throw new CustomException(ErrorType.UNAUTHORIZED_SELLER);
		}
		//검수 완료 전의 상품인지 확인
		if(!product.isPending()) {
			throw new CustomException(ErrorType.INSPECTION_ALREADY_COMPLETED);
		}
	}
}
