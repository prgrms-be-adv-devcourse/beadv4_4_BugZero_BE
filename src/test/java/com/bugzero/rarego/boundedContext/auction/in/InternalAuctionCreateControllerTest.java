package com.bugzero.rarego.boundedContext.auction.in;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bugzero.rarego.boundedContext.auction.app.AuctionSettleAuctionFacade;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionAutoResponseDto;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.response.SuccessType;

@WebMvcTest(controllers = InternalAuctionController.class)
@Import(ResponseAspect.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalAuctionCreateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuctionSettleAuctionFacade facade;

    @Test
    @DisplayName("경매 자동 낙찰 처리 API 호출 성공")
    void settle_Success() throws Exception {
        // given - DTO 빌더 활용
        AuctionAutoResponseDto mockResponse = AuctionAutoResponseDto.builder()
                .requestTime(LocalDateTime.now())
                .processedCount(10)
                .successCount(8)
                .failCount(2)
                .details(List.of())
                .build();

        given(facade.settle()).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/v1/internal/auctions/settle"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(SuccessType.OK.getHttpStatus()))
                .andExpect(jsonPath("$.message").value(SuccessType.OK.getMessage()))
                .andExpect(jsonPath("$.data.processedCount").value(10))
                .andExpect(jsonPath("$.data.successCount").value(8))
                .andExpect(jsonPath("$.data.failCount").value(2));

        verify(facade, times(1)).settle();
    }

    @Test
    @DisplayName("처리할 경매가 없는 경우")
    void settle_NoAuctions() throws Exception {
        // given
        AuctionAutoResponseDto mockResponse = AuctionAutoResponseDto.builder()
                .requestTime(LocalDateTime.now())
                .processedCount(0)
                .successCount(0)
                .failCount(0)
                .details(List.of())
                .build();

        given(facade.settle()).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/v1/internal/auctions/settle"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processedCount").value(0));

        verify(facade, times(1)).settle();
    }
}