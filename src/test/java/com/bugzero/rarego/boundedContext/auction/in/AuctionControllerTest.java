package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.config.JacksonConfig;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.*;
import com.bugzero.rarego.shared.auction.dto.BidLogResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidRequestDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuctionController.class)
@Import({ResponseAspect.class, JacksonConfig.class})
@EnableAspectJAutoProxy
class AuctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuctionFacade auctionFacade;

    @Test
    @DisplayName("성공: 유효한 입찰 요청 시 HTTP 201과 입찰 정보를 반환한다")
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

        given(auctionFacade.createBid(eq(auctionId), eq(memberId), eq(bidAmount.intValue())))
                .willReturn(SuccessResponseDto.from(SuccessType.CREATED, bidResponse));

        // when & then
        mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
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
    @DisplayName("실패: 입찰 금액이 음수인 경우 400 에러를 반환한다")
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
    @DisplayName("실패: 현재 최고 입찰자가 연속 입찰 시 409를 반환한다")
    @WithMockUser(username = "2")
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
                        .content(objectMapper.writeValueAsString(requestDto))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("실패: 경매를 찾을 수 없는 경우 404를 반환한다")
    @WithMockUser(username = "2")
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
                        .content(objectMapper.writeValueAsString(requestDto))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}