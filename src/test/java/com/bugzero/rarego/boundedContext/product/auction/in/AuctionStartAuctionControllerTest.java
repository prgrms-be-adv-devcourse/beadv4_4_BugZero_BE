package com.bugzero.rarego.boundedContext.product.auction.in;

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

import com.bugzero.rarego.boundedContext.product.auction.app.AuctionDetermineStartAuctionUseCase;
import com.bugzero.rarego.global.aspect.ResponseAspect;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AuctionStartAuctionController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableAspectJAutoProxy
@Import(ResponseAspect.class)
class AuctionStartAuctionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuctionDetermineStartAuctionUseCase auctionDetermineStartAuctionUseCase;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("성공 - 시작 시간 확정 요청 시 200 OK와 경매 ID를 반환한다")
	void createAuction_Success() throws Exception {
		// given
		Long auctionId = 1L;

		given(auctionDetermineStartAuctionUseCase.determineStartAuction(eq(auctionId)))
			.willReturn(auctionId);

		// when & then
		mockMvc.perform(patch("/api/v1/auctions/{auctionId}/startTime", auctionId) // 1. patch 사용 및 경로 수정
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk()) // 2. isCreated() 대신 isOk() 사용
			.andExpect(jsonPath("$.status").value(200)) // SuccessResponseDto 구조 검증
			.andExpect(jsonPath("$.data").value(auctionId))
			.andDo(print());
	}

}