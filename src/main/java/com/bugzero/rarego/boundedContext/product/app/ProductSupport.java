package com.bugzero.rarego.boundedContext.product.app;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import com.bugzero.rarego.boundedContext.product.out.ProductMemberRepository;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.product.dto.ProductImageRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductImageUpdateDto;

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

	//상품이미지를 한번에 불러와야할 때 FetchJoin 용
	public Product findByIdWithImages (Long productId) {
		return productRepository.findByIdWithImages(productId)
			.orElseThrow(() -> new CustomException(ErrorType.PRODUCT_NOT_FOUND));
	}

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

	public void isAbleToDelete(ProductMember seller, Product product) {
		//해당 멤버의 상품인지 확인
		if (!product.isSeller(seller.getId())) {
			throw new CustomException(ErrorType.UNAUTHORIZED_SELLER);
		}
		//검수 승인된 상품인지 확인
		if(product.isApproved()) {
			throw new CustomException(ErrorType.INSPECTION_ALREADY_COMPLETED);
		}
	}

	//이미지 순서 정규화
	//생성용
	public List<ProductImageRequestDto> normalizeCreateImageOrder(
		List<ProductImageRequestDto> dtos
	) {
		AtomicInteger index = new AtomicInteger(0);

		return dtos.stream()
			.sorted(Comparator.comparing(ProductImageRequestDto::sortOrder))
			.map(dto -> dto.withOrder(index.getAndIncrement()))
			.collect(Collectors.toList());
	}

	//수정용
	public List<ProductImageUpdateDto> normalizeUpdateImageOrder(
		List<ProductImageUpdateDto> dtos
	) {
		AtomicInteger index = new AtomicInteger(0);

		return dtos.stream()
			.sorted(Comparator.comparing(ProductImageUpdateDto::sortOrder))
			.map(dto -> dto.withOrder(index.getAndIncrement()))
			.collect(Collectors.toList());
	}

}
