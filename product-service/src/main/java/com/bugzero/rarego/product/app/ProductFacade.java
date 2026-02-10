package com.bugzero.rarego.product.app;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.product.domain.ProductMember;
import com.bugzero.rarego.product.domain.dto.ProductCreateResponseDto;
import com.bugzero.rarego.product.domain.dto.ProductInspectionRequestDto;
import com.bugzero.rarego.product.domain.dto.ProductInspectionResponseDto;
import com.bugzero.rarego.product.domain.dto.ProductResponseForInspectionDto;
import com.bugzero.rarego.product.domain.dto.ProductSearchForInspectionCondition;
import com.bugzero.rarego.product.domain.dto.ProductUpdateResponseDto;
import com.bugzero.rarego.shared.member.domain.MemberDto;
import com.bugzero.rarego.shared.product.dto.ProductCreateRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductUpdateDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductFacade {

	private final ProductCreateProductUseCase productCreateProductUseCase;
	private final ProductCreateInspectionUseCase productCreateInspectionUseCase;
	private final ProductSyncMemberUseCase productSyncMemberUseCase;
	private final ProductUpdateProductUseCase productUpdateProductUseCase;
	private final ProductDeleteProductUseCase productDeleteProductUseCase;
	private final ProductReadProductsForInspectionUseCase productReadProductsForInspectionUseCase;
	private final ProductReadInspectionUseCase productReadInspectionUseCase;

	//판매자용
	public ProductCreateResponseDto createProduct(String memberUUID, ProductCreateRequestDto dto) {
		return productCreateProductUseCase.createProduct(memberUUID, dto);
	}

	public ProductUpdateResponseDto updateProduct(String publicId, Long productId, ProductUpdateDto productUpdateDto) {
		return productUpdateProductUseCase.updateProduct(publicId, productId, productUpdateDto);
	}

	public void deleteProduct(String publicId, Long productId) {
		productDeleteProductUseCase.deleteProduct(publicId, productId);
	}

	//관리자용
	public ProductInspectionResponseDto createInspection(String memberUUID, ProductInspectionRequestDto dto) {
		return productCreateInspectionUseCase.createInspection(memberUUID, dto);
	}

	public ProductInspectionResponseDto readInspection(Long productId) {
		return productReadInspectionUseCase.readInspection(productId);
	}

	public PagedResponseDto<ProductResponseForInspectionDto> readProductsForInspection(
		ProductSearchForInspectionCondition condition, Pageable pageable) {
		return productReadProductsForInspectionUseCase.readProducts(condition, pageable);
	}

	//멤버 동기화
	public ProductMember syncMember(MemberDto member) {
		return productSyncMemberUseCase.syncMember(member);
	}

}
