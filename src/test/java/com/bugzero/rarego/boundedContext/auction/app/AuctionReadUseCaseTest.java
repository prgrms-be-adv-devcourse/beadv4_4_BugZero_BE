package com.bugzero.rarego.boundedContext.auction.app;

import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.auction.domain.*;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistListResponseDto;
import com.bugzero.rarego.boundedContext.auction.out.*;
import com.bugzero.rarego.boundedContext.product.app.ProductCreateS3PresignerUrlUseCase;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductImage;
import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import com.bugzero.rarego.boundedContext.product.out.ProductImageRepository;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionDetailResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionListResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionSearchCondition;
import com.bugzero.rarego.shared.auction.dto.MyAuctionOrderListResponseDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
	@Mock
	private AuctionBookmarkRepository auctionBookmarkRepository;
	@Mock
	private ProductCreateS3PresignerUrlUseCase s3PresignerUrlUseCase;

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
			.sellerId(100L)
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
		given(support.getPublicMember(memberPublicId)).willReturn(member);
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

		ProductMember productSeller = ProductMember.builder().build();
		ReflectionTestUtils.setField(productSeller, "id", sellerId);

		// 4. 상품 정보
		Product product = Product.builder().seller(productSeller).name("Test Item").build();
		ReflectionTestUtils.setField(product, "id", 50L);

		ProductImage image = ProductImage.builder().product(product).imageUrl("thumb.jpg").build();

		// 5. 판매자 정보
		AuctionMember sellerMember = AuctionMember.builder().id(sellerId).publicId("seller_pub_id").nickname("sellerNick").build();

		// [변경] Repository -> Support Mocking
		given(support.getPublicMember(buyerPublicId)).willReturn(buyer);   // 1. 요청자 조회
		given(support.getOrder(auctionId)).willReturn(order);        // 2. 주문 조회
		given(support.findAuctionById(auctionId)).willReturn(auction);    // 3. 경매 조회
		given(support.getProduct(50L)).willReturn(product);          // 4. 상품 조회
		given(support.getMember(sellerId)).willReturn(sellerMember); // 5. 거래상대 조회

		// [유지] ProductImage는 UseCase가 직접 조회
		given(productImageRepository.findAllByProductId(50L)).willReturn(List.of(image));

		// S3 Presigned URL 변환 Mocking
		lenient().when(s3PresignerUrlUseCase.getPresignedGetUrl(anyString()))
				.thenAnswer(invocation -> invocation.getArgument(0));

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
		given(support.getPublicMember(strangerPublicId)).willReturn(stranger);
		given(support.getOrder(auctionId)).willReturn(order);
		given(support.findAuctionById(auctionId)).willReturn(auction);

		// when & then
		assertThatThrownBy(() -> auctionReadUseCase.getAuctionOrder(auctionId, strangerPublicId))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.AUCTION_ORDER_ACCESS_DENIED);
	}

	// --- 3. 경매 목록 조회 테스트 (변경 없음, Repository 직접 사용) ---
	@Test
	@DisplayName("경매 목록 조회 - 검색 조건에 맞는 경매 반환")
	void getAuctions_success() {
		// [Given]
		// 1. 검색 조건 설정 (예: 상태가 IN_PROGRESS인 것만 조회)
		AuctionSearchCondition condition = new AuctionSearchCondition();
		ReflectionTestUtils.setField(condition, "status", AuctionStatus.IN_PROGRESS);

		Pageable pageable = PageRequest.of(0, 10);

		// 2. Mock 데이터 준비
		Auction auction = Auction.builder()
			.productId(1L).sellerId(1L).startPrice(1000).durationDays(3).build();
		ReflectionTestUtils.setField(auction, "id", 100L);

		// 3. Repository Mocking (핵심 변경 부분)
		// findAllBySearchConditions 호출 시 기대값 설정
		given(auctionRepository.findAllBySearchConditions(
			eq(condition.getIds()),       // ids (null 가능)
			eq(condition.getStatus()),    // status
			isNull(),                     // productIds (검색어 없으므로 null)
			any(Pageable.class)
		)).willReturn(new PageImpl<>(List.of(auction), pageable, 1));

		// 연관 데이터 Mocking (Product, BidCount 등 - 기존과 동일)
		Product product = Product.builder().name("Test Product").build();
		given(productRepository.findAllById(anySet())).willReturn(List.of(product));
		given(bidRepository.countByAuctionIdIn(anySet())).willReturn(List.of());
		given(productImageRepository.findAllByProductIdIn(anySet())).willReturn(List.of());

		// S3 Presigned URL 변환 Mocking
		lenient().when(s3PresignerUrlUseCase.getPresignedGetUrl(anyString()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// [When]
		PagedResponseDto<AuctionListResponseDto> result = auctionReadUseCase.getAuctions(condition, pageable);

		// [Then]
		assertThat(result.data()).hasSize(1);
		assertThat(result.data().get(0).auctionId()).isEqualTo(100L);

		// Verify 호출 확인
		verify(auctionRepository).findAllBySearchConditions(
			eq(condition.getIds()),
			eq(AuctionStatus.IN_PROGRESS),
			isNull(),
			any(Pageable.class)
		);
	}

	@Test
	@DisplayName("경매 목록 조회 - 결과가 없을 때 빈 페이지 반환")
	void getAuctions_empty() {
		AuctionSearchCondition condition = new AuctionSearchCondition();
		Pageable pageable = PageRequest.of(0, 10);
		Page<Auction> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

		// [수정] findAll -> findAllApproved 로 변경
		given(auctionRepository.findAllBySearchConditions(
			isNull(),            // 1. auctionIds (조건 없으므로 null)
			isNull(),            // 2. status (조건 없으므로 null)
			isNull(),            // 3. productIds (조건 없으므로 null)
			any(Pageable.class)  // 4. pageable
		)).willReturn(emptyPage);

		PagedResponseDto<AuctionListResponseDto> result = auctionReadUseCase.getAuctions(condition, pageable);

		assertThat(result.data()).isEmpty();
		assertThat(result.pageDto().totalItems()).isEqualTo(0);
	}

	@Test
	@DisplayName("경매 목록 조회 - 검색 조건(키워드+상태)이 있을 때 상품 검색 후 경매 조회 수행")
	void getAuctions_with_search_condition() {
		// given
		AuctionSearchCondition condition = new AuctionSearchCondition();
		ReflectionTestUtils.setField(condition, "keyword", "Galaxy");
		ReflectionTestUtils.setField(condition, "status", AuctionStatus.IN_PROGRESS);

		Pageable pageable = PageRequest.of(0, 10);

		// [Mock Setup 1] 키워드 검색 결과
		List<Long> matchedProductIds = List.of(50L);
		given(productRepository.findIdsBySearchCondition(eq("Galaxy"), isNull()))
			.willReturn(matchedProductIds);

		// [Mock Setup 2] 경매 조회 결과
		Auction auction = Auction.builder()
			.productId(50L)
			.startPrice(10000)
			.durationDays(3)
			.build();
		ReflectionTestUtils.setField(auction, "id", 1L);
		ReflectionTestUtils.setField(auction, "status", AuctionStatus.IN_PROGRESS);
		Page<Auction> auctionPage = new PageImpl<>(List.of(auction), pageable, 1);

		// [수정] findAll -> findAllApproved 로 변경!
		// Specification 관련 Mocking은 이제 필요 없습니다.
		given(auctionRepository.findAllBySearchConditions(
			isNull(),                       // ✅ [추가됨] 1. auctionIds (현재 조건에는 없으므로 null)
			eq(AuctionStatus.IN_PROGRESS),  // 2. status
			eq(matchedProductIds),          // 3. productIds
			any(Pageable.class))
		).willReturn(auctionPage);

		// [Mock Setup 3] 연관 데이터 (DTO 조립용)
		Product product = Product.builder().name("Galaxy Lego").build();
		ReflectionTestUtils.setField(product, "id", 50L);
		given(productRepository.findAllById(Set.of(50L))).willReturn(List.of(product));
		given(bidRepository.countByAuctionIdIn(Set.of(1L))).willReturn(List.<Object[]>of(new Object[] {1L, 5L}));
		given(productImageRepository.findAllByProductIdIn(Set.of(50L))).willReturn(Collections.emptyList());

		// when
		PagedResponseDto<AuctionListResponseDto> result = auctionReadUseCase.getAuctions(condition, pageable);

		// then
		assertThat(result.data()).hasSize(1);
		assertThat(result.data().get(0).productName()).isEqualTo("Galaxy Lego");

		// [검증] Specification Captor 대신 findAllApproved 호출 여부 검증
		verify(productRepository).findIdsBySearchCondition(eq("Galaxy"), isNull());
		verify(auctionRepository).findAllBySearchConditions(
			isNull(),
			eq(AuctionStatus.IN_PROGRESS),
			eq(matchedProductIds),
			any(Pageable.class)
		);
	}

    @Test
    @DisplayName("관심 경매 조회 - 북마크된 경매 정보와 상품 정보를 조립하여 반환")
    void getMyBookmarks_success() {
        // given
        String publicId = "user-123";
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        // 1. 회원 및 북마크 설정
        AuctionMember member = AuctionMember.builder().publicId(publicId).build();
        ReflectionTestUtils.setField(member, "id", memberId);

        AuctionBookmark bookmark = AuctionBookmark.builder().memberId(memberId).auctionId(100L).build();
        ReflectionTestUtils.setField(bookmark, "id", 1L);
        Page<AuctionBookmark> bookmarkPage = new PageImpl<>(List.of(bookmark), pageable, 1);

        // 2. 경매 및 상품 데이터 설정
        Auction auction = Auction.builder()
                .productId(50L)
                .startPrice(1000)
                .durationDays(3)
                .build();
        ReflectionTestUtils.setField(auction, "id", 100L);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.IN_PROGRESS);

        Product product = Product.builder().name("관심 상품").build();
        ReflectionTestUtils.setField(product, "id", 50L);

        // 3. Mocking
        given(support.getPublicMember(publicId)).willReturn(member);
        given(auctionBookmarkRepository.findAllByMemberId(memberId, pageable)).willReturn(bookmarkPage);
        given(auctionRepository.findAllById(List.of(100L))).willReturn(List.of(auction));

        // 공통 변환 로직(convertToAuctionListDtos) 내부에서 호출하는 Mock들
        given(productRepository.findAllById(Set.of(50L))).willReturn(List.of(product));
        given(bidRepository.countByAuctionIdIn(Set.of(100L))).willReturn(Arrays.asList(new Object[][]{{100L, 3L}}));
        given(productImageRepository.findAllByProductIdIn(Set.of(50L))).willReturn(Collections.emptyList());

		// S3 Presigned URL 변환 Mocking
		lenient().when(s3PresignerUrlUseCase.getPresignedGetUrl(anyString()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// when
		PagedResponseDto<WishlistListResponseDto> result = auctionReadUseCase.getMyBookmarks(publicId, pageable);

        // then
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).bookmarkId()).isEqualTo(1L);
        assertThat(result.data().get(0).auctionInfo().productName()).isEqualTo("관심 상품");
        assertThat(result.data().get(0).auctionInfo().auctionId()).isEqualTo(100L);

        verify(auctionBookmarkRepository).findAllByMemberId(memberId, pageable);
        verify(auctionRepository).findAllById(anyCollection());
    }

	@Test
	@DisplayName("나의 낙찰 목록 조회 - 성공 (데이터 조립 및 정렬 확인)")
	void getMyAuctionOrders_success() {
		// given
		String memberPublicId = "user_pub_id";
		AuctionOrderStatus status = AuctionOrderStatus.PROCESSING;
		Pageable pageable = PageRequest.of(0, 10);

		// 1. Member Mock
		AuctionMember member = AuctionMember.builder().publicId(memberPublicId).build();
		ReflectionTestUtils.setField(member, "id", 10L);

		// 2. Order Mock (낙찰 내역)
		AuctionOrder order = AuctionOrder.builder()
			.auctionId(1L)
			.bidderId(10L)
			.finalPrice(50000)
			.build();
		ReflectionTestUtils.setField(order, "id", 100L);
		ReflectionTestUtils.setField(order, "status", AuctionOrderStatus.PROCESSING); // 결제 대기
		ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.now());

		Page<AuctionOrder> orderPage = new PageImpl<>(List.of(order), pageable, 1);

		// 3. Auction Mock
		Auction auction = Auction.builder().productId(50L).startPrice(1000).durationDays(3).build();
		ReflectionTestUtils.setField(auction, "id", 1L);

		// 4. Product Mock
		Product product = Product.builder().name("Lego Titanic").build();
		ReflectionTestUtils.setField(product, "id", 50L);

		// 5. Image Mock (썸네일 정렬 확인용)
		ProductImage img1 = ProductImage.builder().product(product).imageUrl("thumb.jpg").sortOrder(0).build();
		ProductImage img2 = ProductImage.builder().product(product).imageUrl("detail.jpg").sortOrder(1).build();
		ReflectionTestUtils.setField(img1, "product", product);

		// Mocking Behavior
		given(support.getPublicMember(memberPublicId)).willReturn(member);

		// [핵심] 레포지토리 호출
		given(auctionOrderRepository.findAllByBidderIdAndStatus(eq(10L), eq(status), any(Pageable.class)))
			.willReturn(orderPage);

		// 연관 데이터 조회 Mocking
		given(auctionRepository.findAllById(Set.of(1L))).willReturn(List.of(auction));
		given(productRepository.findAllByIdIn(Set.of(50L))).willReturn(List.of(product));
		given(productImageRepository.findAllByProductIdIn(Set.of(50L))).willReturn(List.of(img1, img2));

		// S3 Presigned URL 변환 Mocking
		lenient().when(s3PresignerUrlUseCase.getPresignedGetUrl(anyString()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// when
		PagedResponseDto<MyAuctionOrderListResponseDto> result =
			auctionReadUseCase.getMyAuctionOrders(memberPublicId, status, pageable);

		// then
		assertThat(result.data()).hasSize(1);
		MyAuctionOrderListResponseDto dto = result.data().get(0);

		// 데이터 매핑 검증
		assertThat(dto.orderId()).isEqualTo(100L);
		assertThat(dto.productName()).isEqualTo("Lego Titanic"); // 상품명 확인
		assertThat(dto.thumbnailUrl()).isEqualTo("thumb.jpg");   // 썸네일(sortOrder=0) 확인
		assertThat(dto.finalPrice()).isEqualTo(50000);
		assertThat(dto.statusDescription()).isEqualTo("결제 대기중"); // 한글 변환 확인
		assertThat(dto.auctionRequired()).isTrue();
	}

	@Test
	@DisplayName("나의 낙찰 목록 조회 - 내역이 없을 때 빈 리스트 반환 (Early Return)")
	void getMyAuctionOrders_empty() {
		// given
		String memberPublicId = "user_pub_id";
		Pageable pageable = PageRequest.of(0, 10);

		AuctionMember member = AuctionMember.builder().publicId(memberPublicId).build();
		ReflectionTestUtils.setField(member, "id", 10L);

		// 빈 페이지 생성
		Page<AuctionOrder> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

		given(support.getPublicMember(memberPublicId)).willReturn(member);

		// status가 null인 경우로 테스트
		given(auctionOrderRepository.findAllByBidderIdAndStatus(eq(10L), isNull(), any(Pageable.class)))
			.willReturn(emptyPage);

		// when
		PagedResponseDto<MyAuctionOrderListResponseDto> result =
			auctionReadUseCase.getMyAuctionOrders(memberPublicId, null, pageable);

		// then
		assertThat(result.data()).isEmpty();
		assertThat(result.pageDto().totalItems()).isEqualTo(0);

		// [중요] 최적화 검증: 목록이 비었으므로 다른 리포지토리는 호출되지 않아야 함
		// (Mockito.verify는 선택 사항입니다)
		// verify(auctionRepository, never()).findAllById(any());
	}
}
