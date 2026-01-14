package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

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

	// [핵심] 실제 DB 대신 Mock 객체가 주입됩니다.
	@MockitoBean private MemberRepository memberRepository;

	private Auction auction;

	// 테스트용 가짜 ID 상수
	private final Long SELLER_ID = 100L;
	private final Long BIDDER_ID = 200L;

	@BeforeEach
	void setUp() {
		bidRepository.deleteAll();
		auctionRepository.deleteAll();
		productRepository.deleteAll();
		// memberRepository는 Mock이므로 deleteAll() 필요 없음

		// 1. MemberRepository Mock 동작 정의
		// 아무 ID나 조회하면 그 ID를 가진 가짜 Member 객체를 반환하도록 설정
		given(memberRepository.findById(anyLong())).willAnswer(invocation -> {
			Long id = invocation.getArgument(0);
			return Optional.of(createMockMember(id)); // 아래 헬퍼 메서드 참조
		});

		// 2. 상품 생성 (sellerId만 맞춰주면 됨)
		Product product = Product.builder()
			.sellerId(SELLER_ID) // 가짜 판매자 ID
			.name("Test Product")
			.description("Desc")
			.build();
		productRepository.save(product);

		// 3. 경매 생성
		auction = Auction.builder()
			.productId(product.getId())
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusHours(1))
			.startPrice(10000)
			.tickSize(1000)
			.build();
		auction.startAuction();
		auctionRepository.save(auction);
	}

	/**
	 * [Helper] Member 엔티티를 Mocking하여 생성
	 * 복잡한 필드(email, provider 등) 채울 필요 없이 ID와 PublicId만 설정
	 */
	private Member createMockMember(Long id) {
		Member mockMember = mock(Member.class);
		given(mockMember.getId()).willReturn(id);
		given(mockMember.getPublicId()).willReturn(UUID.randomUUID().toString());
		return mockMember;
	}

	@Test
	@DisplayName("정상적인 입찰 성공 테스트")
	void createBid_success() {
		// when
		int bidAmount = 10000;
		// 실제 DB에 없어도 Mock Repo가 BIDDER_ID에 해당하는 가짜 객체를 반환해줌
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
		// setUp에서 Product의 sellerId를 SELLER_ID(100L)로 설정했음
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

		// 2. 다른 ID(300L)로 10,500원 입찰 시도
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
			final long tempUserId = 1000L + i; // 1000, 1001, ... 가짜 ID 생성
			executorService.submit(() -> {
				try {
					// Mock Repo가 호출될 때마다 해당 tempUserId를 가진 가짜 Member를 반환하므로 문제 없음
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

		// 1명 성공, 99명 실패 검증
		assertThat(successCount.get()).isEqualTo(1);
		assertThat(failCount.get()).isEqualTo(threadCount - 1);

		Auction findAuction = auctionRepository.findById(auction.getId()).orElseThrow();
		assertThat(findAuction.getCurrentPrice()).isEqualTo(10000);
	}
}