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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import lombok.RequiredArgsConstructor;

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
            log.info("경매 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("경매 전체 테스트 데이터 초기화 시작...");

        // 0. 회원 생성
        AuctionMember seller = createOrGetMember(1L, "seller@test.com", "판매자_제로", MemberRole.SELLER);
        AuctionMember me = createOrGetMember(2L, "me@test.com", "입찰자_나", MemberRole.USER); // 로그인할 계정
        AuctionMember competitor = createOrGetMember(3L, "comp@test.com", "경쟁자_A", MemberRole.USER);

        // ==========================================
        // [Part 1] API 조회 테스트용 (GET /auctions, /me/bids) (내 코드)
        // ==========================================
        log.info("--- [Part 1] API 연동 테스트 데이터 ---");

        // 1-1. 진행 중 (나 vs 경쟁자) -> 24시간 후 종료
        Product product1 = createProduct(seller.getId(), "[1-1] 레고 밀레니엄 팔콘", 10_000);
        Auction auction1 = createAuctionWithTime(product1.getId(), -60, 1440, 10_000, 1_000);

        createBid(auction1, competitor, 15_000);
        createBid(auction1, me, 20_000); // 내가 입찰 중
        createBid(auction1, competitor, 25_000);

        // 1-2. 종료됨 (내가 낙찰 - WON) -> 1시간 전 종료
        Product product2 = createProduct(seller.getId(), "[1-2] 아이폰 15 Pro (낙찰)", 500_000);
        Auction auction2 = createAuctionWithTime(product2.getId(), -120, -60, 500_000, 10_000);

        createBid(auction2, competitor, 550_000);
        createBid(auction2, me, 600_000); // 최종가: 600,000 (내꺼)

        // 1-3. 종료됨 (내가 패찰 - LOST) -> 2시간 전 종료
        Product product3 = createProduct(seller.getId(), "[1-3] 맥북 프로 M3 (패찰)", 2_000_000);
        Auction auction3 = createAuctionWithTime(product3.getId(), -180, -120, 2_000_000, 50_000);

        createBid(auction3, me, 400_000);
        createBid(auction3, competitor, 2_200_000); // 최종가: 2,200,000 (경쟁자꺼)


        // ==========================================
        // [Part 2] 정산 로직 검증용 (2-1 ~ 2-5) (다른 팀원 코드)
        // ==========================================
        log.info("--- [Part 2] 정산 로직 검증 데이터 ---");

        // 2-1. 낙찰 대상 (종료, 입찰3건)
        Product product4 = createProduct(seller.getId(), "[2-1] 정산테스트(낙찰)", 10_000);
        Auction auction4 = createAuctionWithTime(product4.getId(), -120, -1, 10_000, 1_000);
        createBid(auction4, me, 15_000);
        createBid(auction4, competitor, 20_000);
        createBid(auction4, me, 25_000);

        // 2-2. 유찰 대상 (종료, 입찰0건)
        Product product5 = createProduct(seller.getId(), "[2-2] 정산테스트(유찰)", 20_000);
        Auction auction5 = createAuctionWithTime(product5.getId(), -120, -1, 20_000, 2_000);

        // 2-3. 낙찰 대상 (종료, 단독입찰)
        Product product6 = createProduct(seller.getId(), "[2-3] 정산테스트(단독낙찰)", 30_000);
        Auction auction6 = createAuctionWithTime(product6.getId(), -180, -1, 30_000, 3_000);
        createBid(auction6, me, 35_000);

        // 2-4. 진행 중 (1분 후 종료)
        Product product7 = createProduct(seller.getId(), "[2-4] 동적스케줄(1분)", 40_000);
        Auction auction7 = createAuctionWithTime(product7.getId(), -60, 1, 40_000, 4_000);
        createBid(auction7, me, 45_000);
        eventPublisher.publishEvent(new AuctionCreatedEvent(auction7.getId(), auction7.getEndTime()));

        // 2-5. 진행 중 (5분 후 종료)
        Product product8 = createProduct(seller.getId(), "[2-5] 동적스케줄(5분)", 50_000);
        Auction auction8 = createAuctionWithTime(product8.getId(), -60, 5, 50_000, 5_000);
        createBid(auction8, me, 55_000);
        eventPublisher.publishEvent(new AuctionCreatedEvent(auction8.getId(), auction8.getEndTime()));

        // ==========================================
        // 로그 출력 (ID 확인용)
        // ==========================================
        log.info(" [Part 1: API 테스트]");
        log.info("  1-1. 진행 중 (입찰중)   : ID={}", auction1.getId());
        log.info("  1-2. 종료됨 (내가 낙찰) : ID={}", auction2.getId());
        log.info("  1-3. 종료됨 (내가 패찰) : ID={}", auction3.getId());
        log.info("----------------------------------------------------------------");
        log.info(" [Part 2: 정산 로직]");
        log.info("  2-1. 종료됨 (낙찰,입찰3): ID={}", auction4.getId());
        log.info("  2-2. 종료됨 (유찰)      : ID={}", auction5.getId());
        log.info("  2-3. 종료됨 (단독낙찰)  : ID={}", auction6.getId());
        log.info("  2-4. 진행 중 (1분 남음) : ID={}", auction7.getId());
        log.info("  2-5. 진행 중 (5분 남음) : ID={}", auction8.getId());
        log.info("----------------------------------------------------------------");
    }

    // --- Helper Methods ---

    private AuctionMember createOrGetMember(Long id, String email, String nickname, MemberRole role) {
        return auctionMemberRepository.findById(id)
            .orElseGet(() -> auctionMemberRepository.save(AuctionMember.builder()
                .id(id)
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
            .description("테스트용 상품 설명")
            .category(Category.스타워즈)
            .productCondition(ProductCondition.MISB)
            .inspectionStatus(InspectionStatus.PENDING)
            .build());
    }

    // 통합된 경매 생성 헬퍼 (시간 오프셋 기준)
    private Auction createAuctionWithTime(Long productId, int startOffsetMinutes, int endOffsetMinutes, int startPrice, int tickSize) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.plusMinutes(startOffsetMinutes);
        LocalDateTime endTime = now.plusMinutes(endOffsetMinutes);

        // 시간이 과거면 ENDED, 아니면 IN_PROGRESS
        AuctionStatus status = (endTime.isBefore(now)) ? AuctionStatus.ENDED : AuctionStatus.IN_PROGRESS;

        Auction auction = Auction.builder()
            .productId(productId)
            .startPrice(startPrice)
            .tickSize(tickSize)
            .startTime(startTime)
            .endTime(endTime)
            .build();

        // 상태 강제 주입
        try {
            auction.forceStartForTest(); // 기본 시작 처리
            if (status == AuctionStatus.ENDED) {
                var statusField = Auction.class.getDeclaredField("status");
                statusField.setAccessible(true);
                statusField.set(auction, AuctionStatus.ENDED);

                // 가격 초기화 (유찰 대비)
                if (auction.getCurrentPrice() == null) {
                    auction.updateCurrentPrice(startPrice);
                }
            }
        } catch (Exception e) {
            log.error("상태 설정 에러", e);
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

        auction.updateCurrentPrice(amount);
        auctionRepository.save(auction);
    }
}