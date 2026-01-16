package com.bugzero.rarego.boundedContext.auction.in;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.auction.dto.BidLogResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidRequestDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AuctionController.class)
@Import(ResponseAspect.class)
@EnableAspectJAutoProxy
class AuctionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuctionFacade auctionFacade;

	@Test
	@DisplayName("POST /auctions/{id}/bids - 입찰 생성 성공")
	@WithMockUser(username = "2", roles = "USER")
	void createBid_success() throws Exception {
		// given
		Long auctionId = 1L;
		Long memberId = 2L;
		Long bidAmount = 10000L;
		BidRequestDto requestDto = new BidRequestDto(bidAmount);

		BidResponseDto bidResponse = new BidResponseDto(
			100L, auctionId, UUID.randomUUID().toString(), LocalDateTime.now(), bidAmount, bidAmount
		);

		SuccessResponseDto<BidResponseDto> successResponse = SuccessResponseDto.from(
			SuccessType.CREATED,
			bidResponse
		);

		given(auctionFacade.createBid(eq(auctionId), eq(memberId), eq(bidAmount.intValue())))
			.willReturn(successResponse);

		// when & then
		mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto))
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value(SuccessType.CREATED.getHttpStatus()))
			.andExpect(jsonPath("$.data.bidAmount").value(bidAmount));
	}

	@Test
	@DisplayName("GET /auctions/{id}/bids - 경매 입찰 기록 조회 성공")
	@WithMockUser
	void getBids_success() throws Exception {
		// given
		Long auctionId = 1L;
		BidLogResponseDto logDto = new BidLogResponseDto(
			10L, "user_***", LocalDateTime.now(), 50000
		);

		PagedResponseDto<BidLogResponseDto> response = new PagedResponseDto<>(
			List.of(logDto), new PageDto(1, 10, 1, 1, false, false)
		);

		given(auctionFacade.getBidLogs(eq(auctionId), any(Pageable.class)))
			.willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/auctions/{auctionId}/bids", auctionId)
				.param("page", "0")
				.param("size", "10"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].publicId").value("user_***"))
			.andExpect(jsonPath("$.data[0].bidAmount").value(50000));
	}

	@Test
	@DisplayName("실패: 유효성 검사 실패 시 400 에러코드 반환")
	@WithMockUser
	void createBid_fail_validation() throws Exception {
		// given
		Long auctionId = 1L;
		BidRequestDto invalidRequest = new BidRequestDto(-500L);

		// when & then
		mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest))
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	@DisplayName("실패: 비즈니스 예외 발생 시 404 에러코드 반환")
	@WithMockUser(username = "2")
	void createBid_fail_business_exception() throws Exception {
		// given
		Long auctionId = 999L;
		Long memberId = 2L;
		Long bidAmount = 10000L;
		BidRequestDto requestDto = new BidRequestDto(bidAmount);

		given(auctionFacade.createBid(eq(auctionId), eq(memberId), eq(bidAmount.intValue())))
			.willThrow(new CustomException(ErrorType.AUCTION_NOT_FOUND));

		// when & then
		mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto))
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
			.andDo(print())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(ErrorType.AUCTION_NOT_FOUND.getHttpStatus()));
	}
}