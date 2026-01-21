package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistAddResponseDto;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistRemoveResponseDto;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.exception.GlobalExceptionHandler;
import com.bugzero.rarego.global.response.*;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.auction.dto.*;
import com.bugzero.rarego.support.WithMockMemberPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;


@ExtendWith(MockitoExtension.class)
class AuctionControllerTest {

	private MockMvc mockMvc;

	@InjectMocks
	private AuctionController auctionController;

	@Mock
	private AuctionFacade auctionFacade;

	private ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setup() {
		mockMvc = MockMvcBuilders.standaloneSetup(auctionController)
			.setCustomArgumentResolvers(
				new PageableHandlerMethodArgumentResolver(),
				new HandlerMethodArgumentResolver() {
					@Override
					public boolean supportsParameter(MethodParameter parameter) {
						return MemberPrincipal.class.isAssignableFrom(parameter.getParameterType());
					}

					@Override
					public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
						NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
						// Principal의 publicId를 "1"로 설정 (String)
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
		String memberPublicId = "1";
		Long bidAmount = 10000L;
		BidRequestDto requestDto = new BidRequestDto(bidAmount);

		BidResponseDto bidResponse = new BidResponseDto(
			100L, auctionId, memberPublicId, LocalDateTime.now(), bidAmount, 11000L
		);

		SuccessResponseDto<BidResponseDto> successResponse = SuccessResponseDto.from(
			SuccessType.CREATED,
			bidResponse
		);

		// [수정] memberId(Long) -> memberPublicId(String)
		given(auctionFacade.createBid(eq(auctionId), eq(memberPublicId), eq(bidAmount.intValue())))
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
			// GlobalExceptionHandler에서 ResponseEntity를 반환하므로 실제 상태코드 검증
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	@DisplayName("실패: 비즈니스 예외 발생 시 404 에러코드 반환")
	void createBid_fail_business_exception() throws Exception {
		// given
		Long auctionId = 999L;
		String memberPublicId = "1"; // [수정] String 타입
		Long bidAmount = 10000L;
		BidRequestDto requestDto = new BidRequestDto(bidAmount);

		// [수정] memberId(Long) -> memberPublicId(String)
		given(auctionFacade.createBid(eq(auctionId), eq(memberPublicId), eq(bidAmount.intValue())))
			.willThrow(new CustomException(ErrorType.AUCTION_NOT_FOUND));

		// when & then
		mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			// GlobalExceptionHandler에서 ResponseEntity를 반환하므로 실제 상태코드 검증
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(ErrorType.AUCTION_NOT_FOUND.getHttpStatus()));
	}

	@Test
	@DisplayName("경매 상세 조회 성공")
	void getAuctionDetail_success() throws Exception {
		// given
		Long auctionId = 100L;
		String memberPublicId = "1"; // [수정] String 타입

		AuctionDetailResponseDto responseDto = new AuctionDetailResponseDto(
			auctionId,
			50L,
			AuctionStatus.IN_PROGRESS,
			LocalDateTime.now(),
			LocalDateTime.now().plusDays(1),
			3600L,
			new AuctionDetailResponseDto.PriceInfo(10000, 20000, 1000),
			new AuctionDetailResponseDto.BidInfo(true, 21000, null, false),
			new AuctionDetailResponseDto.MyParticipationInfo(false, null)
		);

		// [수정] memberId(Long) -> memberPublicId(String)
		given(auctionFacade.getAuctionDetail(eq(auctionId), eq(memberPublicId)))
			.willReturn(SuccessResponseDto.from(SuccessType.OK, responseDto));

		// when & then
		mockMvc.perform(get("/api/v1/auctions/{auctionId}", auctionId))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.auctionId").value(auctionId))
			.andExpect(jsonPath("$.data.price.currentPrice").value(20000))
			.andExpect(jsonPath("$.data.bid.canBid").value(true));
	}

	@Test
	@DisplayName("낙찰 기록(주문) 상세 조회 성공 - 인증된 사용자")
	void getAuctionOrder_success() throws Exception {
		// given
		Long auctionId = 100L;
		String memberPublicId = "1"; // [수정] String 타입

		AuctionOrderResponseDto responseDto = new AuctionOrderResponseDto(
			7001L, auctionId, "BUYER", AuctionOrderStatus.PROCESSING, "결제 대기중",
			LocalDateTime.now(),
			new AuctionOrderResponseDto.ProductInfo("Lego Titanic", "img.jpg"),
			new AuctionOrderResponseDto.PaymentInfo(150000, 15000, 135000),
			new AuctionOrderResponseDto.TraderInfo("SellerNick", "010-1234-5678"),
			new AuctionOrderResponseDto.ShippingInfo(null, null, null)
		);

		// [수정] memberId(Long) -> memberPublicId(String)
		given(auctionFacade.getAuctionOrder(eq(auctionId), eq(memberPublicId)))
			.willReturn(SuccessResponseDto.from(SuccessType.OK, responseDto));

		// when & then
		mockMvc.perform(get("/api/v1/auctions/{auctionId}/order", auctionId))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.orderId").value(7001L))
			.andExpect(jsonPath("$.data.viewerRole").value("BUYER"));
	}

	@Test
	@DisplayName("GET /auctions - 경매 목록 검색 (조건 매핑 확인)")
	void getAuctions_success() throws Exception {
		// given
		// Mock 응답 데이터
		// (AuctionListResponseDto는 기존에 정의된 것을 사용한다고 가정)
        /* AuctionListResponseDto dto = ...;
           PagedResponseDto<AuctionListResponseDto> response = ...;
           given(auctionFacade.getAuctions(any(AuctionSearchCondition.class), any(Pageable.class)))
               .willReturn(response);
        */

		// *참고: DTO 객체 생성이 번거로우면 verify로 호출 여부만 검증해도 컨트롤러 테스트로는 충분합니다.

		// when
		mockMvc.perform(get("/api/v1/auctions")
				.param("keyword", "Lego")
				.param("category", "TOY")
				.param("sort", "CLOSING_SOON"))
			.andExpect(status().isOk());

		// then: 파라미터가 Condition 객체로 잘 변환되어 Facade로 전달되었는지 검증
		verify(auctionFacade).getAuctions(argThat(condition ->
			condition.getKeyword().equals("Lego") &&
				condition.getCategory().toString().equals("TOY") &&
				condition.getSort().equals("CLOSING_SOON")
		), any(Pageable.class));
	}
  
  @Test
    @DisplayName("성공: 관심 경매 등록 시 HTTP 200과 등록 정보를 반환한다")
    @WithMockMemberPrincipal(publicId = "test-public-id")
    void addBookmark_success() throws Exception {
        // given
        Long auctionId = 1L;
        WishlistAddResponseDto responseDto = WishlistAddResponseDto.of(true, auctionId);

        // any(String.class) 사용
        given(auctionFacade.addBookmark(any(String.class), eq(auctionId)))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/api/v1/auctions/{auctionId}/bookmarks", auctionId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.bookmarked").value(true))
                .andExpect(jsonPath("$.data.auctionId").value(auctionId));
    }

