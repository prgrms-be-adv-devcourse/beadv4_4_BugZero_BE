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
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionDetailResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionSearchCondition;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import jakarta.persistence.criteria.*;

@ExtendWith(MockitoExtension.class)
class AuctionReadUseCaseTest {

	@InjectMocks
	private AuctionReadUseCase auctionReadUseCase;

	// [추가] 리팩토링으로 추가된 Support Mock
	@Mock
	private AuctionSupport support;

	// [유지] Bulk 조회 등에 여전히 쓰이는 Mock들
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

		// 1. 회원 Mock
		AuctionMember member = AuctionMember.builder().publicId(memberPublicId).build();
		ReflectionTestUtils.setField(member, "id", memberId);

		// 2. 경매 Mock
		Auction auction = Auction.builder()
			.productId(50L)
			.startPrice(1000)   // 필수
			.durationDays(3)    // [수정] NPE 원인 해결
			.endTime(LocalDateTime.now().plusDays(1))
			.build();
		ReflectionTestUtils.setField(auction, "id", auctionId);
		ReflectionTestUtils.setField(auction, "currentPrice", 6000);
		ReflectionTestUtils.setField(auction, "status", AuctionStatus.IN_PROGRESS);

		// 3. 입찰 Mock
		Bid highestBid = Bid.builder().bidAmount(6000).bidderId(99L).build();
		Bid myLastBid = Bid.builder().bidAmount(5000).bidderId(memberId).build();

		// [변경] Repository -> Support로 Mocking 대상 변경
		given(support.getMember(memberPublicId)).willReturn(member);
		given(support.findAuctionById(auctionId)).willReturn(auction);

		// [유지] BidRepository는 UseCase가 직접 사용함
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

		// 1. 회원(구매자)
		AuctionMember buyer = AuctionMember.builder().publicId(buyerPublicId).nickname("BuyerNick").build();
		ReflectionTestUtils.setField(buyer, "id", buyerId);

		// 2. 주문 정보
		AuctionOrder order = AuctionOrder.builder()
			.auctionId(auctionId)
			.bidderId(buyerId)
			.sellerId(sellerId)
			.finalPrice(10000)
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

		// 4. 상품 정보
		Product product = Product.builder().sellerId(sellerId).name("Test Item").build();
		ReflectionTestUtils.setField(product, "id", 50L);

		ProductImage image = ProductImage.builder().product(product).imageUrl("thumb.jpg").build();

		// 5. 판매자 정보
		AuctionMember sellerMember = AuctionMember.builder().id(sellerId).publicId("seller_pub_id").nickname("sellerNick").build();

		// [변경] Repository -> Support Mocking
		given(support.getMember(buyerPublicId)).willReturn(buyer);   // 1. 요청자 조회
		given(support.getOrder(auctionId)).willReturn(order);        // 2. 주문 조회
		given(support.findAuctionById(auctionId)).willReturn(auction);    // 3. 경매 조회
		given(support.getProduct(50L)).willReturn(product);          // 4. 상품 조회
		given(support.getMember(sellerId)).willReturn(sellerMember); // 5. 거래상대 조회

		// [유지] ProductImage는 UseCase가 직접 조회
		given(productImageRepository.findAllByProductId(50L)).willReturn(List.of(image));

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

		// 1. 제3자 회원
		AuctionMember stranger = AuctionMember.builder().publicId(strangerPublicId).build();
		ReflectionTestUtils.setField(stranger, "id", strangerId);

		// 2. 주문
		AuctionOrder order = AuctionOrder.builder().auctionId(auctionId).bidderId(buyerId).sellerId(sellerId).finalPrice(10000).build();
		ReflectionTestUtils.setField(order, "id", 777L);
    
		// 3. 경매 정보 (NPE 방지)
		Auction auction = Auction.builder()
			.productId(50L)
			.startPrice(1000) // [수정]
			.durationDays(3)  // [수정] NPE 원인 해결
			.build();

		// [변경] Support Mocking
		given(support.getMember(strangerPublicId)).willReturn(stranger);
		given(support.getOrder(auctionId)).willReturn(order);
		given(support.findAuctionById(auctionId)).willReturn(auction);

		// when & then
		assertThatThrownBy(() -> auctionReadUseCase.getAuctionOrder(auctionId, strangerPublicId))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.AUCTION_ORDER_ACCESS_DENIED);
	}

	// --- 3. 경매 목록 조회 테스트 (변경 없음, Repository 직접 사용) ---
	@Test
	@DisplayName("경매 목록 조회 - 썸네일 정렬(sortOrder) 및 데이터 조립 확인")
	void getAuctions_success() {
		// given
		AuctionSearchCondition condition = new AuctionSearchCondition();
		Pageable pageable = PageRequest.of(0, 10);

		Long auctionId1 = 1L;
		Long productId1 = 100L;
		Auction auction1 = Auction.builder()
			.productId(productId1).startPrice(1000).tickSize(100).durationDays(3).build();
		ReflectionTestUtils.setField(auction1, "id", auctionId1);
		ReflectionTestUtils.setField(auction1, "currentPrice", 2000);
		ReflectionTestUtils.setField(auction1, "status", AuctionStatus.IN_PROGRESS);

		Page<Auction> auctionPage = new PageImpl<>(List.of(auction1), pageable, 1);
		Product product1 = Product.builder().name("Test Product").build();
		ReflectionTestUtils.setField(product1, "id", productId1);
		List<Object[]> bidCounts = List.<Object[]>of(new Object[]{auctionId1, 5L});

		ProductImage img1 = ProductImage.builder().product(product1).imageUrl("url_2.jpg").sortOrder(2).build();
		ProductImage img2 = ProductImage.builder().product(product1).imageUrl("url_0.jpg").sortOrder(0).build();
		ProductImage img3 = ProductImage.builder().product(product1).imageUrl("url_1.jpg").sortOrder(1).build();

		// [수정] findAll -> findAllApproved 로 변경
		// 조건이 없으므로 인자는 (null, null, pageable)
		given(auctionRepository.findAllApproved(isNull(), isNull(), any(Pageable.class)))
			.willReturn(auctionPage);

		given(productRepository.findAllById(Set.of(productId1))).willReturn(List.of(product1));
		given(bidRepository.countByAuctionIdIn(Set.of(auctionId1))).willReturn(bidCounts);
		given(productImageRepository.findAllByProductIdIn(Set.of(productId1))).willReturn(List.of(img1, img2, img3));

		// when
		PagedResponseDto<AuctionListResponseDto> result = auctionReadUseCase.getAuctions(condition, pageable);

		// then
		assertThat(result.data()).hasSize(1);
		AuctionListResponseDto dto = result.data().get(0);
		assertThat(dto.thumbnailUrl()).isEqualTo("url_0.jpg");
	}

	@Test
	@DisplayName("경매 목록 조회 - 결과가 없을 때 빈 페이지 반환")
	void getAuctions_empty() {
		AuctionSearchCondition condition = new AuctionSearchCondition();
		Pageable pageable = PageRequest.of(0, 10);
		Page<Auction> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

		// [수정] findAll -> findAllApproved 로 변경
		given(auctionRepository.findAllApproved(any(), any(), any(Pageable.class)))
			.willReturn(emptyPage);

		PagedResponseDto<AuctionListResponseDto> result = auctionReadUseCase.getAuctions(condition, pageable);

		assertThat(result.data()).isEmpty();
		assertThat(result.pageDto().totalItems()).isEqualTo(0);
	}

	
}
