package com.bugzero.rarego.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.domain.Auction;
import com.bugzero.rarego.domain.AuctionBookmark;
import com.bugzero.rarego.domain.AuctionMember;
import com.bugzero.rarego.domain.AuctionOrder;
import com.bugzero.rarego.domain.AuctionOrderStatus;
import com.bugzero.rarego.domain.Bid;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.in.dto.AuctionBookmarkListResponseDto;
import com.bugzero.rarego.in.dto.AuctionDetailResponseDto;
import com.bugzero.rarego.in.dto.AuctionFilterType;
import com.bugzero.rarego.in.dto.AuctionListResponseDto;
import com.bugzero.rarego.in.dto.AuctionOrderResponseDto;
import com.bugzero.rarego.in.dto.AuctionSearchCondition;
import com.bugzero.rarego.in.dto.BidLogResponseDto;
import com.bugzero.rarego.in.dto.MyAuctionOrderListResponseDto;
import com.bugzero.rarego.in.dto.MyBidResponseDto;
import com.bugzero.rarego.in.dto.MySaleResponseDto;
import com.bugzero.rarego.out.AuctionBookmarkRepository;
import com.bugzero.rarego.out.AuctionMemberRepository;
import com.bugzero.rarego.out.AuctionOrderRepository;
import com.bugzero.rarego.out.AuctionRepository;
import com.bugzero.rarego.out.BidRepository;
import com.bugzero.rarego.out.es.ProductSearchClient;
import com.bugzero.rarego.shared.auction.type.AuctionStatus;
import com.bugzero.rarego.shared.product.dto.ProductAuctionResponseDto;
import com.bugzero.rarego.shared.product.type.Category;

@ExtendWith(MockitoExtension.class)
class AuctionReadUseCaseTest {

    @InjectMocks
    private AuctionReadUseCase auctionReadUseCase;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private AuctionOrderRepository auctionOrderRepository;

    @Mock
    private AuctionBookmarkRepository auctionBookmarkRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionMemberRepository auctionMemberRepository;

    @Mock
    private ProductSearchClient productSearchClient;

    @Mock
    private AuctionSupport support;

