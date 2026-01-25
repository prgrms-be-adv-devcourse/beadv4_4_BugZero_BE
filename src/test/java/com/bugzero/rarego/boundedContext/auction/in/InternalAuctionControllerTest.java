package com.bugzero.rarego.boundedContext.auction.in;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.app.AuctionSettleAuctionFacade;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionAutoSettleResponseDto;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = InternalAuctionController.class)
@Import(ResponseAspect.class)
@EnableAspectJAutoProxy
@AutoConfigureMockMvc(addFilters = false)
class InternalAuctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuctionSettleAuctionFacade facade;

    @MockitoBean
    private AuctionFacade auctionFacade;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Nested
    @DisplayName("경매정보 API")
    class InfoTests {
        @Test
        @DisplayName("정상적인 경매 생성 요청 시 201 OK와 생성된 ID를 반환한다")
        void createAuction_Success() throws Exception {
            // given
            Long productId = 1L;
            String publicId = "1L";
            ProductAuctionRequestDto requestDto = ProductAuctionRequestDto.builder()
                .startPrice(10000)
                .durationDays(7)
                .build();

            given(auctionFacade.createAuction(eq(productId), eq(publicId), any(ProductAuctionRequestDto.class)))
                .willReturn(10L);

            // when & then
            mockMvc.perform(post("/api/v1/internal/auctions/{productId}/{publicId}", productId, publicId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").value(10));
        }

        @Test
        @DisplayName("입찰 시작가가 100원 미만이면 400 Bad Request를 반환한다")
        void createAuction_Fail_MinPrice() throws Exception {
            // given
            Long productId = 1L;
            String sellerUUID = "1L";

            ProductAuctionRequestDto invalidDto = ProductAuctionRequestDto.builder()
                .startPrice(50) // @Min(100) 위반
                .durationDays(7)
                .build();

            // when & then
            mockMvc.perform(post("/api/v1/internal/auctions/{productId}/{publicId}", productId, sellerUUID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("경매기간 설정을 범위 내 하지 않으면 400 Bad Request를 반환한다")
        void createAuction_Fail_MinDuration() throws Exception {
            // given
            Long productId = 1L;
            String sellerUUID = "1L";

            ProductAuctionRequestDto invalidDto = ProductAuctionRequestDto.builder()
                .startPrice(10000) // @Min(100) 위반
                .durationDays(100)
                .build();

            // when & then
            mockMvc.perform(post("/api/v1/internal/auctions/{productId}/{publicId}", productId, sellerUUID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("정상적인 경매 수정 요청 시 200 OK와 생성된 ID를 반환한다")
        void updateAuction_Success() throws Exception {
            // given
            String publicId = "1L";
            ProductAuctionUpdateDto updateDto = ProductAuctionUpdateDto.builder()
                .auctionId(1L)
                .startPrice(10000)
                .durationDays(7)
                .build();

            given(auctionFacade.updateAuction(eq(publicId), any(ProductAuctionUpdateDto.class)))
                .willReturn(10L);

            // when & then
            mockMvc.perform(patch("/api/v1/internal/auctions/{publicId}", publicId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(10));
        }

        @Test
        @DisplayName("성공 - 경매 삭제 요청 시 200 OK를 반환한다")
        void deleteAuction_Success() throws Exception {
            // given
            String publicId = "seller-uuid";
            Long productId = 100L;

            // void 메서드이므로 doNothing 설정
            doNothing().when(auctionFacade).deleteAuction(eq(publicId), eq(productId));

            // when & then
            mockMvc.perform(delete("/api/v1/internal/auctions/{productId}/{publicId}", productId, publicId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andDo(print());

            // UseCase 호출 여부 최종 확인
            verify(auctionFacade).deleteAuction(eq(publicId), eq(productId));
        }
    }
}
