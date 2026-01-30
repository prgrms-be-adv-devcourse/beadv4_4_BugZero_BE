package com.bugzero.rarego.bounded_context.auction.in;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.bugzero.rarego.bounded_context.auction.app.AuctionBidStreamSupport;
import com.bugzero.rarego.bounded_context.auction.domain.Auction;
import com.bugzero.rarego.bounded_context.auction.domain.AuctionStatus;
import com.bugzero.rarego.bounded_context.auction.out.AuctionRepository;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.config.JacksonConfig;
import com.bugzero.rarego.global.exception.GlobalExceptionHandler;

/**
 * GlobalExceptionHandler가 예외를 JSON으로 변환하므로
 * Body의 status 필드와 실제 HTTP Status를 함께 검증합니다.
 */
@WebMvcTest(AuctionBidStreamController.class)
@Import({JacksonConfig.class, ResponseAspect.class, GlobalExceptionHandler.class})
@EnableAspectJAutoProxy
@WithMockUser
class AuctionBidStreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuctionBidStreamSupport streamSupport;

    @MockitoBean
    private AuctionRepository auctionRepository;

    @Test
    @DisplayName("SSE 구독 성공")
    void subscribe_Success() throws Exception {
        // given
        Long auctionId = 1L;
        Auction auction = createAuction(auctionId, 100_000, 50_000);

        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));
        given(streamSupport.getAuctionSubscribers(auctionId)).willReturn(10);
        given(streamSupport.subscribe(eq(auctionId), any())).willReturn(new SseEmitter());

        // when & then
        mockMvc.perform(get("/api/v1/auctions/{auctionId}/subscribe", auctionId))
                .andDo(print())
                .andExpect(status().isOk());

        verify(streamSupport).subscribe(eq(auctionId), eq(100_000));
    }

    @Test
    @DisplayName("존재하지 않는 경매 - 404")
    void subscribe_AuctionNotFound() throws Exception {
        // given
        Long auctionId = 999L;
        given(auctionRepository.findById(auctionId)).willReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api/v1/auctions/{auctionId}/subscribe", auctionId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(2001));

        verify(streamSupport, never()).subscribe(any(), any());
    }

    @Test
    @DisplayName("구독자 수 한도 초과 - 503")
    void subscribe_ServiceUnavailable() throws Exception {
        // given
        Long auctionId = 1L;
        Auction auction = createAuction(auctionId, 100_000, 50_000);

        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));
        given(streamSupport.getAuctionSubscribers(auctionId)).willReturn(1000);

        // when & then
        mockMvc.perform(get("/api/v1/auctions/{auctionId}/subscribe", auctionId))
                .andDo(print())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value(2506));

        verify(streamSupport, never()).subscribe(any(), any());
    }

    @Test
    @DisplayName("현재가가 null이면 시작가 사용")
    void subscribe_UseStartPriceWhenCurrentPriceIsNull() throws Exception {
        // given
        Long auctionId = 1L;
        Auction auction = createAuction(auctionId, null, 50_000);

        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));
        given(streamSupport.getAuctionSubscribers(auctionId)).willReturn(10);
        given(streamSupport.subscribe(eq(auctionId), any())).willReturn(new SseEmitter());

        // when & then
        mockMvc.perform(get("/api/v1/auctions/{auctionId}/subscribe", auctionId))
                .andDo(print())
                .andExpect(status().isOk());

        verify(streamSupport).subscribe(eq(auctionId), eq(50_000));
    }

    @Test
    @DisplayName("전체 구독자 수 조회")
    void getTotalSubscribers() throws Exception {
        // given
        given(streamSupport.getTotalSubscribers()).willReturn(150);

        // when & then
        mockMvc.perform(get("/api/v1/auctions/subscribers/count"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("150"));
    }

    @Test
    @DisplayName("특정 경매 구독자 수 조회")
    void getAuctionSubscribers() throws Exception {
        // given
        Long auctionId = 1L;
        given(streamSupport.getAuctionSubscribers(auctionId)).willReturn(25);

        // when & then
        mockMvc.perform(get("/api/v1/auctions/{auctionId}/subscribers/count", auctionId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("25"));
    }

    private Auction createAuction(Long id, Integer currentPrice, Integer startPrice) {
        Auction auction = Auction.builder()
                .productId(100L)
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .durationDays(3)
                .startPrice(startPrice)
                .build();

        ReflectionTestUtils.setField(auction, "id", id);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(auction, "currentPrice", currentPrice);

        return auction;
    }
}