package com.bugzero.rarego.boundedContext.product.in;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

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
import com.bugzero.rarego.boundedContext.product.domain.ProductCondition;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
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

	private final Long PRODUCT_ID = 1L;

	@Test
	@DisplayName("성공 - 상품 검수 생성 API가 정상적으로 호출되면 201 Created를 반환한다")
	void createProductInspection_success() throws Exception {
		// given
		String inspectorId = "admin-1";
		ProductInspectionRequestDto requestDto = new ProductInspectionRequestDto(
			1L, InspectionStatus.APPROVED, ProductCondition.MISB,
			"검수 승인 완료"
		);

		ProductInspectionResponseDto responseDto = ProductInspectionResponseDto.builder()
			.inspectionId(500L)
			.productId(1L)
			.newStatus(InspectionStatus.APPROVED)
			.productCondition(ProductCondition.MISB)
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
			.andExpect(jsonPath("$.data.productCondition").value("MISB"))
			.andDo(print()); // 요청/응답 상세 내역 출력
	}

	@Test
	@DisplayName("실패 - 상품 ID가 누락된 요청은 400 에러를 반환한다")
	void createInspection_fail_invalidProductId() throws Exception {
		// given
		ProductInspectionRequestDto invalidDto = new ProductInspectionRequestDto(
			null,
			InspectionStatus.APPROVED,
			ProductCondition.MISB,
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

	@Test
	@DisplayName("성공 - 특정 상품의 검수 내역 조회 시 200 OK와 상세 정보를 반환한다")
	void readInspection_Success() throws Exception {
		// given
		ProductInspectionResponseDto responseDto = ProductInspectionResponseDto.builder()
			.inspectionId(500L)
			.productId(PRODUCT_ID)
			.newStatus(InspectionStatus.APPROVED)
			.productCondition(ProductCondition.MISB)
			.reason("검수 완료")
			.createdAt(LocalDateTime.now())
			.build();

		given(productFacade.readInspection(PRODUCT_ID)).willReturn(responseDto);

		// when & then
		mockMvc.perform(get("/api/v1/inspections/{productId}", PRODUCT_ID)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.inspectionId").value(500L))
			.andExpect(jsonPath("$.data.productId").value(PRODUCT_ID))
			.andExpect(jsonPath("$.data.newStatus").value("APPROVED"))
			.andExpect(jsonPath("$.data.reason").value("검수 완료"))
			.andDo(print());
	}

	@Test
	@DisplayName("실패 - 해당 상품의 검수 내역이 없으면 404 Not Found를 반환한다")
	void readInspection_Fail_NotFound() throws Exception {
		// given
		given(productFacade.readInspection(anyLong()))
			.willThrow(new CustomException(ErrorType.INSPECTION_NOT_FOUND));

		// when & then
		mockMvc.perform(get("/api/v1/inspections/{productId}", PRODUCT_ID)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andDo(print());
	}

}