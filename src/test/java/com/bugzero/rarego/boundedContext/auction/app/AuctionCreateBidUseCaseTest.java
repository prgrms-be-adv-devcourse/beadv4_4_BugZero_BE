package com.bugzero.rarego.boundedContext.auction.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
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

		// 입찰자 설정
		AuctionMember bidder = AuctionMember.builder().publicId("user_123").build();
		ReflectionTestUtils.setField(bidder, "id", BIDDER_ID);

		// 상품 설정 (판매자 ID 포함)
		Product product = Product.builder().sellerId(SELLER_ID).build();
		ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

		// 경매 설정
		// [충돌 해결] 테스트 검증값(보증금 1000원)에 맞추기 위해 startPrice는 10000원이어야 함.
		// sellerId는 엔티티 정합성을 위해 포함하는 것이 좋습니다.
		Auction auction = Auction.builder()
			.productId(PRODUCT_ID)
			.sellerId(SELLER_ID)
			.startPrice(startPrice) // 10,000원
			.tickSize(1000)
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusHours(1))
			.durationDays(1)
			.build();
		ReflectionTestUtils.setField(auction, "id", AUCTION_ID);
		auction.startAuction(); // 상태: IN_PROGRESS

		// Mocking
		given(auctionMemberRepository.findById(BIDDER_ID)).willReturn(Optional.of(bidder));
		given(auctionRepository.findByIdWithLock(AUCTION_ID)).willReturn(Optional.of(auction));

		// ProductRepository Mocking
		given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

		given(bidRepository.findTopByAuctionIdOrderByBidTimeDesc(any())).willReturn(Optional.empty());

		given(paymentApiClient.holdDeposit(anyInt(), anyLong(), anyLong()))
			.willReturn(new DepositHoldResponseDto(1L, AUCTION_ID, 1000, "HOLD", LocalDateTime.now()));

		// when
		SuccessResponseDto<BidResponseDto> response = auctionCreateBidUseCase.createBid(AUCTION_ID, BIDDER_ID, bidAmount);

		// then
		verify(paymentApiClient).holdDeposit(eq(1000), eq(BIDDER_ID), eq(AUCTION_ID));
		verify(bidRepository).save(any(Bid.class));
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

		// [충돌 해결] startPrice 등 테스트 목적에 맞게 병합
		Auction auction = Auction.builder()
			.productId(PRODUCT_ID)
			.sellerId(SELLER_ID)
			.startPrice(10000)
			.tickSize(1000)
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusHours(1))
			.durationDays(1)
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
			.durationDays(1)
			.build();
		ReflectionTestUtils.setField(auction, "id", AUCTION_ID);
		auction.startAuction();
		auction.updateCurrentPrice(10000); // 현재가 10,000 -> 최소입찰가 11,000

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