package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.domain.event.AuctionBidCreatedEvent;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;
import com.bugzero.rarego.shared.payment.out.PaymentApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionCreateBidUseCaseTest {

    @InjectMocks
    private AuctionCreateBidUseCase auctionCreateBidUseCase;

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private BidRepository bidRepository;
    @Mock
    private AuctionMemberRepository auctionMemberRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PaymentApiClient paymentApiClient;
    @Mock
    private ApplicationEventPublisher eventPublisher; // [추가] 이벤트 발행자 Mock

    private final Long AUCTION_ID = 1L;
    private final Long PRODUCT_ID = 10L;
    private final Long SELLER_ID = 100L;
    private final Long BIDDER_ID = 200L;

    @Test
    @DisplayName("정상 입찰 성공: 보증금 10% Hold 요청 및 입찰 저장 확인")
    void createBid_Success() {
        // given
        int startPrice = 10000;
        int bidAmount = 20000;

        AuctionMember bidder = AuctionMember.builder().publicId("user_123").build();
        ReflectionTestUtils.setField(bidder, "id", BIDDER_ID);

        Product product = Product.builder().sellerId(SELLER_ID).build();
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

        Auction auction = Auction.builder()
                .productId(PRODUCT_ID)
                .sellerId(SELLER_ID)
                .startPrice(startPrice)
                .tickSize(1000)
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(auction, "id", AUCTION_ID);
        auction.startAuction();

        given(auctionMemberRepository.findById(BIDDER_ID)).willReturn(Optional.of(bidder));
        given(auctionRepository.findByIdWithLock(AUCTION_ID)).willReturn(Optional.of(auction));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
        given(bidRepository.findTopByAuctionIdOrderByBidTimeDesc(any())).willReturn(Optional.empty());

        given(paymentApiClient.holdDeposit(anyInt(), anyLong(), anyLong()))
                .willReturn(new DepositHoldResponseDto(1L, AUCTION_ID, 1000, "HOLD", LocalDateTime.now()));

        // when
        SuccessResponseDto<BidResponseDto> response = auctionCreateBidUseCase.createBid(AUCTION_ID, BIDDER_ID, bidAmount);

        // then
        verify(paymentApiClient).holdDeposit(eq(1000), eq(BIDDER_ID), eq(AUCTION_ID));
        verify(bidRepository).save(any(Bid.class));
        verify(eventPublisher).publishEvent(any(AuctionBidCreatedEvent.class)); // [추가] 이벤트 발행 검증
        assertThat(auction.getCurrentPrice()).isEqualTo(bidAmount);
    }

    @Test
    @DisplayName("입찰 실패: 판매자가 본인 경매에 입찰 시도")
    void createBid_fail_seller_bid() {
        // given
        AuctionMember seller = AuctionMember.builder().build();
        ReflectionTestUtils.setField(seller, "id", SELLER_ID);

        Product product = Product.builder().sellerId(SELLER_ID).build();
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

        Auction auction = Auction.builder()
                .productId(PRODUCT_ID)
                .sellerId(SELLER_ID)
                .startPrice(10000)
                .tickSize(1000)
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(auction, "id", AUCTION_ID);
        auction.startAuction();

        given(auctionMemberRepository.findById(SELLER_ID)).willReturn(Optional.of(seller));
        given(auctionRepository.findByIdWithLock(AUCTION_ID)).willReturn(Optional.of(auction));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() ->
                auctionCreateBidUseCase.createBid(AUCTION_ID, SELLER_ID, 20000)
        )
                .isInstanceOf(CustomException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.AUCTION_SELLER_CANNOT_BID);
    }

    @Test
    @DisplayName("입찰 실패: 최소 입찰가보다 낮은 금액")
    void createBid_fail_low_price() {
        // given
        AuctionMember bidder = AuctionMember.builder().build();
        ReflectionTestUtils.setField(bidder, "id", BIDDER_ID);

        Product product = Product.builder().sellerId(SELLER_ID).build();
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

        Auction auction = Auction.builder()
                .productId(PRODUCT_ID)
                .sellerId(SELLER_ID)
                .startPrice(5000)
                .tickSize(1000)
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(auction, "id", AUCTION_ID);
        auction.startAuction();
        auction.updateCurrentPrice(10000);

        given(auctionMemberRepository.findById(BIDDER_ID)).willReturn(Optional.of(bidder));
        given(auctionRepository.findByIdWithLock(AUCTION_ID)).willReturn(Optional.of(auction));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() ->
                auctionCreateBidUseCase.createBid(AUCTION_ID, BIDDER_ID, 10500)
        )
                .isInstanceOf(CustomException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.AUCTION_BID_AMOUNT_TOO_LOW);
    }
}