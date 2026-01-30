package com.bugzero.rarego.bounded_context.product.auction.in;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

import com.bugzero.rarego.bounded_context.product.auction.app.AuctionCreateAuctionUseCase;
import com.bugzero.rarego.bounded_context.product.auction.app.AuctionUpdateAuctionUseCase;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(InternalProductAuctionController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableAspectJAutoProxy
@Import(ResponseAspect.class)
class InternalProductAuctionControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuctionCreateAuctionUseCase auctionCreateAuctionUseCase;

	@MockitoBean
	AuctionUpdateAuctionUseCase auctionUpdateAuctionUseCase;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("정상적인 경매 생성 요청 시 201 OK와 생성된 ID를 반환한다")
	void createAuction_Success() throws Exception {
		// given
		Long productId = 1L;
		String publicId = "1L";
		ProductAuctionRequestDto requestDto = ProductAuctionRequestDto.builder()
			.startPrice(10000)
			.durationDays(7)
			.build();

		given(auctionCreateAuctionUseCase.createAuction(eq(productId),eq(publicId), any(ProductAuctionRequestDto.class)))
			.willReturn(10L);

		// when & then
		mockMvc.perform(post("/api/v1/internal/auctions/{productId}/{publicId}", productId,publicId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data").value(10));
	}

	@Test
	@DisplayName("입찰 시작가가 100원 미만이면 400 Bad Request를 반환한다")
	void createAuction_Fail_MinPrice() throws Exception {
		// given
		Long productId = 1L;
		String sellerUUID = "1L";

		ProductAuctionRequestDto invalidDto = ProductAuctionRequestDto.builder()
			.startPrice(50) // @Min(100) 위반
			.durationDays(7)
			.build();

		// when & then
		mockMvc.perform(post("/api/v1/internal/auctions/{productId}/{publicId}", productId,sellerUUID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidDto)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("경매기간 설정을 범위 내 하지 않으면 400 Bad Request를 반환한다")
	void createAuction_Fail_MinDuration() throws Exception {
		// given
		Long productId = 1L;
		String sellerUUID = "1L";

		ProductAuctionRequestDto invalidDto = ProductAuctionRequestDto.builder()
			.startPrice(10000) // @Min(100) 위반
			.durationDays(100)
			.build();

		// when & then
		mockMvc.perform(post("/api/v1/internal/auctions/{productId}/{publicId}", productId,sellerUUID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidDto)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("정상적인 경매 수정 요청 시 200 OK와 생성된 ID를 반환한다")
	void updateAuction_Success() throws Exception {
		// given
		String publicId = "1L";
		ProductAuctionUpdateDto updateDto = ProductAuctionUpdateDto.builder()
			.auctionId(1L)
			.startPrice(10000)
			.durationDays(7)
			.build();

		given(auctionUpdateAuctionUseCase.updateAuction(eq(publicId), any(ProductAuctionUpdateDto.class)))
			.willReturn(10L);

		// when & then
		mockMvc.perform(patch("/api/v1/internal/auctions/{publicId}",publicId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateDto)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").value(10));
	}
}