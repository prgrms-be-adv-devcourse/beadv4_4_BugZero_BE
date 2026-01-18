package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.exception.GlobalExceptionHandler;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.auction.dto.BidRequestDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
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
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                        // 헤더에서 ID를 읽어 유저 정보를 동적으로 생성 (없으면 기본값 "2")
                        String memberId = webRequest.getHeader("Test-Member-Id");
                        return User.withUsername(memberId != null ? memberId : "2")
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
        Long memberId = 2L;
        Long bidAmount = 10000L;

        // DTO에서 testerId 제거됨
        BidRequestDto requestDto = new BidRequestDto(bidAmount);

        BidResponseDto bidResponse = new BidResponseDto(
                100L, auctionId, UUID.randomUUID().toString(),
                LocalDateTime.now(), bidAmount, bidAmount
        );

        given(auctionFacade.createBid(eq(auctionId), eq(memberId), eq(bidAmount.intValue())))
                .willReturn(SuccessResponseDto.from(SuccessType.CREATED, bidResponse));

        // when & then
        mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201));
    }

    @Test
    @DisplayName("성공: 헤더를 통해 99번 유저로 입찰을 처리한다")
    void createBid_success_with_other_user() throws Exception {
        // given
        Long auctionId = 1L;
        Long memberId = 99L;
        Long bidAmount = 15000L;
        BidRequestDto requestDto = new BidRequestDto(bidAmount);

        given(auctionFacade.createBid(eq(auctionId), eq(memberId), eq(bidAmount.intValue())))
                .willReturn(SuccessResponseDto.from(SuccessType.CREATED, null));

        // when & then
        mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .header("Test-Member-Id", "99") // 헤더로 유저 주입
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("실패: 입찰 금액이 음수인 경우 Body에 400을 반환한다")
    void createBid_fail_validation() throws Exception {
        // given
        Long auctionId = 1L;
        BidRequestDto invalidRequest = new BidRequestDto(-500L);

        // when & then
        mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("실패: 현재 최고 입찰자가 연속 입찰 시 Body에 409를 반환한다")
    void createBid_fail_consecutive_bid() throws Exception {
        // given
        Long auctionId = 1L;
        Long memberId = 2L;
        BidRequestDto requestDto = new BidRequestDto(20000L);

        given(auctionFacade.createBid(eq(auctionId), eq(memberId), anyInt()))
                .willThrow(new CustomException(ErrorType.AUCTION_ALREADY_HIGHEST_BIDDER));

        // when & then
        mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(2003));
    }

    @Test
    @DisplayName("실패: 경매를 찾을 수 없는 경우 Body에 404를 반환한다")
    void createBid_fail_business_exception() throws Exception {
        // given
        Long auctionId = 999L;
        Long memberId = 2L;
        BidRequestDto requestDto = new BidRequestDto(10000L);

        given(auctionFacade.createBid(eq(auctionId), eq(memberId), anyInt()))
                .willThrow(new CustomException(ErrorType.AUCTION_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(404));
    }
}