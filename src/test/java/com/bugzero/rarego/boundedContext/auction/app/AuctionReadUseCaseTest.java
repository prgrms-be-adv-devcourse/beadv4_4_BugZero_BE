package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.*;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistListResponseDto;
import com.bugzero.rarego.boundedContext.auction.out.*;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductImage;
import com.bugzero.rarego.boundedContext.product.out.ProductImageRepository;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionDetailResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionListResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionSearchCondition;
import jakarta.persistence.criteria.*;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;


@ExtendWith(MockitoExtension.class)
class AuctionReadUseCaseTest {

    @InjectMocks
    private AuctionReadUseCase auctionReadUseCase;

    @Mock
    private AuctionSupport support;

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
                .sellerId(20L)
                .startPrice(1000)
                .durationDays(3)
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

        // 3. 경매 정보
        Auction auction = Auction.builder().productId(50L).startPrice(1000).durationDays(3).build();

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

        // 3. 경매
        Auction auction = Auction.builder().productId(50L).startPrice(1000).durationDays(3).build();

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
    @DisplayName("경매 목록 조회 - 썸네일 정렬 및 데이터 조립 확인")
    void getAuctions_success() {
        // given
        AuctionSearchCondition condition = new AuctionSearchCondition();
        Pageable pageable = PageRequest.of(0, 10);

        Long auctionId = 1L;
        Long productId = 100L;
        Auction auction = Auction.builder()
                .productId(productId)
                .startPrice(1000)
                .durationDays(3)
                .build();
        ReflectionTestUtils.setField(auction, "id", auctionId);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.IN_PROGRESS);

        Page<Auction> auctionPage = new PageImpl<>(List.of(auction), pageable, 1);
        Product product = Product.builder().name("Test Product").build();
        ReflectionTestUtils.setField(product, "id", productId);

        List<Object[]> bidCounts = new ArrayList<>();
        bidCounts.add(new Object[]{auctionId, 5L});

        ProductImage img1 = ProductImage.builder().product(product).imageUrl("url_2.jpg").sortOrder(2).build();
        ProductImage img2 = ProductImage.builder().product(product).imageUrl("url_0.jpg").sortOrder(0).build();
        ProductImage img3 = ProductImage.builder().product(product).imageUrl("url_1.jpg").sortOrder(1).build();

        // Mocking (private 메서드 내부에서 호출되는 Repository들)
        given(auctionRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(auctionPage);
        given(productRepository.findAllById(Set.of(productId))).willReturn(List.of(product));
        given(bidRepository.countByAuctionIdIn(Set.of(auctionId))).willReturn(bidCounts);
        given(productImageRepository.findAllByProductIdIn(Set.of(productId))).willReturn(List.of(img1, img2, img3));

        // when
        PagedResponseDto<AuctionListResponseDto> result = auctionReadUseCase.getAuctions(condition, pageable);

        // then
        assertThat(result.data()).hasSize(1);
        // convertToAuctionListDtos가 sortOrder가 낮은(0) img2를 잘 선택했는지 검증
        assertThat(result.data().get(0).thumbnailUrl()).isEqualTo("url_0.jpg");
    }

    @Test
    @DisplayName("경매 목록 조회 - 결과가 없을 때 빈 페이지 반환")
    void getAuctions_empty() {
        AuctionSearchCondition condition = new AuctionSearchCondition();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Auction> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        given(auctionRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(emptyPage);

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
        Auction auction = Auction.builder().productId(50L).startPrice(10000).durationDays(3).build();
        ReflectionTestUtils.setField(auction, "id", 1L);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.IN_PROGRESS);
        Page<Auction> auctionPage = new PageImpl<>(List.of(auction), pageable, 1);

        given(auctionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(auctionPage);

        // [Mock Setup 3] 연관 데이터 (DTO 조립용)
        Product product = Product.builder().name("Galaxy Lego").build();
        ReflectionTestUtils.setField(product, "id", 50L);
        given(productRepository.findAllById(Set.of(50L))).willReturn(List.of(product));
        given(bidRepository.countByAuctionIdIn(Set.of(1L))).willReturn(List.<Object[]>of(new Object[]{1L, 5L}));
        given(productImageRepository.findAllByProductIdIn(Set.of(50L))).willReturn(Collections.emptyList());

        // [추가] JPA Criteria 객체 Mocking (Specification 강제 실행을 위함)
        Root<Auction> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> path = mock(Path.class);

        // JPA 내부 호출 시 NPE 방지용 Stubbing
        given(root.get(anyString())).willReturn(path);
        given(path.in(anyCollection())).willReturn(mock(Predicate.class));

        // [핵심 수정 1] equal 오버로딩 문제 해결
        // any() -> any(Object.class)로 변경하여 (Expression, Object) 메서드가 호출되도록 강제함
        given(cb.equal(any(), any(Object.class))).willReturn(mock(Predicate.class));

        // [핵심 수정 2] and 가변인자 문제 해결 (이전 단계에서 적용함)
        given(cb.and(any(Predicate[].class))).willReturn(mock(Predicate.class));

        // when
        PagedResponseDto<AuctionListResponseDto> result = auctionReadUseCase.getAuctions(condition, pageable);

        // then
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).productName()).isEqualTo("Galaxy Lego");

        // [핵심 수정] Specification을 캡처하여 내부 로직 강제 실행
        ArgumentCaptor<Specification<Auction>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(auctionRepository).findAll(specCaptor.capture(), any(Pageable.class));

        Specification<Auction> capturedSpec = specCaptor.getValue();

        // 강제 실행! -> 이때 내부의 productRepository.findIdsBySearchCondition()이 호출됨
        capturedSpec.toPredicate(root, query, cb);

        verify(productRepository).findIdsBySearchCondition(eq("Galaxy"), isNull());
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
        given(support.getMember(publicId)).willReturn(member);
        given(auctionBookmarkRepository.findAllByMemberId(memberId, pageable)).willReturn(bookmarkPage);
        given(auctionRepository.findAllById(List.of(100L))).willReturn(List.of(auction));

        // 공통 변환 로직(convertToAuctionListDtos) 내부에서 호출하는 Mock들
        given(productRepository.findAllById(Set.of(50L))).willReturn(List.of(product));
        given(bidRepository.countByAuctionIdIn(Set.of(100L))).willReturn(Arrays.asList(new Object[][]{{100L, 3L}}));
        given(productImageRepository.findAllByProductIdIn(Set.of(50L))).willReturn(Collections.emptyList());

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
}
