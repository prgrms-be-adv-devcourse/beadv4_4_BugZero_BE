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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.domain.event.AuctionBidCreatedEvent;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import com.bugzero.rarego.shared.payment.out.PaymentApiClient;

@ExtendWith(MockitoExtension.class)
class AuctionCreateBidUseCaseTest {

	@InjectMocks
	private AuctionCreateBidUseCase auctionCreateBidUseCase;

	// [핵심] Repository 대신 Support를 Mocking
	@Mock
	private AuctionSupport support;

	// 입찰 저장 및 검증용 Repository는 유지
	@Mock
	private BidRepository bidRepository;

	@Mock
	private PaymentApiClient paymentApiClient;
	@Mock
	private ApplicationEventPublisher eventPublisher;

	// UseCase가 직접 쓰지 않는 Repository Mock들은 제거함 (AuctionRepository, AuctionMemberRepository)

	private final Long AUCTION_ID = 1L;
	private final Long BIDDER_ID = 100L;
	private final String BIDDER_PUBLICID = "bidder-uuid";
	private final Long SELLER_ID = 200L;
	private final String SELLER_PUBLICID = "seller-uuid";
	private final Long PRODUCT_ID = 50L;

	@Test
	@DisplayName("정상 입찰 성공: 보증금 10% Hold 요청 및 입찰 저장 확인")
	void createBid_Success() {
		// given
		// 1. 입찰자(Member) 설정
		AuctionMember bidder = AuctionMember.builder()
			.publicId(BIDDER_PUBLICID)
			.build();
		ReflectionTestUtils.setField(bidder, "id", BIDDER_ID);

		// 2. 경매(Auction) 설정
		Auction auction = Auction.builder()
			.productId(PRODUCT_ID)
			.sellerId(SELLER_ID)
			.startPrice(1000)
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusHours(1))
			.durationDays(1)
			.build();
		ReflectionTestUtils.setField(auction, "id", AUCTION_ID);
		ReflectionTestUtils.setField(auction, "status", AuctionStatus.IN_PROGRESS);
		ReflectionTestUtils.setField(auction, "currentPrice", 5000);

		// 3. Mocking (Support 사용)
		// Repository와 달리 Support는 Entity를 직접 반환하므로 Optional.of() 제거
		given(support.getPublicMember(BIDDER_PUBLICID)).willReturn(bidder);

		// [중요] UseCase에서 getAuctionWithLock을 호출하므로 맞춰줌
		given(support.getAuctionWithLock(AUCTION_ID)).willReturn(auction);

		// 연속 입찰 검증 (Repository 직접 호출 유지)
		given(bidRepository.findTopByAuctionIdOrderByBidTimeDesc(AUCTION_ID)).willReturn(Optional.empty());

		// when
		BidResponseDto result = auctionCreateBidUseCase.createBid(AUCTION_ID, BIDDER_PUBLICID, 6000);

		// then
		assertThat(result.bidAmount()).isEqualTo(6000);
		assertThat(auction.getCurrentPrice()).isEqualTo(6000); // Dirty Checking 시뮬레이션

		// Verify
		verify(bidRepository).save(any(Bid.class));
		verify(eventPublisher).publishEvent(any(AuctionBidCreatedEvent.class));
	}

	@Test
	@DisplayName("입찰 실패: 판매자가 본인 경매에 입찰 시도")
	void createBid_fail_seller_bid() {
		// given
		AuctionMember seller = AuctionMember.builder()
			.publicId(SELLER_PUBLICID)
			.build();
		ReflectionTestUtils.setField(seller, "id", SELLER_ID);

		Auction auction = Auction.builder()
			.productId(PRODUCT_ID)
			.sellerId(SELLER_ID)
			.startPrice(10000)
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusHours(1))
			.durationDays(1)
			.build();
		ReflectionTestUtils.setField(auction, "id", AUCTION_ID);
		ReflectionTestUtils.setField(auction, "status", AuctionStatus.IN_PROGRESS);

		// [수정] Support Mocking
		given(support.getPublicMember(SELLER_PUBLICID)).willReturn(seller);
		given(support.getAuctionWithLock(AUCTION_ID)).willReturn(auction);

		// when & then
		assertThatThrownBy(() ->
			auctionCreateBidUseCase.createBid(AUCTION_ID, SELLER_PUBLICID, 20000)
		)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUCTION_SELLER_CANNOT_BID);
	}

	@Test
	@DisplayName("입찰 실패: 최소 입찰가보다 낮은 금액")
	void createBid_fail_low_price() {
		// given
		AuctionMember bidder = AuctionMember.builder()
			.publicId(BIDDER_PUBLICID)
			.build();
		ReflectionTestUtils.setField(bidder, "id", BIDDER_ID);

		Auction auction = Auction.builder()
			.productId(PRODUCT_ID)
			.sellerId(SELLER_ID)
			.startPrice(5000)
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusHours(1))
			.durationDays(1)
			.build();
		ReflectionTestUtils.setField(auction, "id", AUCTION_ID);
		ReflectionTestUtils.setField(auction, "status", AuctionStatus.IN_PROGRESS);
		ReflectionTestUtils.setField(auction, "currentPrice", 10000);
		ReflectionTestUtils.setField(auction, "tickSize", 1000);
		// 현재가 10,000 -> 호가단위 1,000 -> 최소입찰가 11,000

		// [수정] Support Mocking
		given(support.getPublicMember(BIDDER_PUBLICID)).willReturn(bidder);
		given(support.getAuctionWithLock(AUCTION_ID)).willReturn(auction);

		// 연속 입찰 아님
		given(bidRepository.findTopByAuctionIdOrderByBidTimeDesc(AUCTION_ID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() ->
			auctionCreateBidUseCase.createBid(AUCTION_ID, BIDDER_PUBLICID, 10500)
		)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUCTION_BID_AMOUNT_TOO_LOW);
	}
}
