package com.bugzero.rarego.boundedContext.product.app;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

@ExtendWith(MockitoExtension.class)
class ProductCreateInspectionUseCaseTest {
	@Mock
	private InspectionRepository inspectionRepository;
	@Mock
	private ProductSupport productSupport;

	@InjectMocks
	private ProductCreateInspectionUseCase useCase;

	@Captor
	private ArgumentCaptor<Inspection> inspectionCaptor;

	private final String ADMIN_UUID = "admin-uuid";
	private final Long ADMIN_INTERNAL_ID = 200L;
	private final Long PRODUCT_ID = 1L;

	private ProductMember commonAdmin;
	private ProductMember commonSeller;
	private Product commonProduct;

	@BeforeEach
	void setUp() {
		// 1. 관리자 셋업
		commonAdmin = ProductMember.builder()
			.id(ADMIN_INTERNAL_ID)
			.publicId(ADMIN_UUID)
			.build();

		// 2. 판매자 셋업 (정상 상태)
		commonSeller = ProductMember.builder()
			.id(100L)
			.deleted(false)
			.build();

		// 3. 상품 셋업 (검수 대기 상태)
		commonProduct = Product.builder()
			.seller(commonSeller)
			.inspectionStatus(InspectionStatus.PENDING)
			.productCondition(ProductCondition.INSPECTION)
			.build();
	}

	@Test
	@DisplayName("성공: 모든 검증을 통과하면 검수 정보가 저장되고 상품 상태가 동기화된다")
	void createInspection_Success() {
		// given
		ProductInspectionRequestDto requestDto = createRequest(InspectionStatus.APPROVED, "통과");
		Product spyProduct = spy(commonProduct);

		given(productSupport.verifyValidateProduct(PRODUCT_ID)).willReturn(spyProduct);
		given(productSupport.verifyValidateMember(ADMIN_UUID)).willReturn(commonAdmin);
		given(inspectionRepository.save(any(Inspection.class))).willAnswer(invocation -> {
			Inspection ins = invocation.getArgument(0);
			ReflectionTestUtils.setField(ins, "id", 500L);
			return ins;
		});

		// when
		ProductInspectionResponseDto response = useCase.createInspection(ADMIN_UUID, requestDto);

		// then
		verify(inspectionRepository).save(inspectionCaptor.capture());
		Inspection saved = inspectionCaptor.getValue();

		assertThat(response.inspectionId()).isEqualTo(500L);
		assertThat(saved.getInspectorId()).isEqualTo(ADMIN_INTERNAL_ID);
		verify(spyProduct).determineInspection(InspectionStatus.APPROVED);
		verify(spyProduct).determineProductCondition(ProductCondition.MISB);
	}

	@Nested
	@DisplayName("검수 생성 실패 케이스")
	class FailureCases {

		@Test
		@DisplayName("반려 시 사유가 없으면 예외가 발생한다")
		void fail_rejected_without_reason() {
			ProductInspectionRequestDto request = createRequest(InspectionStatus.REJECTED, null);

			assertThatThrownBy(() -> useCase.createInspection(ADMIN_UUID, request))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorType", ErrorType.INSPECTION_REJECT_REASON_REQUIRED);
		}

		@Test
		@DisplayName("상품의 판매자 정보가 없거나 탈퇴한 회원이면 예외가 발생한다")
		void fail_seller_not_found_or_deleted() {
			// given: 탈퇴한 판매자로 설정
			ProductMember deletedSeller = ProductMember.builder().deleted(true).build();
			commonProduct = Product.builder().seller(deletedSeller).build();

			given(productSupport.verifyValidateProduct(PRODUCT_ID)).willReturn(commonProduct);
			ProductInspectionRequestDto request = createRequest(InspectionStatus.APPROVED, "통과");

			// when & then
			assertThatThrownBy(() -> useCase.createInspection(ADMIN_UUID, request))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorType", ErrorType.MEMBER_NOT_FOUND);
		}

		@Test
		@DisplayName("이미 검수가 완료된 상품은 다시 검수할 수 없다")
		void fail_already_completed() {
			// given: 검수 상태는 틀렸지만, seller 정보는 정상인 상품을 준비해야 함
			Product completedProduct = Product.builder()
				.seller(commonSeller) // @BeforeEach에서 만든 정상 seller 주입 (필수!)
				.inspectionStatus(InspectionStatus.APPROVED)
				.productCondition(ProductCondition.MISB)
				.build();

			given(productSupport.verifyValidateProduct(PRODUCT_ID)).willReturn(completedProduct);
			ProductInspectionRequestDto request = createRequest(InspectionStatus.APPROVED, "통과");

			// when & then
			// 이제 seller 검증을 통과하고 checkedProductStatus()까지 도달하여 원하는 에러를 던집니다.
			assertThatThrownBy(() -> useCase.createInspection(ADMIN_UUID, request))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorType", ErrorType.INSPECTION_ALREADY_COMPLETED);
		}
	}

	// Helper: 반복되는 Request 생성을 분리
	private ProductInspectionRequestDto createRequest(InspectionStatus status, String reason) {
		return new ProductInspectionRequestDto(PRODUCT_ID, status, ProductCondition.MISB, reason);
	}
}