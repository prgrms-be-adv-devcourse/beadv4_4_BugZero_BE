package com.bugzero.rarego.boundedContext.auction.in;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.exception.GlobalExceptionHandler;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.auction.dto.AuctionDetailResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidLogResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidRequestDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AuctionController.class)
@Import(ResponseAspect.class)
@EnableAspectJAutoProxy
class AuctionControllerTest {

	private MockMvc mockMvc;

	@Autowired
	private AuctionController auctionController;

	private ObjectMapper objectMapper = new ObjectMapper();

	@MockitoBean
	private AuctionFacade auctionFacade;

	@BeforeEach
	void setup() {
		mockMvc = MockMvcBuilders.standaloneSetup(auctionController)
			.setCustomArgumentResolvers(
				new PageableHandlerMethodArgumentResolver(),
				// UserDetails resolver
				new HandlerMethodArgumentResolver() {
					@Override
					public boolean supportsParameter(MethodParameter parameter) {
						return UserDetails.class.isAssignableFrom(parameter.getParameterType());
					}

					@Override
					public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
						NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
						return User.withUsername("2")
							.password("password")
							.roles("USER")
							.build();
					}
				},
				// MemberPrincipal resolver
				new HandlerMethodArgumentResolver() {
					@Override
					public boolean supportsParameter(MethodParameter parameter) {
						return MemberPrincipal.class.isAssignableFrom(parameter.getParameterType());
					}

					@Override
					public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
						NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
						return new MemberPrincipal("1", "USER");
					}
				}
			)
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	@DisplayName("POST /auctions/{id}/bids - 입찰 생성 성공")
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
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value(SuccessType.CREATED.getHttpStatus()))
			.andExpect(jsonPath("$.data.bidAmount").value(bidAmount));
	}

	@Test
	@DisplayName("GET /auctions/{id}/bids - 경매 입찰 기록 조회 성공")
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
	void createBid_fail_validation() throws Exception {
		// given
		Long auctionId = 1L;
		BidRequestDto invalidRequest = new BidRequestDto(-500L);

		// when & then
		mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	@DisplayName("실패: 비즈니스 예외 발생 시 404 에러코드 반환")
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
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(ErrorType.AUCTION_NOT_FOUND.getHttpStatus()));
	}

	@Test
	@DisplayName("경매 상세 조회 성공")
	void getAuctionDetail_success() throws Exception {
		// given
		Long auctionId = 100L;
		AuctionDetailResponseDto responseDto = new AuctionDetailResponseDto(
			auctionId,
			50L,
			LocalDateTime.now(),
			LocalDateTime.now().plusDays(1),
			AuctionStatus.IN_PROGRESS,
			10000,
			20000,
			1000
		);

		given(auctionFacade.getAuctionDetail(auctionId))
			.willReturn(SuccessResponseDto.from(SuccessType.OK, responseDto));

		// when & then
		mockMvc.perform(get("/api/v1/auctions/{auctionId}", auctionId))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(auctionId))
			.andExpect(jsonPath("$.data.currentPrice").value(20000));
	}

	@Test
	@DisplayName("낙찰 기록(주문) 상세 조회 성공 - 인증된 사용자")
	void getAuctionOrder_success() throws Exception {
		// given
		Long auctionId = 100L;
		Long memberId = 1L;  // MemberPrincipal resolver에서 "1"을 반환

		AuctionOrderResponseDto responseDto = new AuctionOrderResponseDto(
			7001L, auctionId, "BUYER", AuctionOrderStatus.PROCESSING, "결제 대기중",
			LocalDateTime.now(),
			new AuctionOrderResponseDto.ProductInfo("Lego Titanic", "img.jpg"),
			new AuctionOrderResponseDto.PaymentInfo(150000, 15000, 135000),
			new AuctionOrderResponseDto.TraderInfo("SellerNick", "010-1234-5678"),
			new AuctionOrderResponseDto.ShippingInfo(null, null, null)
		);

		given(auctionFacade.getAuctionOrder(eq(auctionId), eq(memberId)))
			.willReturn(SuccessResponseDto.from(SuccessType.OK, responseDto));

		// when & then
		mockMvc.perform(get("/api/v1/auctions/{auctionId}/order", auctionId))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.orderId").value(7001L))
			.andExpect(jsonPath("$.data.viewerRole").value("BUYER"));
	}
}