    private Auction auction;
    private final Long auctionId = 1L;
    private final Long productId = 10L;
    private final Long sellerId = 100L;
    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        auction = Auction.builder()
                .productId(productId)
                .sellerId(sellerId)
                .startPrice(10000)
                .durationDays(3)
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(auction, "id", auctionId);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(auction, "currentPrice", 15000);
    }

    // === Helper Methods ===

    private AuctionMember createMember(Long id, String publicId) {
        AuctionMember member = AuctionMember.builder()
                .publicId(publicId)
                .contactPhone("010-1234-5678")
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Bid createBid(Long id, Long auctionId, Long bidderId, int amount) {
        Bid bid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .bidAmount(amount)
                .bidTime(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(bid, "id", id);
        return bid;
    }

    private AuctionOrder createOrder(Long id, Long auctionId, Long sellerId, Long bidderId, int finalPrice) {
        AuctionOrder order = AuctionOrder.builder()
                .auctionId(auctionId)
                .sellerId(sellerId)
                .bidderId(bidderId)
                .finalPrice(finalPrice)
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.now());
        return order;
    }

    private ProductAuctionResponseDto createProductDto(Long id, String name) {
        return ProductAuctionResponseDto.builder()
                .id(id)
                .name(name)
                .description("설명")
                .thumbnailUrl("thumb.jpg")
                .imageUrls(List.of("img1.jpg", "img2.jpg"))
                .category("스타워즈")
                .build();
    }

    // ============================
    // getBidLogs 테스트
    // ============================
    @Nested
    @DisplayName("getBidLogs - 경매 입찰 기록 조회")
    class GetBidLogsTest {

        @Test
        @DisplayName("성공 - 입찰 기록이 있을 때 publicId와 함께 반환")
        void getBidLogs_success() {
            // given
            Long bidderId = 50L;
            Bid bid = createBid(1L, auctionId, bidderId, 15000);

            Page<Bid> bidPage = new PageImpl<>(List.of(bid), pageable, 1);
            given(bidRepository.findAllByAuctionIdOrderByBidTimeDesc(auctionId, pageable)).willReturn(bidPage);

            AuctionMember bidder = createMember(bidderId, "bidder_pub");
            given(auctionMemberRepository.findAllById(Set.of(bidderId))).willReturn(List.of(bidder));

            // when
            PagedResponseDto<BidLogResponseDto> result = auctionReadUseCase.getBidLogs(auctionId, pageable);

            // then
            assertThat(result.data()).hasSize(1);
            BidLogResponseDto dto = result.data().get(0);
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.publicId()).isEqualTo("bidder_pub");
            assertThat(dto.bidAmount()).isEqualTo(15000);
            assertThat(dto.bidTime()).isNotNull();
        }

        @Test
        @DisplayName("빈 결과 - 입찰 기록이 없을 때 빈 리스트 반환")
        void getBidLogs_empty() {
            // given
            Page<Bid> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            given(bidRepository.findAllByAuctionIdOrderByBidTimeDesc(auctionId, pageable)).willReturn(emptyPage);

            // when
            PagedResponseDto<BidLogResponseDto> result = auctionReadUseCase.getBidLogs(auctionId, pageable);

            // then
            assertThat(result.data()).isEmpty();
            verify(auctionMemberRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("입찰자 정보가 없을 때 publicId가 unknown으로 반환")
        void getBidLogs_unknownBidder() {
            // given
            Bid bid = createBid(1L, auctionId, 999L, 20000);
            Page<Bid> bidPage = new PageImpl<>(List.of(bid), pageable, 1);
            given(bidRepository.findAllByAuctionIdOrderByBidTimeDesc(auctionId, pageable)).willReturn(bidPage);

            // 해당 bidder 조회 결과가 없음
            given(auctionMemberRepository.findAllById(Set.of(999L))).willReturn(Collections.emptyList());

            // when
            PagedResponseDto<BidLogResponseDto> result = auctionReadUseCase.getBidLogs(auctionId, pageable);

            // then
            assertThat(result.data()).hasSize(1);
            assertThat(result.data().get(0).publicId()).isEqualTo("unknown");
        }
    }

    // ============================
    // getMyBids 테스트
    // ============================
    @Nested
    @DisplayName("getMyBids - 나의 입찰 내역 조회")
    class GetMyBidsTest {

        @Test
        @DisplayName("성공 - 입찰 내역과 경매 정보를 조립하여 반환")
        void getMyBids_success() {
            // given
            String memberPublicId = "bidder_pub";
            Long memberId = 50L;
            AuctionMember member = createMember(memberId, memberPublicId);

            Bid bid = createBid(1L, auctionId, memberId, 12000);
            Page<Bid> bidPage = new PageImpl<>(List.of(bid), pageable, 1);

            given(support.getPublicMember(memberPublicId)).willReturn(member);
            given(bidRepository.findAllByBidderIdAndAuctionStatus(eq(memberId), isNull(), eq(pageable))).willReturn(bidPage);
            given(auctionRepository.findAllById(Set.of(auctionId))).willReturn(List.of(auction));

            // when
            PagedResponseDto<MyBidResponseDto> result = auctionReadUseCase.getMyBids(memberPublicId, null, pageable);

            // then
            assertThat(result.data()).hasSize(1);
            MyBidResponseDto dto = result.data().get(0);
            assertThat(dto.bidId()).isEqualTo(1L);
            assertThat(dto.auctionId()).isEqualTo(auctionId);
            assertThat(dto.productId()).isEqualTo(productId);
            assertThat(dto.bidAmount()).isEqualTo(12000);
            assertThat(dto.auctionStatus()).isEqualTo(AuctionStatus.IN_PROGRESS);
            assertThat(dto.currentPrice()).isEqualTo(15000);
            assertThat(dto.endTime()).isNotNull();
        }

        @Test
        @DisplayName("빈 결과 - 입찰 내역이 없을 때 빈 리스트 반환")
        void getMyBids_empty() {
            // given
            String memberPublicId = "user_pub";
            AuctionMember member = createMember(10L, memberPublicId);

            Page<Bid> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            given(support.getPublicMember(memberPublicId)).willReturn(member);
            given(bidRepository.findAllByBidderIdAndAuctionStatus(eq(10L), isNull(), eq(pageable))).willReturn(emptyPage);

            // when
            PagedResponseDto<MyBidResponseDto> result = auctionReadUseCase.getMyBids(memberPublicId, null, pageable);

            // then
            assertThat(result.data()).isEmpty();
        }

        @Test
        @DisplayName("상태 필터 적용 - IN_PROGRESS 상태만 조회")
        void getMyBids_withStatusFilter() {
            // given
            String memberPublicId = "bidder_pub";
            Long memberId = 50L;
            AuctionMember member = createMember(memberId, memberPublicId);

            Bid bid = createBid(1L, auctionId, memberId, 12000);
            Page<Bid> bidPage = new PageImpl<>(List.of(bid), pageable, 1);

            given(support.getPublicMember(memberPublicId)).willReturn(member);
            given(bidRepository.findAllByBidderIdAndAuctionStatus(eq(memberId), eq(AuctionStatus.IN_PROGRESS), eq(pageable))).willReturn(bidPage);
            given(auctionRepository.findAllById(Set.of(auctionId))).willReturn(List.of(auction));

            // when
            PagedResponseDto<MyBidResponseDto> result = auctionReadUseCase.getMyBids(memberPublicId, AuctionStatus.IN_PROGRESS, pageable);

            // then
            assertThat(result.data()).hasSize(1);
            verify(bidRepository).findAllByBidderIdAndAuctionStatus(memberId, AuctionStatus.IN_PROGRESS, pageable);
        }
    }

    // ============================
    // getMySales 테스트
    // ============================
    @Nested
    @DisplayName("getMySales - 나의 판매 내역 조회")
    class GetMySalesTest {

        @Test
        @DisplayName("성공 - 판매 내역을 상품/주문/입찰수와 함께 조립하여 반환")
        void getMySales_success() {
            // given
            String memberPublicId = "seller_pub";
            AuctionMember member = createMember(sellerId, memberPublicId);

            ProductAuctionResponseDto productDto = createProductDto(productId, "Lego StarWars");

            AuctionOrder order = createOrder(100L, auctionId, sellerId, 50L, 50000);

            given(support.getPublicMember(memberPublicId)).willReturn(member);
            given(productSearchClient.getProductIdsBySellerId(sellerId)).willReturn(List.of(productId));
            given(auctionRepository.findAllByProductIdIn(eq(List.of(productId)), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(auction), pageable, 1));
            given(productSearchClient.getProducts(anyCollection())).willReturn(Map.of(productId, productDto));
            given(auctionOrderRepository.findAllByAuctionIdIn(anyCollection())).willReturn(List.of(order));
            given(bidRepository.countByAuctionIdIn(anyCollection()))
                    .willReturn(List.<Object[]>of(new Object[]{auctionId, 5L}));

            // when
            PagedResponseDto<MySaleResponseDto> result =
                    auctionReadUseCase.getMySales(memberPublicId, null, pageable);

            // then
            assertThat(result.data()).hasSize(1);
            MySaleResponseDto dto = result.data().get(0);
            assertThat(dto.auctionId()).isEqualTo(auctionId);
            assertThat(dto.title()).isEqualTo("Lego StarWars");
            assertThat(dto.thumbnailUrl()).isEqualTo("thumb.jpg");
            assertThat(dto.bidCount()).isEqualTo(5);
            assertThat(dto.auctionStatus()).isEqualTo(AuctionStatus.IN_PROGRESS);
            assertThat(dto.currentPrice()).isEqualTo(15000);
        }

        @Test
        @DisplayName("filter가 null이면 전체 조회 (NPE 발생하지 않음)")
        void getMySales_filterNull() {
            // given
            String memberPublicId = "seller_pub";
            AuctionMember member = createMember(sellerId, memberPublicId);

            given(support.getPublicMember(memberPublicId)).willReturn(member);
            given(productSearchClient.getProductIdsBySellerId(sellerId)).willReturn(List.of(productId));
            given(auctionRepository.findAllByProductIdIn(eq(List.of(productId)), any(Pageable.class)))
                    .willReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

            // when
            PagedResponseDto<MySaleResponseDto> result =
                    auctionReadUseCase.getMySales(memberPublicId, null, pageable);

            // then
            assertThat(result.data()).isEmpty();
            // switch가 아닌 null 분기를 타서 findAllByProductIdIn 호출
            verify(auctionRepository).findAllByProductIdIn(eq(List.of(productId)), any(Pageable.class));
            verify(auctionRepository, never()).findAllByProductIdInAndStatusIn(any(), any(), any());
        }

        @Test
        @DisplayName("filter가 ONGOING이면 SCHEDULED, IN_PROGRESS 상태만 조회")
        void getMySales_filterOngoing() {
            // given
            String memberPublicId = "seller_pub";
            AuctionMember member = createMember(sellerId, memberPublicId);

            given(support.getPublicMember(memberPublicId)).willReturn(member);
            given(productSearchClient.getProductIdsBySellerId(sellerId)).willReturn(List.of(productId));
            given(auctionRepository.findAllByProductIdInAndStatusIn(
                    eq(List.of(productId)),
                    eq(List.of(AuctionStatus.SCHEDULED, AuctionStatus.IN_PROGRESS)),
                    any(Pageable.class)))
                    .willReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

            // when
            PagedResponseDto<MySaleResponseDto> result =
                    auctionReadUseCase.getMySales(memberPublicId, AuctionFilterType.ONGOING, pageable);

            // then
            assertThat(result.data()).isEmpty();
            verify(auctionRepository).findAllByProductIdInAndStatusIn(
                    eq(List.of(productId)),
                    eq(List.of(AuctionStatus.SCHEDULED, AuctionStatus.IN_PROGRESS)),
                    any(Pageable.class));
        }

        @Test
        @DisplayName("빈 결과 - 판매 상품이 없을 때 빈 리스트 반환")
        void getMySales_empty() {
            // given
            String memberPublicId = "seller_pub";
            AuctionMember member = createMember(sellerId, memberPublicId);

            given(support.getPublicMember(memberPublicId)).willReturn(member);
            given(productSearchClient.getProductIdsBySellerId(sellerId)).willReturn(List.of(productId));
            given(auctionRepository.findAllByProductIdIn(eq(List.of(productId)), any(Pageable.class)))
                    .willReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

            // when
            PagedResponseDto<MySaleResponseDto> result =
                    auctionReadUseCase.getMySales(memberPublicId, null, pageable);

            // then
            assertThat(result.data()).isEmpty();
            verify(productSearchClient, never()).getProducts(anyCollection());
        }

        @Test
        @DisplayName("actionRequired - 경매 종료 + 입찰 0건이면 true")
        void getMySales_actionRequired_noBids() {
            // given
            String memberPublicId = "seller_pub";
            AuctionMember member = createMember(sellerId, memberPublicId);

            Auction endedAuction = Auction.builder()
                    .productId(productId).sellerId(sellerId).startPrice(10000)
                    .durationDays(3)
                    .startTime(LocalDateTime.now().minusDays(4))
                    .endTime(LocalDateTime.now().minusDays(1))
                    .build();
            ReflectionTestUtils.setField(endedAuction, "id", auctionId);
            ReflectionTestUtils.setField(endedAuction, "status", AuctionStatus.ENDED);
            ReflectionTestUtils.setField(endedAuction, "currentPrice", 10000);

            ProductAuctionResponseDto productDto = createProductDto(productId, "유찰 상품");

            given(support.getPublicMember(memberPublicId)).willReturn(member);
            given(productSearchClient.getProductIdsBySellerId(sellerId)).willReturn(List.of(productId));
            given(auctionRepository.findAllByProductIdIn(eq(List.of(productId)), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(endedAuction), pageable, 1));
            given(productSearchClient.getProducts(anyCollection())).willReturn(Map.of(productId, productDto));
            given(auctionOrderRepository.findAllByAuctionIdIn(anyCollection())).willReturn(Collections.emptyList());
            // bidCount = 0
            given(bidRepository.countByAuctionIdIn(anyCollection())).willReturn(Collections.emptyList());

            // when
            PagedResponseDto<MySaleResponseDto> result =
                    auctionReadUseCase.getMySales(memberPublicId, null, pageable);

            // then
            assertThat(result.data()).hasSize(1);
            assertThat(result.data().get(0).actionRequired()).isTrue();
            assertThat(result.data().get(0).bidCount()).isZero();
        }
    }

    // ============================
    // getAuctionDetail 테스트
    // ============================
    @Nested
    @DisplayName("getAuctionDetail - 경매 상세 조회")
    class GetAuctionDetailTest {

        @Test
        @DisplayName("성공 - 로그인 회원, 입찰 기록 있음")
        void getAuctionDetail_member_withBid() {
            // given
            String memberPublicId = "member_pub_id";
            Long memberId = 50L;
            AuctionMember member = createMember(memberId, memberPublicId);

            ProductAuctionResponseDto productDto = ProductAuctionResponseDto.builder()
                    .id(productId)
                    .name("Lego StarWars")
                    .description("테스트 상품 설명")
                    .thumbnailUrl("lego.jpg")
                    .imageUrls(List.of("img1.jpg", "img2.jpg"))
                    .build();

            Bid highestBid = createBid(10L, auctionId, 99L, 15000);
            Bid myLastBid = createBid(11L, auctionId, memberId, 12000);

            given(support.getPublicMember(memberPublicId)).willReturn(member);
            given(support.findAuctionById(auctionId)).willReturn(auction);
            given(productSearchClient.getProduct(productId)).willReturn(Optional.of(productDto));
            given(bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auctionId)).willReturn(Optional.of(highestBid));
            given(bidRepository.findTopByAuctionIdAndBidderIdOrderByBidAmountDesc(auctionId, memberId)).willReturn(Optional.of(myLastBid));

            // when
            AuctionDetailResponseDto result = auctionReadUseCase.getAuctionDetail(auctionId, memberPublicId);

            // then
            // 기본 정보
            assertThat(result.auctionId()).isEqualTo(auctionId);
            assertThat(result.productId()).isEqualTo(productId);
            assertThat(result.productName()).isEqualTo("Lego StarWars");
            assertThat(result.productDescription()).isEqualTo("테스트 상품 설명");
            assertThat(result.imageUrls()).hasSize(2);
            assertThat(result.status()).isEqualTo(AuctionStatus.IN_PROGRESS);
            assertThat(result.remainingSeconds()).isGreaterThan(0);

            // 가격 정보
            assertThat(result.price().startPrice()).isEqualTo(10000);
            assertThat(result.price().currentPrice()).isEqualTo(15000);

            // 입찰 정보 - 최고 입찰자는 99L이므로 내가 아님
            assertThat(result.bid().highestBidderId()).isEqualTo(99L);
            assertThat(result.bid().isMyHighestBid()).isFalse();
            assertThat(result.bid().isSeller()).isFalse();
            assertThat(result.bid().canBid()).isTrue();
            // minBidPrice = currentPrice + tickSize (15000 + 1000 = 16000)
            assertThat(result.bid().minBidPrice()).isEqualTo(15000 + auction.getTickSize());

            // 내 참여 정보
            assertThat(result.myParticipation().hasBid()).isTrue();
            assertThat(result.myParticipation().myLastBidPrice()).isEqualTo(12000);
        }

        @Test
        @DisplayName("비로그인 사용자(guest) - myParticipation 없고 canBid false")
        void getAuctionDetail_guest() {
            // given
            ProductAuctionResponseDto productDto = createProductDto(productId, "Guest 상품");

            Bid highestBid = createBid(10L, auctionId, 99L, 15000);

            given(support.findAuctionById(auctionId)).willReturn(auction);
            given(productSearchClient.getProduct(productId)).willReturn(Optional.of(productDto));
            given(bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auctionId)).willReturn(Optional.of(highestBid));

            // when - memberPublicId가 null
            AuctionDetailResponseDto result = auctionReadUseCase.getAuctionDetail(auctionId, null);

            // then
            assertThat(result.bid().canBid()).isFalse();
            assertThat(result.bid().isMyHighestBid()).isFalse();
            assertThat(result.bid().isSeller()).isFalse();
            assertThat(result.myParticipation().hasBid()).isFalse();
            assertThat(result.myParticipation().myLastBidPrice()).isNull();
        }

        @Test
        @DisplayName("판매자 본인 조회 - isSeller true, canBid false")
        void getAuctionDetail_seller() {
            // given
            String sellerPublicId = "seller_pub";
            AuctionMember sellerMember = createMember(sellerId, sellerPublicId);

            ProductAuctionResponseDto productDto = createProductDto(productId, "내 상품");

            given(support.getPublicMember(sellerPublicId)).willReturn(sellerMember);
            given(support.findAuctionById(auctionId)).willReturn(auction);
            given(productSearchClient.getProduct(productId)).willReturn(Optional.of(productDto));
            given(bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auctionId)).willReturn(Optional.empty());
            given(bidRepository.findTopByAuctionIdAndBidderIdOrderByBidAmountDesc(auctionId, sellerId)).willReturn(Optional.empty());

            // when
            AuctionDetailResponseDto result = auctionReadUseCase.getAuctionDetail(auctionId, sellerPublicId);

            // then
            assertThat(result.bid().isSeller()).isTrue();
            assertThat(result.bid().canBid()).isFalse();
        }

        @Test
        @DisplayName("상품 조회 실패 시 CustomException 발생")
        void getAuctionDetail_productNotFound() {
            // given
            String memberPublicId = "member_pub";
            AuctionMember member = createMember(50L, memberPublicId);

            given(support.getPublicMember(memberPublicId)).willReturn(member);
            given(support.findAuctionById(auctionId)).willReturn(auction);
            given(productSearchClient.getProduct(productId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> auctionReadUseCase.getAuctionDetail(auctionId, memberPublicId))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("최고 입찰자 본인 조회 - isMyHighestBid true, canBid false")
        void getAuctionDetail_highestBidder() {
            // given
            String memberPublicId = "top_bidder_pub";
            Long memberId = 60L;
            AuctionMember member = createMember(memberId, memberPublicId);

            ProductAuctionResponseDto productDto = createProductDto(productId, "최고가 상품");

            Bid highestBid = createBid(10L, auctionId, memberId, 15000);
            Bid myLastBid = createBid(10L, auctionId, memberId, 15000);

            given(support.getPublicMember(memberPublicId)).willReturn(member);
            given(support.findAuctionById(auctionId)).willReturn(auction);
            given(productSearchClient.getProduct(productId)).willReturn(Optional.of(productDto));
            given(bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auctionId)).willReturn(Optional.of(highestBid));
            given(bidRepository.findTopByAuctionIdAndBidderIdOrderByBidAmountDesc(auctionId, memberId)).willReturn(Optional.of(myLastBid));

            // when
            AuctionDetailResponseDto result = auctionReadUseCase.getAuctionDetail(auctionId, memberPublicId);

            // then
            assertThat(result.bid().isMyHighestBid()).isTrue();
            assertThat(result.bid().canBid()).isFalse();
        }
    }

    // ============================
    // getAuctionOrder 테스트
    // ============================
    @Nested
    @DisplayName("getAuctionOrder - 낙찰 기록 상세 조회")
    class GetAuctionOrderTest {

        @Test
        @DisplayName("성공 - 구매자(BUYER) 시점 조회")
        void getAuctionOrder_buyer() {
            // given
            String buyerPublicId = "buyer_pub";
            Long buyerId = 50L;
            AuctionMember buyer = createMember(buyerId, buyerPublicId);

            AuctionOrder order = createOrder(100L, auctionId, sellerId, buyerId, 50000);

            AuctionMember seller = createMember(sellerId, "seller_pub");

            ProductAuctionResponseDto productDto = createProductDto(productId, "낙찰 상품");

            given(support.getPublicMember(buyerPublicId)).willReturn(buyer);
            given(support.getOrder(auctionId)).willReturn(order);
            given(support.findAuctionById(auctionId)).willReturn(auction);
            given(productSearchClient.getProduct(productId)).willReturn(Optional.of(productDto));
            given(support.getMember(sellerId)).willReturn(seller);

            // when
            AuctionOrderResponseDto result = auctionReadUseCase.getAuctionOrder(auctionId, buyerPublicId);

            // then
            assertThat(result.orderId()).isEqualTo(100L);
            assertThat(result.auctionId()).isEqualTo(auctionId);
            assertThat(result.viewerRole()).isEqualTo("BUYER");
            assertThat(result.orderStatus()).isEqualTo(AuctionOrderStatus.PROCESSING);
            assertThat(result.statusDescription()).isEqualTo("결제 대기중");

            // 상품 정보
            assertThat(result.productInfo().title()).isEqualTo("낙찰 상품");
            assertThat(result.productInfo().thumbnailUrl()).isEqualTo("thumb.jpg");

            // 결제 정보 (finalPrice=50000, deposit=5000, payment=45000)
            assertThat(result.paymentInfo().finalPrice()).isEqualTo(50000);
            assertThat(result.paymentInfo().depositUsed()).isEqualTo(5000);
            assertThat(result.paymentInfo().paymentAmount()).isEqualTo(45000);

            // 거래 상대 정보 (판매자)
            assertThat(result.trader().nickname()).isEqualTo("seller_pub");
        }

        @Test
        @DisplayName("성공 - 판매자(SELLER) 시점 조회")
        void getAuctionOrder_seller() {
            // given
            String sellerPublicId = "seller_pub";
            Long buyerId = 50L;
            AuctionMember sellerMember = createMember(sellerId, sellerPublicId);

            AuctionOrder order = createOrder(100L, auctionId, sellerId, buyerId, 50000);

            AuctionMember buyerMember = createMember(buyerId, "buyer_pub");

            ProductAuctionResponseDto productDto = createProductDto(productId, "판매 상품");

            given(support.getPublicMember(sellerPublicId)).willReturn(sellerMember);
            given(support.getOrder(auctionId)).willReturn(order);
            given(support.findAuctionById(auctionId)).willReturn(auction);
            given(productSearchClient.getProduct(productId)).willReturn(Optional.of(productDto));
            given(support.getMember(buyerId)).willReturn(buyerMember);

            // when
            AuctionOrderResponseDto result = auctionReadUseCase.getAuctionOrder(auctionId, sellerPublicId);

            // then
            assertThat(result.viewerRole()).isEqualTo("SELLER");
            assertThat(result.statusDescription()).isEqualTo("입금 대기중");
            // 거래 상대는 구매자
            assertThat(result.trader().nickname()).isEqualTo("buyer_pub");
        }

        @Test
        @DisplayName("권한 없음 - GUEST 접근 시 CustomException 발생")
        void getAuctionOrder_accessDenied() {
            // given
            String guestPublicId = "guest_pub";
            Long guestId = 999L;
            AuctionMember guest = createMember(guestId, guestPublicId);

            Long buyerId = 50L;
            AuctionOrder order = createOrder(100L, auctionId, sellerId, buyerId, 50000);

            given(support.getPublicMember(guestPublicId)).willReturn(guest);
            given(support.getOrder(auctionId)).willReturn(order);
            given(support.findAuctionById(auctionId)).willReturn(auction);

            // when & then
            assertThatThrownBy(() -> auctionReadUseCase.getAuctionOrder(auctionId, guestPublicId))
                    .isInstanceOf(CustomException.class);
        }
    }

    // ============================
    // getAuctions 테스트
    // ============================
    @Nested
    @DisplayName("getAuctions - 경매 목록 조회")
    class GetAuctionsTest {

        @Test
        @DisplayName("성공 - 조건 없이 전체 조회")
        void getAuctions_success() {
            // given
            AuctionSearchCondition condition = new AuctionSearchCondition();
            Page<Auction> auctionPage = new PageImpl<>(List.of(auction), pageable, 1);

            given(productSearchClient.getApprovedProductIds()).willReturn(List.of(productId));
            given(auctionRepository.findAllBySearchConditions(any(), any(), any(), any(), any())).willReturn(auctionPage);

            ProductAuctionResponseDto pDto = createProductDto(productId, "Test Product");
            given(productSearchClient.getProducts(anyCollection())).willReturn(Map.of(productId, pDto));
            given(bidRepository.countByAuctionIdIn(anyCollection()))
                    .willReturn(List.<Object[]>of(new Object[]{auctionId, 3L}));

            // when
            PagedResponseDto<AuctionListResponseDto> result = auctionReadUseCase.getAuctions(condition, pageable);

            // then
            assertThat(result.data()).hasSize(1);
            AuctionListResponseDto dto = result.data().get(0);
            assertThat(dto.auctionId()).isEqualTo(auctionId);
            assertThat(dto.productId()).isEqualTo(productId);
            assertThat(dto.productName()).isEqualTo("Test Product");
            assertThat(dto.currentPrice()).isEqualTo(15000);
            assertThat(dto.startPrice()).isEqualTo(10000);
            assertThat(dto.bidsCount()).isEqualTo(3);
            assertThat(dto.auctionStatus()).isEqualTo(AuctionStatus.IN_PROGRESS);

            verify(productSearchClient).getApprovedProductIds();
        }

    @Test
    @DisplayName("경매 목록 조회 - 검색 조건(키워드+상태)이 있을 때 상품 검색 후 경매 조회 수행")
    void getAuctions_with_search_condition() {
        // given
        AuctionSearchCondition condition = new AuctionSearchCondition();
        condition.setKeyword("키워드");
        condition.setCategory(Category.STARWARS);
        Pageable pageable = PageRequest.of(0, 10);

        // 1. 키워드로 상품 ID 검색
        given(productSearchClient.searchProductIds("키워드", Category.STARWARS)).willReturn(List.of(productId));

        // 2. 검수 승인된 상품 ID 목록 조회
        given(productSearchClient.getApprovedProductIds()).willReturn(List.of(productId));

            Page<Auction> auctionPage = new PageImpl<>(List.of(auction), pageable, 1);
            given(auctionRepository.findAllBySearchConditions(any(), any(), eq(List.of(productId)), eq(List.of(productId)), any())).willReturn(auctionPage);

            ProductAuctionResponseDto pDto = createProductDto(productId, "검색 상품");
            given(productSearchClient.getProducts(anyCollection())).willReturn(Map.of(productId, pDto));
            given(bidRepository.countByAuctionIdIn(anyCollection())).willReturn(Collections.emptyList());

            // when
            PagedResponseDto<AuctionListResponseDto> result = auctionReadUseCase.getAuctions(condition, pageable);

        // then
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).productName()).isEqualTo("검색 상품");
        verify(productSearchClient).searchProductIds("키워드", Category.STARWARS);
        verify(productSearchClient).getApprovedProductIds();
    }

        @Test
        @DisplayName("검색 결과 없음 - 상품 검색이 빈 리스트이면 DB 조회 없이 빈 결과 반환")
        void getAuctions_searchResultEmpty() {
            // given
            AuctionSearchCondition condition = new AuctionSearchCondition();
            condition.setKeyword("없는상품");

            given(productSearchClient.searchProductIds("없는상품", null)).willReturn(Collections.emptyList());

            // when
            PagedResponseDto<AuctionListResponseDto> result = auctionReadUseCase.getAuctions(condition, pageable);

            // then
            assertThat(result.data()).isEmpty();
            verify(auctionRepository, never()).findAllBySearchConditions(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("경매 목록 결과가 비어있으면 빈 리스트 반환")
        void getAuctions_auctionEmpty() {
            // given
            AuctionSearchCondition condition = new AuctionSearchCondition();

            given(productSearchClient.getApprovedProductIds()).willReturn(List.of(productId));
            given(auctionRepository.findAllBySearchConditions(any(), any(), any(), any(), any()))
                    .willReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

            // when
            PagedResponseDto<AuctionListResponseDto> result = auctionReadUseCase.getAuctions(condition, pageable);

            // then
            assertThat(result.data()).isEmpty();
            verify(productSearchClient, never()).getProducts(anyCollection());
        }
    }

    // ============================
    // getMyAuctionOrders 테스트
    // ============================
    @Nested
    @DisplayName("getMyAuctionOrders - 나의 낙찰 목록 조회")
    class GetMyAuctionOrdersTest {

        @Test
        @DisplayName("성공 - 데이터 조립 및 필드 매핑 확인")
        void getMyAuctionOrders_success() {
            // given
            String memberPublicId = "bidder_id";
            Long memberId = 20L;
            AuctionMember member = createMember(memberId, memberPublicId);

            AuctionOrder order = createOrder(100L, auctionId, sellerId, memberId, 50000);

            ProductAuctionResponseDto productDto = createProductDto(productId, "Lego Titanic");

            Page<AuctionOrder> orderPage = new PageImpl<>(List.of(order), pageable, 1);

            given(support.getPublicMember(memberPublicId)).willReturn(member);
            given(auctionOrderRepository.findAllByBidderIdAndStatus(eq(memberId), isNull(), any())).willReturn(orderPage);
            given(auctionRepository.findAllById(any())).willReturn(List.of(auction));
            given(productSearchClient.getProducts(anyCollection())).willReturn(Map.of(productId, productDto));

            // when
            PagedResponseDto<MyAuctionOrderListResponseDto> result =
                    auctionReadUseCase.getMyAuctionOrders(memberPublicId, null, pageable);

            // then
            assertThat(result.data()).hasSize(1);
            MyAuctionOrderListResponseDto dto = result.data().get(0);
            assertThat(dto.orderId()).isEqualTo(100L);
            assertThat(dto.auctionId()).isEqualTo(auctionId);
            assertThat(dto.productName()).isEqualTo("Lego Titanic");
            assertThat(dto.thumbnailUrl()).isEqualTo("thumb.jpg");
            assertThat(dto.finalPrice()).isEqualTo(50000);
            assertThat(dto.orderStatus()).isEqualTo(AuctionOrderStatus.PROCESSING);
            assertThat(dto.statusDescription()).isEqualTo("결제 대기중");
            assertThat(dto.auctionRequired()).isTrue(); // PROCESSING이면 true
            assertThat(dto.tradeDate()).isNotNull();
        }

        @Test
        @DisplayName("빈 결과 - 내역이 없을 때 빈 리스트 반환 (Early Return)")
        void getMyAuctionOrders_empty() {
            // given
            String memberPublicId = "user_pub_id";
            AuctionMember member = createMember(10L, memberPublicId);

            Page<AuctionOrder> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            given(support.getPublicMember(memberPublicId)).willReturn(member);
            given(auctionOrderRepository.findAllByBidderIdAndStatus(eq(10L), isNull(), any(Pageable.class)))
                    .willReturn(emptyPage);

            // when
            PagedResponseDto<MyAuctionOrderListResponseDto> result =
                    auctionReadUseCase.getMyAuctionOrders(memberPublicId, null, pageable);

            // then
            assertThat(result.data()).isEmpty();
            verify(productSearchClient, never()).getProducts(anyCollection());
        }

        @Test
        @DisplayName("상품이 ES에 없을 때 삭제된 상품으로 표시")
        void getMyAuctionOrders_productDeleted() {
            // given
            String memberPublicId = "bidder_pub";
            Long memberId = 20L;
            AuctionMember member = createMember(memberId, memberPublicId);

            AuctionOrder order = createOrder(100L, auctionId, sellerId, memberId, 30000);

            Page<AuctionOrder> orderPage = new PageImpl<>(List.of(order), pageable, 1);

            given(support.getPublicMember(memberPublicId)).willReturn(member);
            given(auctionOrderRepository.findAllByBidderIdAndStatus(eq(memberId), isNull(), any())).willReturn(orderPage);
            given(auctionRepository.findAllById(any())).willReturn(List.of(auction));
            // 상품 정보가 ES에 없음
            given(productSearchClient.getProducts(anyCollection())).willReturn(Collections.emptyMap());

            // when
            PagedResponseDto<MyAuctionOrderListResponseDto> result =
                    auctionReadUseCase.getMyAuctionOrders(memberPublicId, null, pageable);

            // then
            assertThat(result.data()).hasSize(1);
            assertThat(result.data().get(0).productName()).isEqualTo("삭제된 상품");
        }
    }

    // ============================
    // getMyBookmarks 테스트
    // ============================
    @Nested
    @DisplayName("getMyBookmarks - 내 관심 경매 목록 조회")
    class GetMyBookmarksTest {

        @Test
        @DisplayName("성공 - 북마크된 경매 정보와 상품 정보를 조립하여 반환")
        void getMyBookmarks_success() {
            // given
            String publicId = "user_1";
            AuctionMember member = createMember(1L, publicId);

            AuctionBookmark bookmark = AuctionBookmark.builder().memberId(1L).auctionId(auctionId).build();
            ReflectionTestUtils.setField(bookmark, "id", 1L);
            Page<AuctionBookmark> bookmarkPage = new PageImpl<>(List.of(bookmark), pageable, 1);

            ProductAuctionResponseDto productDto = createProductDto(productId, "관심 상품");

            given(support.getPublicMember(publicId)).willReturn(member);
            given(auctionBookmarkRepository.findAllByMemberId(eq(1L), any())).willReturn(bookmarkPage);
            given(auctionRepository.findAllById(any())).willReturn(List.of(auction));
            given(productSearchClient.getProducts(anyCollection())).willReturn(Map.of(productId, productDto));
            given(bidRepository.countByAuctionIdIn(anyCollection()))
                    .willReturn(List.<Object[]>of(new Object[]{auctionId, 7L}));

            // when
            PagedResponseDto<AuctionBookmarkListResponseDto> result = auctionReadUseCase.getMyBookmarks(publicId, pageable);

            // then
            assertThat(result.data()).hasSize(1);
            AuctionBookmarkListResponseDto bookmarkDto = result.data().get(0);
            assertThat(bookmarkDto.bookmarkId()).isEqualTo(1L);

            AuctionListResponseDto auctionInfo = bookmarkDto.auctionInfo();
            assertThat(auctionInfo.auctionId()).isEqualTo(auctionId);
            assertThat(auctionInfo.productName()).isEqualTo("관심 상품");
            assertThat(auctionInfo.thumbnailUrl()).isEqualTo("thumb.jpg");
            assertThat(auctionInfo.currentPrice()).isEqualTo(15000);
            assertThat(auctionInfo.bidsCount()).isEqualTo(7);
            assertThat(auctionInfo.auctionStatus()).isEqualTo(AuctionStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("빈 결과 - 북마크가 없을 때 빈 리스트 반환")
        void getMyBookmarks_empty() {
            // given
            String publicId = "user_1";
            AuctionMember member = createMember(1L, publicId);

            Page<AuctionBookmark> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            given(support.getPublicMember(publicId)).willReturn(member);
            given(auctionBookmarkRepository.findAllByMemberId(eq(1L), any())).willReturn(emptyPage);

            // when
            PagedResponseDto<AuctionBookmarkListResponseDto> result = auctionReadUseCase.getMyBookmarks(publicId, pageable);

            // then
            assertThat(result.data()).isEmpty();
            verify(auctionRepository, never()).findAllById(any());
        }
    }
}
