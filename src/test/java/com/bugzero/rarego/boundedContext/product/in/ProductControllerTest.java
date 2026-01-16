package com.bugzero.rarego.boundedContext.product.in;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

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
import com.bugzero.rarego.boundedContext.product.domain.Category;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductImageRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductResponseDto;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableAspectJAutoProxy
@Import(ResponseAspect.class)
class ProductControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductFacade productFacade;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("성공 - 올바른 상품 정보와 memberId가 전달되면 201 응답을 반환한다")
	void createProduct_success() throws Exception {
		// given
		String memberId = "1L";
		ProductRequestDto requestDto = new ProductRequestDto(
			"스타워즈 레고",
			Category.스타워즈,
			"한정판 스타워즈 레고입니다.",
			new ProductAuctionRequestDto(10000, 7),
			List.of(new ProductImageRequestDto("https://s3.image.com/test.jpg", 1))
		);

		ProductResponseDto responseDto = ProductResponseDto.builder()
			.productId(100L)
			.auctionId(1)
			.inspectionStatus(InspectionStatus.PENDING)
			.build();

		given(productFacade.createProduct(memberId, requestDto)).willReturn(responseDto);

		// when & then
		mockMvc.perform(post("/api/v1/products")
				.param("memberId", String.valueOf(memberId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.productId").value(100L))
			.andExpect(jsonPath("$.data.inspectionStatus").value("PENDING"))
			.andDo(print()); // 요청/응답 로그 출력
	}

	@Test
	@DisplayName("실패 - 상품명이 비어있으면 400 Bad Request를 반환한다")
	void createProduct_fail_invalidName() throws Exception {
		ProductRequestDto invalidRequest = new ProductRequestDto(
			"", // @NotBlank 위반
			Category.스타워즈,
			"설명",
			new ProductAuctionRequestDto(1000, 7),
			List.of(new ProductImageRequestDto("url", 1))
		);

		// when & then
		mockMvc.perform(post("/api/v1/products")
				.param("memberId", "1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andExpect(status().isBadRequest())
			.andDo(print());
	}

	@Test
	@DisplayName("실패 - 경매 기간이 범위를 벗어나면 400 에러를 반환한다")
	void createProduct_fail_invalidDuration() throws Exception {
		// given: durationDays를 31로 설정 (@Max(30) 위반)
		ProductAuctionRequestDto invalidAuction = new ProductAuctionRequestDto(
			1000,
			31);
		ProductRequestDto request = new ProductRequestDto("스타워즈 시리즈",
			Category.스타워즈,
			"설명",
			invalidAuction,
			List.of(new ProductImageRequestDto("url", 1)));

		// when & then
		mockMvc.perform(post("/api/v1/products")
				.param("memberId", "1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andDo(print());
	}
}