    @Test
    @DisplayName("성공: 이미 관심 등록된 경매에 중복 등록 시 bookmarked=false를 반환한다")
    @WithMockMemberPrincipal(publicId = "test-public-id")
    void addBookmark_already_exists() throws Exception {
        // given
        Long auctionId = 1L;
        WishlistAddResponseDto responseDto = WishlistAddResponseDto.of(false, auctionId);

        given(auctionFacade.addBookmark(any(String.class), eq(auctionId)))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/api/v1/auctions/{auctionId}/bookmarks", auctionId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.bookmarked").value(false))
                .andExpect(jsonPath("$.data.auctionId").value(auctionId));
    }

    @Test
    @DisplayName("실패: 존재하지 않는 경매에 관심 등록 시 404를 반환한다")
    @WithMockMemberPrincipal(publicId = "test-public-id")
    void addBookmark_fail_auction_not_found() throws Exception {
        // given
        Long auctionId = 999L;

        given(auctionFacade.addBookmark(any(String.class), eq(auctionId)))
                .willThrow(new CustomException(ErrorType.AUCTION_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/auctions/{auctionId}/bookmarks", auctionId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("성공: 관심 경매 해제 시 HTTP 200과 해제 정보를 반환한다")
    void removeBookmark_success() throws Exception {
        // given
        Long bookmarkId = 1L; // 리팩토링: auctionId -> bookmarkId
        // 서비스 응답 DTO가 bookmarkId를 담고 있다면 그에 맞게 수정
        WishlistRemoveResponseDto responseDto = WishlistRemoveResponseDto.of(true, bookmarkId);

        // Facade의 메서드 인자가 변경되었으므로 eq(bookmarkId)로 수정
        given(auctionFacade.removeBookmark(any(String.class), eq(bookmarkId)))
                .willReturn(responseDto);

        // when & then
        // URL 경로 변수도 명확하게 bookmarkId로 인지되도록 변경
        mockMvc.perform(delete("/api/v1/auctions/{bookmarkId}/bookmarks", bookmarkId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.removed").value(true))
                // DTO의 필드명이 bookmarkId로 바뀌었다면 아래 코드도 수정 필요
                .andExpect(jsonPath("$.data.bookmarkId").value(bookmarkId));
    }

    @Test
    @DisplayName("실패: 관심 등록되지 않은 경매 해제 시 404를 반환한다")
    void removeBookmark_fail_bookmark_not_found() throws Exception {
        // given
        Long bookmarkId = 1L;

        given(auctionFacade.removeBookmark(any(String.class), eq(bookmarkId)))
                .willThrow(new CustomException(ErrorType.BOOKMARK_NOT_FOUND));

        // when & then
        mockMvc.perform(delete("/api/v1/auctions/{bookmarkId}/bookmarks", bookmarkId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("실패: 타인의 북마크를 해제하려 할 때 403을 반환한다")
        // 새로 추가된 보안 로직 테스트
    void removeBookmark_fail_unauthorized() throws Exception {
        // given
        Long bookmarkId = 1L;

        given(auctionFacade.removeBookmark(any(String.class), eq(bookmarkId)))
                .willThrow(new CustomException(ErrorType.BOOKMARK_UNAUTHORIZED_ACCESS));

        // when & then
        mockMvc.perform(delete("/api/v1/auctions/{bookmarkId}/bookmarks", bookmarkId))
                .andDo(print())
                .andExpect(status().isForbidden()) // 403 Forbidden
                .andExpect(jsonPath("$.status").value(403));
    }
}

