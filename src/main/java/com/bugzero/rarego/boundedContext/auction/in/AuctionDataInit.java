package com.bugzero.rarego.boundedContext.auction.in;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionCreatedEvent;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.boundedContext.product.domain.Category;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductCondition;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.shared.member.domain.MemberRole;
import com.bugzero.rarego.shared.member.domain.Provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("dev")
public class AuctionDataInit implements CommandLineRunner {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ProductRepository productRepository;
    private final AuctionMemberRepository auctionMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void run(String... args) {
        // 중복 초기화 방지
        if (auctionRepository.count() > 0) {
            log.info("ℹ️ 경매 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("🚀 경매 테스트 데이터 초기화 시작...");

        // 1. 회원 생성
        AuctionMember seller = createOrGetMember(1L, "seller@test.com", "판매자_제로", MemberRole.SELLER);
        AuctionMember me = createOrGetMember(2L, "me@test.com", "입찰자_나", MemberRole.USER); // 로그인 ID
        AuctionMember competitor = createOrGetMember(3L, "comp@test.com", "경쟁자_A", MemberRole.USER);

        // ==========================================
        // [Part 1] API 조회 테스트용 시나리오 (GET /auctions, GET /me/bids)
        // ==========================================

        // 1-1. 진행 중인 경매 (나 vs 경쟁자)
        Product product1 = createProduct(seller.getId(), "레고 밀레니엄 팔콘 (진행중)", 10_000);
        Auction auctionInProgress = createAuction(product1, 10_000, AuctionStatus.IN_PROGRESS, 1440); // 24시간 후 종료

        createBid(auctionInProgress, competitor, 15_000);
        createBid(auctionInProgress, me, 20_000);
        createBid(auctionInProgress, competitor, 25_000); // 현재가 25,000 (내가 지고 있음)

        // 1-2. 종료된 경매 (내가 낙찰 - WON)
        Product product2 = createProduct(seller.getId(), "아이폰 15 Pro (낙찰)", 500_000);
        Auction auctionWon = createAuction(product2, 500_000, AuctionStatus.ENDED, -60); // 1시간 전 종료

        createBid(auctionWon, competitor, 550_000);
        createBid(auctionWon, me, 600_000); // 최종 낙찰

        // 1-3. 종료된 경매 (내가 패찰 - LOST)
        Product product3 = createProduct(seller.getId(), "맥북 프로 M3 (패찰)", 2_000_000);
        Auction auctionLost = createAuction(product3, 2_000_000, AuctionStatus.ENDED, -120); // 2시간 전 종료

        createBid(auctionLost, me, 2_100_000);
        createBid(auctionLost, competitor, 2_200_000); // 최종 낙찰자는 경쟁자

        log.info("✅ API 테스트용 데이터 생성 완료");

        // ==========================================
        // [Part 2] 정산/스케줄러 테스트용 (기존 코드 반영)
        // ==========================================

        // 2-1. 유찰 대상 (입찰 없음, 종료됨)
        Product product4 = createProduct(seller.getId(), "유찰 테스트 상품", 50_000);
        createAuction(product4, 50_000, AuctionStatus.ENDED, -10);

        // 2-2. 마감 임박 (1분 후 종료 -> 자동 정산 테스트)
        Product product5 = createProduct(seller.getId(), "마감임박 상품(1분)", 1_000);
        Auction auctionEndingSoon = createAuction(product5, 1_000, AuctionStatus.IN_PROGRESS, 1);
        createBid(auctionEndingSoon, me, 5_000);

        // 이벤트 발행 (스케줄러가 있다면 감지)
        eventPublisher.publishEvent(new AuctionCreatedEvent(auctionEndingSoon.getId(), auctionEndingSoon.getEndTime()));

        log.info("✅ 정산 테스트용 데이터 생성 완료");
        log.info("=== 초기화 종료 (내 ID: 2L) ===");
    }

    // --- Helper Methods ---

    private AuctionMember createOrGetMember(Long id, String email, String nickname, MemberRole role) {
        return auctionMemberRepository.findById(id)
            .orElseGet(() -> auctionMemberRepository.save(AuctionMember.builder()
                .id(id) // 테스트용 ID 지정
                .publicId(UUID.randomUUID().toString())
                .email(email)
                .nickname(nickname)
                .role(role)
                .provider(Provider.GOOGLE)
                .providerId("provider_" + UUID.randomUUID())
                .build()));
    }

    private Product createProduct(Long sellerId, String name, int startPrice) {
        return productRepository.save(Product.builder()
            .sellerId(sellerId)
            .name(name)
            .description("테스트용 상품입니다.")
            .category(Category.스타워즈)
            .productCondition(ProductCondition.MISB)
            .inspectionStatus(InspectionStatus.PENDING)
            .build());
    }

    private Auction createAuction(Product product, int startPrice, AuctionStatus status, int endMinutesOffset) {
        LocalDateTime now = LocalDateTime.now();
        Auction auction = Auction.builder()
            .productId(product.getId())
            .startPrice(startPrice)
            .tickSize(1_000)
            .startTime(now.minusHours(1)) // 1시간 전 시작
            .endTime(now.plusMinutes(endMinutesOffset)) // 종료 시간 설정
            .build();

        // 상태 설정
        if (status == AuctionStatus.IN_PROGRESS) {
            auction.startAuction();
        } else if (status == AuctionStatus.ENDED) {
            // ENDED 상태 강제 주입 (Reflection)
            // 엔티티에 endAuction() 메서드가 있다면 그걸 쓰는 게 좋습니다.
            try {
                auction.startAuction(); // 일단 시작 후
                var field = Auction.class.getDeclaredField("status");
                field.setAccessible(true);
                field.set(auction, AuctionStatus.ENDED);

                // 가격 초기화 (입찰이 없는 경우를 대비)
                if (auction.getCurrentPrice() == null) {
                    auction.updateCurrentPrice(startPrice);
                }
            } catch (Exception e) {
                log.error("상태 변경 에러", e);
            }
        }

        return auctionRepository.save(auction);
    }

    private void createBid(Auction auction, AuctionMember bidder, int amount) {
        Bid bid = Bid.builder()
            .auctionId(auction.getId())
            .bidderId(bidder.getId())
            .bidAmount(amount)
            .build();

        bidRepository.save(bid);

        // 경매 현재가 업데이트
        auction.updateCurrentPrice(amount);
        auctionRepository.save(auction);
    }
}