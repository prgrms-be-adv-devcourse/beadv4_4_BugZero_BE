package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.*;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionPaymentTimeoutResponse;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.auction.dto.BidLogResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import com.bugzero.rarego.shared.auction.dto.MyBidResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionFacadeTest {

    @InjectMocks
    private AuctionFacade auctionFacade;

    // UseCases
    @Mock
    private AuctionCreateBidUseCase auctionCreateBidUseCase;
    @Mock
    private AuctionProcessTimeoutUseCase auctionProcessTimeoutUseCase;

    // Repositories
    @Mock
    private BidRepository bidRepository;
    @Mock
    private AuctionMemberRepository auctionMemberRepository;
    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private AuctionOrderRepository auctionOrderRepository;

    @Test
    @DisplayName("입찰 생성 요청 시 UseCase를 호출하고 결과를 반환한다")
    void createBid_Success() {
        // given
        Long auctionId = 1L;
        Long memberId = 100L;
        int bidAmount = 50000;

        BidResponseDto bidResponse = new BidResponseDto(
                1L, auctionId, "public-id", LocalDateTime.now(), (long) bidAmount, (long) bidAmount
        );

        SuccessResponseDto<BidResponseDto> expectedResponse = SuccessResponseDto.from(
                SuccessType.CREATED,
                bidResponse
        );

        given(auctionCreateBidUseCase.createBid(auctionId, memberId, bidAmount))
                .willReturn(expectedResponse);

        // when
        SuccessResponseDto<BidResponseDto> result = auctionFacade.createBid(auctionId, memberId, bidAmount);

        // then
        assertThat(result).isEqualTo(expectedResponse);
        verify(auctionCreateBidUseCase).createBid(auctionId, memberId, bidAmount);
    }

    @Test
    @DisplayName("경매 입찰 기록 조회: 입찰자와 매핑하여 반환한다")
    void getBidLogs_Success() {
        // given
        Long auctionId = 1L;
        Long bidderId = 100L;
        Pageable pageable = PageRequest.of(0, 10);

        Bid bid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .bidAmount(50000)
                .build();
        ReflectionTestUtils.setField(bid, "id", 1L);

        AuctionMember bidder = AuctionMember.builder().publicId("user_masked").build();
        ReflectionTestUtils.setField(bidder, "id", bidderId);

        given(bidRepository.findAllByAuctionIdOrderByBidTimeDesc(eq(auctionId), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(bid)));

        given(auctionMemberRepository.findAllById(anySet())).willReturn(List.of(bidder));

        // when
        PagedResponseDto<BidLogResponseDto> result = auctionFacade.getBidLogs(auctionId, pageable);

        // then
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).publicId()).isEqualTo("user_masked");
        assertThat(result.data().get(0).bidAmount()).isEqualTo(50000);
    }

    @Test
    @DisplayName("나의 입찰 내역 조회: 경매 정보와 매핑하여 반환한다")
    void getMyBids_Success() {
        // given
        Long memberId = 100L;
        Long auctionId = 10L;
        Pageable pageable = PageRequest.of(0, 10);

        Auction auction = Auction.builder()
                .productId(50L)
                .startPrice(10000)
                .tickSize(1000)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(1))
                .durationDays(1)
                .build();
        ReflectionTestUtils.setField(auction, "id", auctionId);
        auction.startAuction();
        auction.updateCurrentPrice(15000);

        Bid bid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(memberId)
                .bidAmount(15000)
                .build();
        ReflectionTestUtils.setField(bid, "id", 1L);

        given(bidRepository.findMyBids(eq(memberId), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(bid)));

        given(auctionRepository.findAllById(anySet())).willReturn(List.of(auction));

        // when
        PagedResponseDto<MyBidResponseDto> result = auctionFacade.getMyBids(memberId, null, pageable);

        // then
        assertThat(result.data()).hasSize(1);
        MyBidResponseDto dto = result.data().get(0);
        assertThat(dto.auctionStatus()).isEqualTo(AuctionStatus.IN_PROGRESS);
        assertThat(dto.bidAmount()).isEqualTo(15000);
        assertThat(dto.currentPrice()).isEqualTo(15000);
    }

    @Nested
    @DisplayName("결제 타임아웃 처리")
    class ProcessPaymentTimeoutTests {

        @Test
        @DisplayName("UseCase 호출 후 결과 반환")
        void processPaymentTimeout_callsUseCaseAndReturnsResult() {
            // given
            List<AuctionPaymentTimeoutResponse.TimeoutDetail> details = List.of(
                    new AuctionPaymentTimeoutResponse.TimeoutDetail(1L, 100L, 200L, 3000, AuctionOrderStatus.FAILED),
                    new AuctionPaymentTimeoutResponse.TimeoutDetail(2L, 101L, 201L, 5000, AuctionOrderStatus.FAILED)
            );

            AuctionPaymentTimeoutResponse expectedResponse = new AuctionPaymentTimeoutResponse(
                    LocalDateTime.now(),
                    2,
                    details
            );

            given(auctionProcessTimeoutUseCase.execute()).willReturn(expectedResponse);

            // when
            AuctionPaymentTimeoutResponse result = auctionFacade.processPaymentTimeout();

            // then
            assertThat(result).isEqualTo(expectedResponse);
            assertThat(result.processedCount()).isEqualTo(2);
            verify(auctionProcessTimeoutUseCase, times(1)).execute();
        }

        @Test
        @DisplayName("타임아웃 대상 없으면 빈 결과 반환")
        void processPaymentTimeout_noTargets_returnsEmptyResult() {
            // given
            AuctionPaymentTimeoutResponse expectedResponse = new AuctionPaymentTimeoutResponse(
                    LocalDateTime.now(),
                    0,
                    List.of()
            );

            given(auctionProcessTimeoutUseCase.execute()).willReturn(expectedResponse);

            // when
            AuctionPaymentTimeoutResponse result = auctionFacade.processPaymentTimeout();

            // then
            assertThat(result.processedCount()).isZero();
            verify(auctionProcessTimeoutUseCase, times(1)).execute();
        }
    }

    /*
     * TODO: GitHub Actions 환경에서 실패하는 문제 해결 필요
     * @Test
    @DisplayName("나의 판매 목록 조회: 상품, 주문, 입찰수 정보를 종합하여 반환한다")
    void getMySales_Success() {
        Long sellerId = 1L;
        AuctionFilterType filter = AuctionFilterType.ALL;
        Pageable pageable = PageRequest.of(0, 10);

        given(productRepository.findAllIdsBySellerId(sellerId)).willReturn(List.of(10L, 20L));

        Auction auction1 = Auction.builder()
            .productId(10L)
            .sellerId(sellerId)
            .startPrice(1000)
            .tickSize(100)
            .startTime(LocalDateTime.now().minusHours(1))
            .endTime(LocalDateTime.now().plusDays(1))
            .build();
        auction1.updateCurrentPrice(0);
        ReflectionTestUtils.setField(auction1, "id", 100L);
        auction1.startAuction();

        Auction auction2 = Auction.builder()
            .productId(20L)
            .sellerId(sellerId)
            .startPrice(2000)
            .tickSize(100)
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(2))
            .build();
        auction2.updateCurrentPrice(0);
        ReflectionTestUtils.setField(auction2, "id", 200L);

        given(auctionRepository.findAllByProductIdIn(anyList(), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(auction1, auction2)));

        Product product1 = Product.builder().name("Product 1").sellerId(sellerId).build();
        ReflectionTestUtils.setField(product1, "id", 10L);
        Product product2 = Product.builder().name("Product 2").sellerId(sellerId).build();
        ReflectionTestUtils.setField(product2, "id", 20L);

        given(productRepository.findAllByIdIn(anySet())).willReturn(List.of(product1, product2));

        AuctionOrder order = AuctionOrder.builder()
            .auctionId(100L)
            .sellerId(sellerId)
            .bidderId(2L)
            .finalPrice(50000)
            .build();
        ReflectionTestUtils.setField(order, "status", AuctionOrderStatus.PROCESSING);

        given(auctionOrderRepository.findAllByAuctionIdIn(anySet())).willReturn(List.of(order));

        given(bidRepository.countByAuctionIdIn(anySet()))
            .willReturn(List.of(new Object[]{100L, 5L}, new Object[]{200L, 0L}));

        PagedResponseDto<MySaleResponseDto> result = auctionFacade.getMySales(sellerId, filter, pageable);

        assertThat(result.data()).hasSize(2);

        MySaleResponseDto dto1 = result.data().stream()
            .filter(d -> d.auctionId().equals(100L)).findFirst().orElseThrow();
        assertThat(dto1.title()).isEqualTo("Product 1");
        assertThat(dto1.bidCount()).isEqualTo(5);
        assertThat(dto1.tradeStatus()).isEqualTo(AuctionOrderStatus.PROCESSING);
    }
    */
}