package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember; // [변경] Member -> AuctionMember
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository; // [변경] MemberRepository -> AuctionMemberRepository
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBootTest
@ActiveProfiles("test")
class AuctionCreateBidUseCaseTest {

	@Autowired private AuctionCreateBidUseCase auctionCreateBidUseCase;
	@Autowired private AuctionRepository auctionRepository;
	@Autowired private BidRepository bidRepository;
	@Autowired private ProductRepository productRepository;

	// [변경] MemberRepository -> AuctionMemberRepository 로 변경
	@MockitoBean private AuctionMemberRepository auctionMemberRepository;

	private Auction auction;

	private final Long SELLER_ID = 100L;
	private final Long BIDDER_ID = 200L;

	@BeforeEach
	void setUp() {
		bidRepository.deleteAll();
		auctionRepository.deleteAll();
		productRepository.deleteAll();

		// [변경] AuctionMemberRepository Mock 동작 정의
		given(auctionMemberRepository.findById(anyLong())).willAnswer(invocation -> {
			Long id = invocation.getArgument(0);
			return Optional.of(createMockMember(id));
		});

		// 상품 생성
		Product product = Product.builder()
			.sellerId(SELLER_ID)
			.name("Test Product")
			.description("Desc")
			.build();
		productRepository.save(product);

		// 경매 생성
		auction = Auction.builder()
			.productId(product.getId())
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusHours(1))
			.startPrice(10000)
			.tickSize(1000)
			.build();

		// [추가] Auction 상태를 IN_PROGRESS로 변경 (이전 오류 수정 사항 반영)
		auction.startAuction();
		auctionRepository.save(auction);
	}

	// [변경] 반환 타입 Member -> AuctionMember
	private AuctionMember createMockMember(Long id) {
		// Mock 객체 생성 시 AuctionMember 클래스 사용
		AuctionMember mockMember = mock(AuctionMember.class);
		given(mockMember.getId()).willReturn(id);
		given(mockMember.getPublicId()).willReturn(UUID.randomUUID().toString());
		return mockMember;
	}

	@Test
	@DisplayName("정상적인 입찰 성공 테스트")
	void createBid_success() {
		// when
		int bidAmount = 10000;
		auctionCreateBidUseCase.createBid(auction.getId(), BIDDER_ID, bidAmount);

		// then
		Auction findAuction = auctionRepository.findById(auction.getId()).orElseThrow();
		Bid findBid = bidRepository.findAll().get(0);

		assertThat(findAuction.getCurrentPrice()).isEqualTo(bidAmount);
		assertThat(findBid.getBidAmount()).isEqualTo(bidAmount);
		assertThat(findBid.getBidderId()).isEqualTo(BIDDER_ID);
	}

	@Test
	@DisplayName("입찰 실패: 판매자가 본인 경매에 입찰 시도")
	void createBid_fail_seller_bid() {
		assertThatThrownBy(() ->
			auctionCreateBidUseCase.createBid(auction.getId(), SELLER_ID, 11000)
		)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUCTION_SELLER_CANNOT_BID);
	}

	@Test
	@DisplayName("입찰 실패: 최소 입찰가보다 낮은 금액")
	void createBid_fail_low_price() {
		// 1. 10,000원 입찰
		auctionCreateBidUseCase.createBid(auction.getId(), BIDDER_ID, 10000);

		// 2. 다른 ID(300L)로 10,500원 입찰 시도 (최소 11,000원이어야 함)
		Long otherBidderId = 300L;

		assertThatThrownBy(() ->
			auctionCreateBidUseCase.createBid(auction.getId(), otherBidderId, 10500)
		)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUCTION_BID_AMOUNT_TOO_LOW);
	}

	@Test
	@DisplayName("동시성 테스트: 동시에 100명이 입찰 시도")
	void createBid_concurrency() throws InterruptedException {
		int threadCount = 100;
		ExecutorService executorService = Executors.newFixedThreadPool(32);
		CountDownLatch latch = new CountDownLatch(threadCount);

		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();

		for (int i = 0; i < threadCount; i++) {
			final long tempUserId = 1000L + i;
			executorService.submit(() -> {
				try {
					auctionCreateBidUseCase.createBid(auction.getId(), tempUserId, 10000);
					successCount.incrementAndGet();
				} catch (Exception e) {
					failCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();

		assertThat(successCount.get()).isEqualTo(1);
		assertThat(failCount.get()).isEqualTo(threadCount - 1);

		Auction findAuction = auctionRepository.findById(auction.getId()).orElseThrow();
		assertThat(findAuction.getCurrentPrice()).isEqualTo(10000);
	}
}