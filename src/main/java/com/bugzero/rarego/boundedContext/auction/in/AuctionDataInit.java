package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionCreatedEvent;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.boundedContext.product.domain.Category;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductCondition;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("dev")
public class AuctionDataInit implements CommandLineRunner {

    private static final String SELLER_PUBLIC_ID = "eea43e1c-eb7d-406a-8e5d-5ffbbddd488f";
    private static final String BIDDER_PUBLIC_ID = "f8d99e06-fa28-4dad-9371-b7b8baa6ae6b";
    private static final String COMPETITOR_PUBLIC_ID = "competitor_public_3";

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ProductRepository productRepository;
    private final AuctionMemberRepository auctionMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void run(String... args) {
        if (auctionRepository.count() > 0) {
            log.info("경매 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("경매 전체 테스트 데이터 초기화 시작...");

        // 0. 회원 생성 (MemberRole 제거)
        AuctionMember seller = createOrGetMember(1L, SELLER_PUBLIC_ID, "seller@auction.com", "AuctionSeller");
        AuctionMember me = createOrGetMember(2L, BIDDER_PUBLIC_ID, "buyer@auction.com", "AuctionBuyer");
        AuctionMember competitor = createOrGetMember(3L, COMPETITOR_PUBLIC_ID, "comp@test.com", "경쟁자_A");

        // ==========================================
        // [Part 1] API 조회 테스트용
        // ==========================================
        log.info("--- [Part 1] API 연동 테스트 데이터 ---");

        Product product1 = createProduct(seller.getId(), "[1-1] 레고 밀레니엄 팔콘", 10_000);
        Auction auction1 = createAuctionWithTime(product1.getId(), -60, 1440, 10_000, 1_000);
      
        // ========== 2. 상품 + 정상 경매 생성 (다른 팀원 코드) ==========
        Product product1 = Product.builder()
                .sellerId(seller.getId())
                .name("스타워즈 제다이 레고")
                .description("미개봉 새상품입니다. 경매로 저렴하게 내놓습니다.")
                .category(Category.스타워즈)
                .productCondition(ProductCondition.MISB)
                .inspectionStatus(InspectionStatus.PENDING)
                .build();
        productRepository.save(product1);

        Auction normalAuction = Auction.builder()
                .productId(product1.getId())
                .sellerId(product1.getSellerId())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(3))
                .startPrice(1_000_000)
                .tickSize(50_000)
                .build();
        normalAuction.startAuction();
        auctionRepository.save(normalAuction);

        log.info("정상 경매 생성 완료 - AuctionId: {}, ProductId: {}", normalAuction.getId(), product1.getId());

        // ========== 3. 정산 테스트용 경매 생성 (내 코드) ==========

        // 3-1. 종료 + 입찰 있음 (낙찰 대상)
        Product product2 = createProduct(seller.getId(), "테스트 상품 1", 10_000);
        Auction auction1 = createAuction(product2.getId(), product2.getSellerId(), -2, -1, 10_000, 1_000);
        auction1.forceStartForTest();
        auctionRepository.save(auction1);
        bidRepository.save(createBid(auction1.getId(), buyer.getId(), 15_000));
        bidRepository.save(createBid(auction1.getId(), seller.getId(), 20_000));
        bidRepository.save(createBid(auction1.getId(), buyer.getId(), 25_000));

        // 3-2. 종료 + 입찰 없음 (유찰 대상)
        Product product3 = createProduct(seller.getId(), "테스트 상품 2", 20_000);
        Auction auction2 = createAuction(product3.getId(), product3.getSellerId(), -2, 0, 20_000, 2_000);
        auction2.forceStartForTest();
        auctionRepository.save(auction2);

        // 3-3. 종료 + 입찰 1건 (낙찰 대상)
        Product product4 = createProduct(seller.getId(), "테스트 상품 3", 30_000);
        Auction auction3 = createAuction(product4.getId(), product4.getSellerId(), -3, 0, 30_000, 3_000);
        auction3.forceStartForTest();
        auctionRepository.save(auction3);
        bidRepository.save(createBid(auction3.getId(), buyer.getId(), 35_000));

        // 3-4. 진행 중 + 1분 후 종료 (동적 스케줄링 테스트용)
        Product product5 = createProduct(seller.getId(), "테스트 상품 4", 40_000);
        Auction auction4 = createAuction(product5.getId(), product5.getSellerId(), -1, 1, 40_000, 4_000);
        auction4.forceStartForTest();
        auctionRepository.save(auction4);
        bidRepository.save(createBid(auction4.getId(), buyer.getId(), 45_000));

        eventPublisher.publishEvent(
                new AuctionCreatedEvent(
                        auction4.getId(),
                                auction4.getEndTime()));

        // 3-5. 진행 중 + 5분 후 종료 (동적 스케줄링 테스트용)
        Product product6 = createProduct(seller.getId(), "테스트 상품 5", 50_000);
        Auction auction5 = createAuction(product6.getId(), product6.getSellerId(), -1, 5, 50_000, 5_000);
        auction5.forceStartForTest();
        auctionRepository.save(auction5);
        bidRepository.save(createBid(auction5.getId(), buyer.getId(), 55_000));

        eventPublisher.publishEvent(
                new AuctionCreatedEvent(
                        auction5.getId(),
                                auction5.getEndTime()));

        log.info("=== 경매 테스트 데이터 초기화 완료 ===");
        log.info("- 정상 경매: 1건 (3일 후 종료)");
        log.info("- 낙찰 대상 (종료됨): 2건 (auction1, auction3)");
        log.info("- 유찰 대상 (종료됨): 1건 (auction2)");
        log.info("- 진행 중 (1분 후 자동 정산): 1건 (auction4)");
        log.info("- 진행 중 (5분 후 자동 정산): 1건 (auction5)");
    }

    // --- Helper Methods ---

    private AuctionMember createOrGetMember(Long id, String publicId, String email, String nickname) {
        return auctionMemberRepository.findById(id)
                .orElseGet(() -> auctionMemberRepository.save(
                        AuctionMember.builder()
                                .id(id)
                                .publicId(publicId)
                                .email(email)
                                .nickname(nickname)
                                .build()
                ));
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

    private Auction createAuction(
            Long productId,
            Long sellerId,
            int startHoursOffset,
            int endMinutesOffset,
            int startPrice,
            int tickSize
    ) {
        LocalDateTime now = LocalDateTime.now();

        Auction auction = Auction.builder()
                .productId(productId)
                .sellerId(sellerId)
                .startTime(now.plusHours(startHoursOffset))
                .endTime(endMinutesOffset > 0
                        ? now.plusMinutes(endMinutesOffset)
                        : now.plusHours(startHoursOffset).minusMinutes(-endMinutesOffset))
                .startPrice(startPrice)
                .tickSize(tickSize)
                .startTime(now.plusMinutes(startOffsetMinutes))
                .endTime(now.plusMinutes(endOffsetMinutes))
                .build();

        auction.forceStartForTest();
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
