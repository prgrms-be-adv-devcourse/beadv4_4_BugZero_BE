package com.bugzero.rarego.boundedContext.product.in;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bugzero.rarego.boundedContext.product.app.ProductFacade;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.shared.product.dto.ProductInspectionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductInspectionResponseDto;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ProductInspectionController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableAspectJAutoProxy
@Import(ResponseAspect.class)
class ProductInspectionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductFacade productFacade;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("상품 검수 생성 API가 정상적으로 호출되면 201 Created를 반환한다")
	void createProductInspection_success() throws Exception {
		// given
		String inspectorId = "admin-1";
		ProductInspectionRequestDto requestDto = new ProductInspectionRequestDto(
			1L, InspectionStatus.APPROVED, "검수 승인 완료"
		);

		ProductInspectionResponseDto responseDto = ProductInspectionResponseDto.builder()
			.inspectionId(500L)
			.productId(1L)
			.newStatus(InspectionStatus.APPROVED)
			.reason("검수 승인 완료")
			.build();

		// Facade의 동작을 Mocking (로직 검증은 UseCase 테스트에서 이미 했으므로 결과값만 정의)
		given(productFacade.createInspection(eq(inspectorId), any(ProductInspectionRequestDto.class)))
			.willReturn(responseDto);

		// when & then
		mockMvc.perform(post("/api/v1/inspections")
				.param("inspectorId", inspectorId) // RequestParam 처리
				.contentType(MediaType.APPLICATION_JSON) // JSON 요청임을 명시
				.content(objectMapper.writeValueAsString(requestDto))) // Body를 JSON으로 변환
			.andExpect(status().isCreated()) // SuccessResponseDto 구조상 HTTP 200 안에 SuccessType.CREATED 포함
			.andExpect(jsonPath("$.status").value(201)) // SuccessResponseDto 내부의 status 값 검증
			.andExpect(jsonPath("$.data.inspectionId").value(500L))
			.andExpect(jsonPath("$.data.newStatus").value("APPROVED"))
			.andDo(print()); // 요청/응답 상세 내역 출력
	}

	@Test
	@DisplayName("상품 ID가 누락된 요청은 400 에러를 반환한다")
	void createInspection_fail_invalidProductId() throws Exception {
		// given
		ProductInspectionRequestDto invalidDto = new ProductInspectionRequestDto(
			null,
			InspectionStatus.APPROVED,
			"검수 통과"
		);

		// when & then
		mockMvc.perform(post("/api/v1/inspections")
				.param("inspectorId", "admin-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidDto)))
			.andExpect(status().isBadRequest()) // 400 검증
			.andDo(print());
	}

}