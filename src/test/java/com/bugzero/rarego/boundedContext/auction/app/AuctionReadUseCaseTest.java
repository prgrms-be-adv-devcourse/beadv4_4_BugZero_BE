package com.bugzero.rarego.boundedContext.auction.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
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
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductImage;
import com.bugzero.rarego.boundedContext.product.out.ProductImageRepository;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.dto.AuctionDetailResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderResponseDto;

@ExtendWith(MockitoExtension.class)
class AuctionReadUseCaseTest {

	@InjectMocks
	private AuctionReadUseCase auctionReadUseCase;

	@Mock
	private AuctionRepository auctionRepository;
	@Mock
	private BidRepository bidRepository;
	@Mock
	private AuctionOrderRepository auctionOrderRepository;
	@Mock
	private ProductRepository productRepository;
	@Mock
	private AuctionMemberRepository auctionMemberRepository;
	@Mock
	private ProductImageRepository productImageRepository;

	// --- 1. 경매 상세 조회 (getAuctionDetail) 테스트 ---

	@Test
	@DisplayName("상세 조회 - 로그인 회원(publicId 전달), 입찰 기록 있음")
	void getAuctionDetail_member_withBid() {
		// given
		Long auctionId = 1L;
		String memberPublicId = "user_pub_id_1";
		Long memberId = 10L;

		// 1. 회원 조회 Mocking
		AuctionMember member = AuctionMember.builder()
			.publicId(memberPublicId)
			.build();
		ReflectionTestUtils.setField(member, "id", memberId);

		// 2. 경매 정보 (NPE 방지를 위해 int 필드 필수 설정)
		Auction auction = Auction.builder()
			.productId(50L)
			.startPrice(1000)   // 필수
			.durationDays(3)    // [수정] NPE 원인 해결
			.endTime(LocalDateTime.now().plusDays(1))
			.build();
		ReflectionTestUtils.setField(auction, "id", auctionId);
		ReflectionTestUtils.setField(auction, "currentPrice", 6000);
		ReflectionTestUtils.setField(auction, "status", AuctionStatus.IN_PROGRESS);

		// 3. 입찰 정보
		Bid highestBid = Bid.builder().bidAmount(6000).bidderId(99L).build();
		Bid myLastBid = Bid.builder().bidAmount(5000).bidderId(memberId).build();

		// Mock Behavior
		given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));
		given(auctionMemberRepository.findByPublicId(memberPublicId)).willReturn(Optional.of(member));
		given(bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auctionId)).willReturn(Optional.of(highestBid));
		given(bidRepository.findTopByAuctionIdAndBidderIdOrderByBidAmountDesc(auctionId, memberId))
			.willReturn(Optional.of(myLastBid));

		// when
		AuctionDetailResponseDto result = auctionReadUseCase.getAuctionDetail(auctionId, memberPublicId);

		// then
		assertThat(result.myParticipation().hasBid()).isTrue();
		assertThat(result.myParticipation().myLastBidPrice()).isEqualTo(5000);
		assertThat(result.bid().isMyHighestBid()).isFalse();
	}

	// --- 2. 낙찰 기록 상세 조회 (getAuctionOrder) 테스트 ---

	@Test
	@DisplayName("낙찰 상세 조회 - 낙찰자(BUYER)가 조회 시 정상 반환")
	void getAuctionOrder_asBuyer() {
		// given
		Long auctionId = 1L;
		String buyerPublicId = "buyer_pub_id";
		Long buyerId = 10L;
		Long sellerId = 20L;

		// 1. 회원(구매자) 조회 Mocking
		AuctionMember buyer = AuctionMember.builder()
			.publicId(buyerPublicId)
			.nickname("BuyerNick")
			.build();
		ReflectionTestUtils.setField(buyer, "id", buyerId);

		// 2. 주문 정보 (낙찰자는 buyerId)
		AuctionOrder order = AuctionOrder.builder()
			.auctionId(auctionId)
			.bidderId(buyerId)
			.sellerId(sellerId)
			.finalPrice(10000) // [수정] NPE 방지 (int 필드)
			.build();
		ReflectionTestUtils.setField(order, "id", 777L);
		ReflectionTestUtils.setField(order, "status", AuctionOrderStatus.PROCESSING);

		// 3. 관련 데이터 (경매) - NPE 방지
		Auction auction = Auction.builder()
			.productId(50L)
			.startPrice(1000)  // [수정] 필수
			.durationDays(3)   // [수정] NPE 원인 해결
			.build();
		ReflectionTestUtils.setField(auction, "id", auctionId);

		Product product = Product.builder().sellerId(sellerId).name("Test Item").build();
		ReflectionTestUtils.setField(product, "id", 50L);

		ProductImage image = ProductImage.builder().product(product).imageUrl("thumb.jpg").build();

		AuctionMember sellerMember = AuctionMember.builder()
			.id(sellerId)
			.publicId("seller_pub_id")
			.nickname("sellerNick")
			.build();

		// Mock Behavior
		given(auctionMemberRepository.findByPublicId(buyerPublicId)).willReturn(Optional.of(buyer));
		given(auctionOrderRepository.findByAuctionId(auctionId)).willReturn(Optional.of(order));
		given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));
		given(productRepository.findById(50L)).willReturn(Optional.of(product));
		given(productImageRepository.findAllByProductId(50L)).willReturn(List.of(image));
		given(auctionMemberRepository.findById(sellerId)).willReturn(Optional.of(sellerMember));

		// when
		AuctionOrderResponseDto result = auctionReadUseCase.getAuctionOrder(auctionId, buyerPublicId);

		// then
		assertThat(result.viewerRole()).isEqualTo("BUYER");
		assertThat(result.orderId()).isEqualTo(777L);
		assertThat(result.trader().nickname()).isEqualTo("seller_pub_id");
	}

	@Test
	@DisplayName("낙찰 상세 조회 - 권한 없는 제3자가 조회 시 예외 발생")
	void getAuctionOrder_forbidden() {
		// given
		Long auctionId = 1L;
		String strangerPublicId = "stranger_pub_id";
		Long strangerId = 99L;
		Long buyerId = 10L;
		Long sellerId = 20L;

		// 1. 제3자 회원 조회 Mocking
		AuctionMember stranger = AuctionMember.builder().publicId(strangerPublicId).build();
		ReflectionTestUtils.setField(stranger, "id", strangerId);

		// 2. 주문 정보 (NPE 방지)
		AuctionOrder order = AuctionOrder.builder()
			.auctionId(auctionId)
			.bidderId(buyerId)
			.sellerId(sellerId)
			.finalPrice(10000) // [수정] NPE 원인 해결
			.build();
		ReflectionTestUtils.setField(order, "id", 777L);

		// 3. 경매 정보 (NPE 방지)
		Auction auction = Auction.builder()
			.productId(50L)
			.startPrice(1000) // [수정]
			.durationDays(3)  // [수정] NPE 원인 해결
			.build();

		// Mock Behavior
		given(auctionMemberRepository.findByPublicId(strangerPublicId)).willReturn(Optional.of(stranger));
		given(auctionOrderRepository.findByAuctionId(auctionId)).willReturn(Optional.of(order));
		given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));

		// when & then
		assertThatThrownBy(() -> auctionReadUseCase.getAuctionOrder(auctionId, strangerPublicId))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.AUCTION_ORDER_ACCESS_DENIED);
	}
}