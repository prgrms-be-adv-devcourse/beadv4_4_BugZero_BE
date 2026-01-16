package com.bugzero.rarego.boundedContext.member.in;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

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
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionFilterType;
import com.bugzero.rarego.shared.auction.dto.MyBidResponseDto;
import com.bugzero.rarego.shared.auction.dto.MySaleResponseDto;

@WebMvcTest(controllers = MemberController.class)
@Import(ResponseAspect.class)
@EnableAspectJAutoProxy
class MemberControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuctionFacade auctionFacade;

	@Test
	@DisplayName("GET /members/me/bids - 내 입찰 내역 조회 성공")
	@WithMockUser(username = "2")
	void getMyBids_success() throws Exception {
		// given
		Long memberId = 2L;
		MyBidResponseDto myBidDto = new MyBidResponseDto(
			1L, 1L, 10L, 10000L, LocalDateTime.now(), AuctionStatus.IN_PROGRESS, 10000L, LocalDateTime.now().plusDays(1)
		);

		PagedResponseDto<MyBidResponseDto> response = new PagedResponseDto<>(
			List.of(myBidDto), new PageDto(1, 10, 1, 1, false, false)
		);

		// Controller에서 auctionStatus 파라미터를 받으므로 eq(null) 사용
		given(auctionFacade.getMyBids(eq(memberId), eq(null), any(Pageable.class)))
			.willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/members/me/bids")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].bidAmount").value(10000))
			.andExpect(jsonPath("$.data[0].auctionStatus").value("IN_PROGRESS"));
	}

	@Test
	@DisplayName("GET /members/me/sales - 내 판매 목록 조회 성공")
	@WithMockUser(username = "2")
	void getMySales_success() throws Exception {
		// given
		Long memberId = 2L;
		MySaleResponseDto mySaleDto = MySaleResponseDto.builder()
			.auctionId(10L)
			.title("내 판매 상품")
			.currentPrice(20000)
			.bidCount(5)
			.tradeStatus(AuctionOrderStatus.PROCESSING)
			.build();

		PagedResponseDto<MySaleResponseDto> response = new PagedResponseDto<>(
			List.of(mySaleDto), new PageDto(1, 10, 1, 1, false, false)
		);

		given(auctionFacade.getMySales(eq(memberId), eq(AuctionFilterType.ALL), any(Pageable.class)))
			.willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/members/me/sales")
				.param("filter", "ALL")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].title").value("내 판매 상품"))
			.andExpect(jsonPath("$.data[0].bidCount").value(5))
			.andExpect(jsonPath("$.data[0].tradeStatus").value("PROCESSING"));
	}
}