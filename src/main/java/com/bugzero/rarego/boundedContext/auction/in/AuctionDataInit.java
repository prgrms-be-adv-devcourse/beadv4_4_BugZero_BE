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
import java.util.UUID;

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
        if (auctionRepository.count() > 0) {
            log.info("경매 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("경매 전체 테스트 데이터 초기화 시작...");

        // 0. 회원 생성 (MemberRole 제거)
        AuctionMember seller = createOrGetMember(1L, "seller@test.com", "판매자_제로");
        AuctionMember me = createOrGetMember(2L, "me@test.com", "입찰자_나");
        AuctionMember competitor = createOrGetMember(3L, "comp@test.com", "경쟁자_A");

        // ==========================================
        // [Part 1] API 조회 테스트용
        // ==========================================
        log.info("--- [Part 1] API 연동 테스트 데이터 ---");

        Product product1 = createProduct(seller.getId(), "[1-1] 레고 밀레니엄 팔콘", 10_000);
        Auction auction1 = createAuctionWithTime(product1.getId(), -60, 1440, 10_000, 1_000);

        createBid(auction1, competitor, 15_000);
        createBid(auction1, me, 20_000);
        createBid(auction1, competitor, 25_000);

        Product product2 = createProduct(seller.getId(), "[1-2] 아이폰 15 Pro (낙찰)", 500_000);
        Auction auction2 = createAuctionWithTime(product2.getId(), -120, -60, 500_000, 10_000);

        createBid(auction2, competitor, 550_000);
        createBid(auction2, me, 600_000);

        Product product3 = createProduct(seller.getId(), "[1-3] 맥북 프로 M3 (패찰)", 2_000_000);
        Auction auction3 = createAuctionWithTime(product3.getId(), -180, -120, 2_000_000, 50_000);

        createBid(auction3, me, 400_000);
        createBid(auction3, competitor, 2_200_000);

        // ==========================================
        // [Part 2] 정산 로직 검증용
        // ==========================================
        log.info("--- [Part 2] 정산 로직 검증 데이터 ---");

        Product product4 = createProduct(seller.getId(), "[2-1] 정산테스트(낙찰)", 10_000);
        Auction auction4 = createAuctionWithTime(product4.getId(), -120, -1, 10_000, 1_000);
        createBid(auction4, me, 15_000);
        createBid(auction4, competitor, 20_000);
        createBid(auction4, me, 25_000);

        Product product5 = createProduct(seller.getId(), "[2-2] 정산테스트(유찰)", 20_000);
        Auction auction5 = createAuctionWithTime(product5.getId(), -120, -1, 20_000, 2_000);

        Product product6 = createProduct(seller.getId(), "[2-3] 정산테스트(단독낙찰)", 30_000);
        Auction auction6 = createAuctionWithTime(product6.getId(), -180, -1, 30_000, 3_000);
        createBid(auction6, me, 35_000);

        Product product7 = createProduct(seller.getId(), "[2-4] 동적스케줄(1분)", 40_000);
        Auction auction7 = createAuctionWithTime(product7.getId(), -60, 1, 40_000, 4_000);
        createBid(auction7, me, 45_000);
        eventPublisher.publishEvent(new AuctionCreatedEvent(auction7.getId(), auction7.getEndTime()));

        Product product8 = createProduct(seller.getId(), "[2-5] 동적스케줄(5분)", 50_000);
        Auction auction8 = createAuctionWithTime(product8.getId(), -60, 5, 50_000, 5_000);
        createBid(auction8, me, 55_000);
        eventPublisher.publishEvent(new AuctionCreatedEvent(auction8.getId(), auction8.getEndTime()));
    }

    // --- Helper Methods ---

    private AuctionMember createOrGetMember(Long id, String email, String nickname) {
        return auctionMemberRepository.findById(id)
                .orElseGet(() -> auctionMemberRepository.save(
                        AuctionMember.builder()
                                .id(id)
                                .publicId(UUID.randomUUID().toString())
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

    private Auction createAuctionWithTime(Long productId, int startOffsetMinutes, int endOffsetMinutes,
                                          int startPrice, int tickSize) {
        LocalDateTime now = LocalDateTime.now();

        Auction auction = Auction.builder()
                .productId(productId)
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
