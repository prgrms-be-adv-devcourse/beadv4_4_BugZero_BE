package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.app.AuctionSettleAuctionFacade;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionAutoSettleResponseDto;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.response.SuccessType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalAuctionController.class)
@Import(ResponseAspect.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalAuctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuctionSettleAuctionFacade facade;

    @MockitoBean
    private AuctionFacade auctionFacade;

    @Nested
    @DisplayName("경매 정산 API")
    class SettleTests {

        @Test
        @DisplayName("경매 자동 낙찰 처리 API 호출 성공")
        void settle_Success() throws Exception {
            // given
            AuctionAutoSettleResponseDto mockResponse = AuctionAutoSettleResponseDto.builder()
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
            AuctionAutoSettleResponseDto mockResponse = AuctionAutoSettleResponseDto.builder()
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
}
