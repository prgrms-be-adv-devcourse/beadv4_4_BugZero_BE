package com.bugzero.rarego.boundedContext.auction.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

		// [수정] 빌더에서 .id() 제거하고 ReflectionTestUtils 사용
		AuctionMember bidder = AuctionMember.builder().publicId("user_123").build();
		ReflectionTestUtils.setField(bidder, "id", BIDDER_ID);

		Product product = Product.builder().sellerId(SELLER_ID).build();
		ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

		Auction auction = Auction.builder()
			.product(product)
			.startPrice(startPrice)
			.tickSize(1000)
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusHours(1))
			.build();
		ReflectionTestUtils.setField(auction, "id", AUCTION_ID);
		auction.startAuction(); // IN_PROGRESS로 변경

		// Mocking
		given(auctionMemberRepository.findById(BIDDER_ID)).willReturn(Optional.of(bidder));
		given(auctionRepository.findByIdWithLock(AUCTION_ID)).willReturn(Optional.of(auction));
		// UseCase 구현에 따라 productRepository가 호출될 수도, auction.getProduct()를 쓸 수도 있음.
		// 안전하게 호출된다고 가정하고 Stubbing (안 쓰이면 UnnecessaryStubbingException 날 수 있으므로 leniency() 권장하거나 제거)
		// given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

		given(bidRepository.findTopByAuctionIdOrderByBidTimeDesc(any())).willReturn(Optional.empty());

		// Payment Client Mock
		given(paymentApiClient.holdDeposit(anyInt(), anyLong(), anyLong()))
			.willReturn(new DepositHoldResponseDto(1L, AUCTION_ID, 1000, "HOLD", LocalDateTime.now()));

		// when
		SuccessResponseDto<BidResponseDto> response = auctionCreateBidUseCase.createBid(AUCTION_ID, BIDDER_ID, bidAmount);

		// then
		// 1. 보증금은 시작가(10000)의 10%인 1000원이어야 함
		verify(paymentApiClient).holdDeposit(eq(1000), eq(BIDDER_ID), eq(AUCTION_ID));

		// 2. 입찰 정보 저장되었는지 확인
		verify(bidRepository).save(any(Bid.class));

		// 3. 경매 현재가 업데이트 확인
		assertThat(auction.getCurrentPrice()).isEqualTo(bidAmount);
	}

	@Test
	@DisplayName("입찰 실패: 판매자가 본인 경매에 입찰 시도")
	void createBid_fail_seller_bid() {
		// given
		AuctionMember seller = AuctionMember.builder().build();
		ReflectionTestUtils.setField(seller, "id", SELLER_ID);
		given(auctionMemberRepository.findById(SELLER_ID)).willReturn(Optional.of(seller));

		Product product = Product.builder().sellerId(SELLER_ID).build();
		ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

		Auction auction = Auction.builder()
			.product(product)
			.startPrice(10000)
			.tickSize(1000)
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusHours(1))
			.build();
		ReflectionTestUtils.setField(auction, "id", AUCTION_ID);
		auction.startAuction();

		given(auctionRepository.findByIdWithLock(AUCTION_ID)).willReturn(Optional.of(auction));
		// given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product)); // 필요시 추가
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
		ReflectionTestUtils.setField(bidder, "id", BIDDER_ID);
		given(auctionMemberRepository.findById(BIDDER_ID)).willReturn(Optional.of(bidder));

		Product product = Product.builder().sellerId(SELLER_ID).build();
		ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

		Auction auction = Auction.builder()
			.product(product)
			.startPrice(5000)
			.tickSize(1000)
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusHours(1))
			.build();
		ReflectionTestUtils.setField(auction, "id", AUCTION_ID);
		auction.startAuction();
		auction.updateCurrentPrice(10000); // 현재가 10,000

		given(auctionRepository.findByIdWithLock(AUCTION_ID)).willReturn(Optional.of(auction));
		// given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product)); // 필요시 추가
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