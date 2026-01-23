package com.bugzero.rarego.boundedContext.product.app;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.product.domain.Inspection;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.boundedContext.product.domain.ProductCondition;
import com.bugzero.rarego.boundedContext.product.out.InspectionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.product.dto.ProductInspectionResponseDto;

@ExtendWith(MockitoExtension.class)
class ProductReadInspectionUseCaseTest {
	@Mock
	private InspectionRepository inspectionRepository;

	@InjectMocks
	private ProductReadInspectionUseCase useCase;

	private final Long PRODUCT_ID = 1L;
	private final Long INSPECTION_ID = 500L;

	@Test
	@DisplayName("성공: 특정 상품의 검수 내역을 조회하여 DTO로 반환한다")
	void readInspection_Success() {
		// given
		LocalDateTime now = LocalDateTime.now();
		Inspection inspection = createInspectionFixture(now);

		given(inspectionRepository.findByProductId(PRODUCT_ID))
			.willReturn(Optional.of(inspection));

		// when
		ProductInspectionResponseDto response = useCase.readInspection(PRODUCT_ID);

		// then
		assertAll(
			() -> assertThat(response.inspectionId()).isEqualTo(INSPECTION_ID),
			() -> assertThat(response.productId()).isEqualTo(PRODUCT_ID),
			() -> assertThat(response.newStatus()).isEqualTo(InspectionStatus.APPROVED),
			() -> assertThat(response.productCondition()).isEqualTo(ProductCondition.MISB),
			() -> assertThat(response.reason()).isEqualTo("검수 완료"),
			() -> assertThat(response.createdAt()).isEqualTo(now),
			() -> assertThat(response.updatedAt()).isEqualTo(now)
		);

		verify(inspectionRepository, times(1)).findByProductId(PRODUCT_ID);
	}

	@Test
	@DisplayName("실패: 해당 상품의 검수 내역이 없으면 INSPECTION_NOT_FOUND 예외를 던진다")
	void readInspection_Fail_NotFound() {
		// given
		given(inspectionRepository.findByProductId(anyLong()))
			.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> useCase.readInspection(PRODUCT_ID))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.INSPECTION_NOT_FOUND);
	}

	// --- Helper Method ---
	private Inspection createInspectionFixture(LocalDateTime time) {
		// 실제 엔티티 구조에 맞게 생성 (ID와 날짜는 Reflection으로 주입)
		Inspection inspection = Inspection.builder()
			.inspectionStatus(InspectionStatus.APPROVED)
			.productCondition(ProductCondition.MISB)
			.reason("검수 완료")
			.build();

		ReflectionTestUtils.setField(inspection, "id", INSPECTION_ID);
		ReflectionTestUtils.setField(inspection, "createdAt", time);
		ReflectionTestUtils.setField(inspection, "updatedAt", time);

		return inspection;
	}
}