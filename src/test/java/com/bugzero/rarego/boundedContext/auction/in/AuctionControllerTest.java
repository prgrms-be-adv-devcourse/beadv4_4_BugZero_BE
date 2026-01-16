package com.bugzero.rarego.boundedContext.auction.in;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
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
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.exception.GlobalExceptionHandler;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.auction.dto.BidRequestDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;

import tools.jackson.databind.ObjectMapper;


@WebMvcTest(controllers = AuctionController.class)
@Import(ResponseAspect.class)
@EnableAspectJAutoProxy
class AuctionControllerTest {

	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AuctionController auctionController;

	@MockitoBean
	private AuctionFacade auctionFacade;

	@BeforeEach
	void setup() {
		mockMvc = MockMvcBuilders.standaloneSetup(auctionController)
			.setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
				@Override
				public boolean supportsParameter(MethodParameter parameter) {
					return UserDetails.class.isAssignableFrom(parameter.getParameterType());
				}

				@Override
				public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
					NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
					return User.withUsername("user_public_2")
						.password("password")
						.roles("USER")
						.build();
				}
			})
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	@DisplayName("성공: 유효한 입찰 요청 시 HTTP 201과 입찰 정보를 반환한다")
	void createBid_success() throws Exception {
		// given
		Long auctionId = 1L;
		String memberPublicId = "user_public_2";
		Long bidAmount = 10000L;

		BidRequestDto requestDto = new BidRequestDto(bidAmount);

		BidResponseDto bidResponse = new BidResponseDto(
			100L,
			auctionId,
			UUID.randomUUID().toString(),
			LocalDateTime.now(),
			bidAmount,
			bidAmount
		);

		SuccessResponseDto<BidResponseDto> successResponse = SuccessResponseDto.from(
			SuccessType.CREATED,
			bidResponse
		);

		given(auctionFacade.createBid(eq(auctionId), eq(memberPublicId), eq(bidAmount.intValue())))
			.willReturn(successResponse);

		// when & then
		mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isCreated()) // 성공 시에는 201 Created (유지)
			.andExpect(jsonPath("$.status").value(SuccessType.CREATED.getHttpStatus()))
			.andExpect(jsonPath("$.message").value(SuccessType.CREATED.getMessage()))
			.andExpect(jsonPath("$.data.auctionId").value(auctionId));
	}

	@Test
	@DisplayName("실패: 입찰 금액이 음수이거나 0인 경우 HTTP 200(Body:400)을 반환한다")
	void createBid_fail_validation() throws Exception {
		// given
		Long auctionId = 1L;
		BidRequestDto invalidRequest = new BidRequestDto(-500L);

		// when & then
		mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andDo(print())
			// [변경] GlobalExceptionHandler가 에러 발생 시에도 200 OK를 리턴하므로 isOk()로 검증
			.andExpect(status().isOk())
			// 대신 JSON Body 내부의 status 값이 400인지 확인
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	@DisplayName("실패: 경매가 존재하지 않거나 종료된 경우 HTTP 200(Body:404)을 반환한다")
	void createBid_fail_business_exception() throws Exception {
		// given
		Long auctionId = 999L;
		String memberPublicId = "user_public_2";
		Long bidAmount = 10000L;
		BidRequestDto requestDto = new BidRequestDto(bidAmount);

		// Mocking: 예외 발생
		given(auctionFacade.createBid(eq(auctionId), eq(memberPublicId), eq(bidAmount.intValue())))
			.willThrow(new CustomException(ErrorType.AUCTION_NOT_FOUND));

		// when & then
		mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			// [변경] isNotFound() -> isOk()
			.andExpect(status().isOk())
			// JSON Body 내부의 status 코드로 404 검증
			.andExpect(jsonPath("$.status").value(ErrorType.AUCTION_NOT_FOUND.getHttpStatus()))
			.andExpect(jsonPath("$.message").value(ErrorType.AUCTION_NOT_FOUND.getMessage()));
	}
}
