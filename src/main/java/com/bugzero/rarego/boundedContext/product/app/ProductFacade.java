package com.bugzero.rarego.boundedContext.product.app;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.shared.member.domain.MemberDto;
import com.bugzero.rarego.shared.product.dto.ProductInspectionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductInspectionResponseDto;
import com.bugzero.rarego.shared.product.dto.ProductRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductRequestResponseDto;
import com.bugzero.rarego.shared.product.dto.ProductResponseForInspectionDto;
import com.bugzero.rarego.shared.product.dto.ProductSearchForInspectionCondition;
import com.bugzero.rarego.shared.product.dto.ProductUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductUpdateResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductFacade {

	private final ProductCreateProductUseCase productCreateProductUseCase;
	private final ProductCreateInspectionUseCase productCreateInspectionUseCase;
	private final ProductSyncMemberUseCase productSyncMemberUseCase;
	private final ProductUpdateProductUseCase productUpdateProductUseCase;
	private final ProductDeleteProductUseCase productDeleteProductUseCase;
	private final ProductReadProductForInspectionUseCase productReadProductForInspectionUseCase;
	private final ProductReadInspectionUseCase productReadInspectionUseCase;

	//판매자용
	public ProductRequestResponseDto createProduct(String memberUUID, ProductRequestDto dto) {
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
		return productReadProductForInspectionUseCase.readProducts(condition, pageable);
	}

	//멤버 동기화
	public ProductMember syncMember(MemberDto member) {
		return productSyncMemberUseCase.syncMember(member);
	}


}
