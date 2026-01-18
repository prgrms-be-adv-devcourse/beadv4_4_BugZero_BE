package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.auction.dto.BidRequestDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
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
@AutoConfigureMockMvc(addFilters = false)
class AuctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuctionFacade auctionFacade;

    @Test
    @DisplayName("성공: testerId를 포함한 유효한 입찰 요청 시 201을 반환한다")
    void createBid_success_with_testerId() throws Exception {
        // given
        Long auctionId = 1L;
        Long testerId = 3L; // 테스트용으로 지정한 ID
        Long bidAmount = 10000L;

        // testerId를 포함하도록 Dto 생성
        BidRequestDto requestDto = new BidRequestDto(bidAmount, testerId);

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

        // Facade가 전달받은 testerId를 memberId로 사용하는지 검증 (중요!)
        given(auctionFacade.createBid(eq(auctionId), eq(testerId), eq(bidAmount.intValue())))
                .willReturn(successResponse);

        // when & then
        mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bidAmount").value(bidAmount));
    }

    @Test
    @DisplayName("성공: testerId가 없을 경우 기본값(2L)을 사용한다")
    void createBid_success_default_memberId() throws Exception {
        // given
        Long auctionId = 1L;
        Long defaultMemberId = 2L;
        Long bidAmount = 10000L;

        // testerId가 null인 요청
        BidRequestDto requestDto = new BidRequestDto(bidAmount, null);

        given(auctionFacade.createBid(eq(auctionId), eq(defaultMemberId), anyInt()))
                .willReturn(SuccessResponseDto.from(SuccessType.CREATED, null));

        // when & then
        mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("실패: 동일한 testerId로 연속 입찰 시 409 예외가 발생한다")
    void createBid_fail_consecutive_bid() throws Exception {
        // given
        Long auctionId = 1L;
        Long testerId = 2L;
        BidRequestDto requestDto = new BidRequestDto(15000L, testerId);

        // 정의하신 AUCTION_ALREADY_HIGHEST_BIDDER를 던지도록 설정
        given(auctionFacade.createBid(eq(auctionId), eq(testerId), anyInt()))
                .willThrow(new CustomException(ErrorType.AUCTION_ALREADY_HIGHEST_BIDDER));

        // when & then
        mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.message").value("현재 최고 입찰자이므로 연속 입찰할 수 없습니다."));
    }
}