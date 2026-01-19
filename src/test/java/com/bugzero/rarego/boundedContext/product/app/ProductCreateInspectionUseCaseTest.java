package com.bugzero.rarego.boundedContext.product.app;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
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
import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import com.bugzero.rarego.boundedContext.product.out.InspectionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.product.dto.ProductInspectionRequestDto;

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

	private final String ADMIN_ID = "admin-uuid";
	private final Long PRODUCT_ID = 1L;
	private final Long SELLER_ID = 100L;

	@Test
	@DisplayName("검수 생성 시 입력받은 정보가 엔티티로 올바르게 변환되어 저장된다")
	void createInspection_verify_saved_data() {
		// given
		ProductInspectionRequestDto requestDto = new ProductInspectionRequestDto(
			PRODUCT_ID,
			InspectionStatus.APPROVED,
			"검수 통과입니다."
		);

		Product product = Product.builder()
			.sellerId(SELLER_ID)
			.inspectionStatus(InspectionStatus.PENDING)
			.build();

		Product spyProduct = spy(product);

		ProductMember mockSeller = ProductMember.builder().id(SELLER_ID).build();
		ProductMember mockAdmin = ProductMember.builder().id(200L).publicId(ADMIN_ID).build();

		// Repository 저장 시 반환값 지정
		given(inspectionRepository.save(any(Inspection.class))).willAnswer(invocation -> {
			Inspection ins = invocation.getArgument(0);
			ReflectionTestUtils.setField(ins, "id", 500L);
			return ins;
		});

		given(productSupport.verifyValidateProduct(PRODUCT_ID)).willReturn(spyProduct);
		given(productSupport.verifyValidateMember(SELLER_ID)).willReturn(mockSeller);
		given(productSupport.verifyValidateMember(ADMIN_ID)).willReturn(mockAdmin);

		// when
		useCase.createInspection(ADMIN_ID, requestDto);

		// then
		// 1. 저장되는 데이터 캡처 및 검증
		verify(inspectionRepository).save(inspectionCaptor.capture());
		Inspection savedInspection = inspectionCaptor.getValue();

		assertThat(savedInspection.getProduct()).isEqualTo(spyProduct);
		assertThat(savedInspection.getInspectorId()).isEqualTo(200L); // admin.getId()
		assertThat(savedInspection.getStatus()).isEqualTo(InspectionStatus.APPROVED);
		assertThat(savedInspection.getReason()).isEqualTo("검수 통과입니다.");

		// 2. 부가 로직 검증 (상품 상태 동기화)
		verify(spyProduct).determineInspection(InspectionStatus.APPROVED);
	}

	@Test
	@DisplayName("검수 반려 시 사유가 없으면 예외가 발생한다")
	void createInspection_fail_rejected_without_reason() {
		// given
		ProductInspectionRequestDto requestDto = new ProductInspectionRequestDto(
			PRODUCT_ID,
			InspectionStatus.REJECTED,
			null // 사유 누락
		);

		// when & then
		assertThatThrownBy(() -> useCase.createInspection(ADMIN_ID, requestDto))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.INSPECTION_REJECT_REASON_REQUIRED);

		// 로직의 가장 상단에서 걸리므로 Repository 호출이 전혀 없어야 함
		verifyNoInteractions(inspectionRepository);
	}

	@Test
	@DisplayName("이미 검수가 완료된 상품은 다시 검수할 수 없다")
	void createInspection_fail_already_completed() {
		// given
		ProductInspectionRequestDto requestDto = new ProductInspectionRequestDto(
			PRODUCT_ID, InspectionStatus.APPROVED, "사유"
		);

		// 이미 APPROVED 상태인 상품 준비
		Product alreadyDoneProduct = Product.builder()
			.inspectionStatus(InspectionStatus.APPROVED)
			.build();

		given(productSupport.verifyValidateProduct(PRODUCT_ID)).willReturn(alreadyDoneProduct);

		// when & then
		assertThatThrownBy(() -> useCase.createInspection(ADMIN_ID, requestDto))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.INSPECTION_ALREADY_COMPLETED);
	}
}