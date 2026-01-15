package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class) // 스프링 컨텍스트 없이 Mockito만 사용
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

	private final Long AUCTION_ID = 1L;
	private final Long PRODUCT_ID = 10L;
	private final Long SELLER_ID = 100L;
	private final Long BIDDER_ID = 200L;

	@Test
	@DisplayName("정상적인 입찰 성공 테스트")
	void createBid_success() {
		// given
		int bidAmount = 10000;

		// 1. 입찰자(AuctionMember) Mock
		AuctionMember bidder = AuctionMember.builder().build();
		ReflectionTestUtils.setField(bidder, "id", BIDDER_ID); // ID 주입
		ReflectionTestUtils.setField(bidder, "publicId", "test-public-id");
		given(auctionMemberRepository.findById(BIDDER_ID)).willReturn(Optional.of(bidder));

		// 2. 경매(Auction) Mock - 정상 진행 중 상태
		Auction auction = Auction.builder()
			.productId(PRODUCT_ID)
			.startPrice(5000)
			.tickSize(1000)
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusHours(1))
			.build();
		ReflectionTestUtils.setField(auction, "id", AUCTION_ID);
		auction.startAuction(); // 상태를 IN_PROGRESS로 변경

		// 주의: useCase에서 findByIdWithLock을 호출하므로 이를 Mocking 해야 함
		given(auctionRepository.findByIdWithLock(AUCTION_ID)).willReturn(Optional.of(auction));

		// 3. 상품(Product) Mock
		Product product = Product.builder()
			.sellerId(SELLER_ID) // 판매자는 다른 사람
			.build();
		given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

		// 4. 이전 입찰 내역 Mock (없음)
		given(bidRepository.findTopByAuctionIdOrderByBidTimeDesc(AUCTION_ID)).willReturn(Optional.empty());

		// when
		auctionCreateBidUseCase.createBid(AUCTION_ID, BIDDER_ID, bidAmount);

		// then
		// 1. 경매 현재가가 업데이트 되었는지 확인
		assertThat(auction.getCurrentPrice()).isEqualTo(bidAmount);

		// 2. 입찰 정보가 저장되었는지 확인 (save 호출 여부 검증)
		verify(bidRepository).save(any(Bid.class));
	}

	@Test
	@DisplayName("입찰 실패: 판매자가 본인 경매에 입찰 시도")
	void createBid_fail_seller_bid() {
		// given
		AuctionMember seller = AuctionMember.builder().build();
		ReflectionTestUtils.setField(seller, "id", SELLER_ID); // 입찰자가 곧 판매자
		given(auctionMemberRepository.findById(SELLER_ID)).willReturn(Optional.of(seller));

		Auction auction = Auction.builder()
			.productId(PRODUCT_ID)
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusHours(1))
			.build();

		// [수정] Auction ID 설정 추가
		ReflectionTestUtils.setField(auction, "id", AUCTION_ID);
		auction.startAuction();

		given(auctionRepository.findByIdWithLock(AUCTION_ID)).willReturn(Optional.of(auction));

		Product product = Product.builder()
			.sellerId(SELLER_ID) // 판매자 ID 설정
			.build();
		given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

		// 이전 입찰 내역 Mock
		given(bidRepository.findTopByAuctionIdOrderByBidTimeDesc(AUCTION_ID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() ->
			auctionCreateBidUseCase.createBid(AUCTION_ID, SELLER_ID, 10000)
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
		given(auctionMemberRepository.findById(BIDDER_ID)).willReturn(Optional.of(bidder));

		Auction auction = Auction.builder()
			.productId(PRODUCT_ID)
			.startPrice(5000)
			.tickSize(1000)
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusHours(1))
			.build();

		// [수정] Auction ID 설정 추가
		ReflectionTestUtils.setField(auction, "id", AUCTION_ID);
		auction.startAuction();
		auction.updateCurrentPrice(10000); // 현재가 설정

		given(auctionRepository.findByIdWithLock(AUCTION_ID)).willReturn(Optional.of(auction));

		Product product = Product.builder()
			.sellerId(SELLER_ID)
			.build();
		given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

		// [수정] 이 테스트에서도 validateBid가 호출되므로 Mocking 필요 (ID가 설정되었으므로 정상 매칭됨)
		given(bidRepository.findTopByAuctionIdOrderByBidTimeDesc(AUCTION_ID)).willReturn(Optional.empty());

		// when & then
		// 10,500원 입찰 시도 (11,000원보다 낮음)
		assertThatThrownBy(() ->
			auctionCreateBidUseCase.createBid(AUCTION_ID, BIDDER_ID, 10500)
		)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUCTION_BID_AMOUNT_TOO_LOW);
	}
}