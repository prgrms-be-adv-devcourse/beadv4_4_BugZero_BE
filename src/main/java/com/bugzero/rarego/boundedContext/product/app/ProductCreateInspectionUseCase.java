package com.bugzero.rarego.boundedContext.product.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.product.domain.Inspection;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductCondition;
import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import com.bugzero.rarego.boundedContext.product.out.InspectionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.product.dto.ProductInspectionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductInspectionResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductCreateInspectionUseCase {
	private final InspectionRepository inspectionRepository;
	private final ProductSupport productSupport;

	@Transactional
	public ProductInspectionResponseDto createInspection(String inspectorId, ProductInspectionRequestDto dto) {
		//반려 시 이유가 있는지 확인
		checkedReason(dto);
		//유효한 상품인지 확인
		Product product = productSupport.verifyValidateProduct(dto.productId());
		//유효한 판매자인지 확인(TODO추후 아래 verifyValidateSeller 메서드로 변경예정)
		ProductMember seller = productSupport.verifyValidateMember(product.getSellerId());
		//이미 검수가 끝난 상품인지 확인
		checkedProductStatus(product);
		//유효한 관리자인지 확인
		ProductMember admin = productSupport.verifyValidateMember(inspectorId);

		Inspection inspection = inspectionRepository.save(dto.toEntity(product, seller, admin.getId()));

		//상품데이터의 검수 상태도 동기화
		product.determineInspection(dto.status());
		//상품데이터의 상품상태도 동기화
		product.determineProductCondition(dto.productCondition());

		return ProductInspectionResponseDto.builder()
			.inspectionId(inspection.getId())
			.productId(inspection.getProduct().getId())
			.newStatus(inspection.getInspectionStatus())
			.reason(inspection.getReason())
			.build();
	}

	private void checkedReason(ProductInspectionRequestDto dto) {
		if (dto.status() == InspectionStatus.REJECTED && dto.reason() == null) {
			throw new CustomException(ErrorType.INSPECTION_REJECT_REASON_REQUIRED);
		}
	}

	private void checkedProductStatus(Product product) {
		// 검수 상태가 대기중이 아니거나 상품 상태가 검수 예정 중인 경우가 아니라면 검수가 이미 완료된 것이기 때문에 예외 발생
		if (product.getInspectionStatus() != InspectionStatus.PENDING
			|| product.getProductCondition() != ProductCondition.INSPECTION) {
			throw new CustomException(ErrorType.INSPECTION_ALREADY_COMPLETED);
		}
	}

	//TODO 추후 Product에서 Long sellerId -> ProductMember seller 변경 시 연관관계 매핑을 통해 seller 객체를 가지고 올 수 있도록 변경.
	// 그렇게 seller 객체를 가져올 때 만약 seller데이터가 삭제되었다면 Null 값을 반환하게 됨. 따라서 굳이  verifyValidateMember(Long memberId)
	// 를 통해 멤버 존재 여부를 따로 확인할 필요 없어짐. 따라서 추후 아래 코드로 변경

	// private Product verifyValidateSeller(Long productId) {
	// 	//유효한 상품인지 확인
	// 	Product product = productSupport.verifyValidateProduct(productId);
	//
	// 	//
	// 	ProductMember seller = product.getSeller();
	// 	if (seller == null || seller.isDeleted) {
	// 		throw new CustomException(ErrorType.MEMBER_NOT_FOUND);
	// 	}
	//
	// 	return product;
	// }
}
