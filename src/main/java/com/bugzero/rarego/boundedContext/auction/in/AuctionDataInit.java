package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.domain.event.AuctionCreatedEvent;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.boundedContext.product.domain.Category;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductCondition;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Configuration
@Slf4j
@Profile("dev")
public class AuctionDataInit {

    private final AuctionDataInit self; // 프록시 호출용 (트랜잭션 보장)
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ProductRepository productRepository;
    private final AuctionMemberRepository auctionMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AuctionDataInit(
            @Lazy AuctionDataInit self,
            AuctionRepository auctionRepository,
            BidRepository bidRepository,
            ProductRepository productRepository,
            AuctionMemberRepository auctionMemberRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.self = self;
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.productRepository = productRepository;
        this.auctionMemberRepository = auctionMemberRepository;
        this.eventPublisher = eventPublisher;
    }

    @Bean
    public ApplicationRunner auctionBaseInitDataRunner() {
        return args -> {
            self.makeBaseAuctionData();
        };
    }

    @Transactional
    public void makeBaseAuctionData() {
        if (auctionRepository.count() > 0) {
            log.info("이미 경매 데이터가 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("=== 경매 전체 통합 테스트 데이터 초기화 시작 ===");

        // 1. 회원 데이터 생성
        AuctionMember seller = createOrGetMember(1L, "seller@test.com", "판매자_제로");
        AuctionMember me = createOrGetMember(2L, "me@test.com", "입찰자_나");
        AuctionMember competitor = createOrGetMember(3L, "comp@test.com", "경쟁자_A");

        // [Part 1] API 및 UI 연동 테스트용 데이터
        log.info("--- [Part 1] API 연동 테스트 데이터 생성 ---");

        // 진행중 경매 + 입찰 경쟁 시나리오
        Product p1 = createProduct(seller.getId(), "[1-1] 레고 밀레니엄 팔콘");
        Auction a1 = createAuction(p1.getId(), seller.getId(), -60, 1440, 10_000, 1_000); // 1시간전~24시간후
        createBid(a1, competitor, 11_000);
        createBid(a1, me, 12_000);
        createBid(a1, competitor, 13_000);

        // 고가 경매 (3일 후 종료)
        Product p2 = createProduct(seller.getId(), "[1-2] 맥북 프로 M3");
        Auction a2 = createAuction(p2.getId(), seller.getId(), 0, 4320, 1_000_000, 50_000);
        createBid(a2, competitor, 1_050_000);
        createBid(a2, me, 1_100_000);

        // [Part 2] 상태별/스케줄링 테스트 데이터
        log.info("--- [Part 2] 상태별/SSE 스케줄링 데이터 생성 ---");

        // 2-1. 종료 + 낙찰 대상 (입찰 있음)
        Product p3 = createProduct(seller.getId(), "테스트 상품 1 (낙찰대상)");
        Auction a3 = createAuction(p3.getId(), p3.getSellerId(), -120, -60, 10_000, 1_000);
        createBid(a3, me, 15_000);
        a3.end(); // 종료 상태로 변경

        // 2-2. 종료 + 유찰 대상 (입찰 없음)
        Product p4 = createProduct(seller.getId(), "테스트 상품 2 (유찰대상)");
        Auction a4 = createAuction(p4.getId(), p4.getSellerId(), -120, 0, 20_000, 2_000);
        a4.end();

        // 2-3. SSE 테스트용 (1분 후 종료)
        Product p5 = createProduct(seller.getId(), "테스트 상품 3 (1분후 종료)");
        Auction a5 = createAuction(p5.getId(), p5.getSellerId(), -1, 1, 40_000, 4_000);
        createBid(a5, me, 45_000);
        publishCreatedEvent(a5);

        // 2-4. SSE 테스트용 (5분 후 종료)
        Product p6 = createProduct(seller.getId(), "테스트 상품 4 (5분후 종료)");
        Auction a6 = createAuction(p6.getId(), p6.getSellerId(), -1, 5, 50_000, 5_000);
        createBid(a6, me, 55_000);
        publishCreatedEvent(a6);

        log.info("=== 경매 테스트 데이터 초기화 완료 ===");
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

    private Product createProduct(Long sellerId, String name) {
        return productRepository.save(Product.builder()
                .sellerId(sellerId)
                .name(name)
                .description("테스트용 상품 설명")
                .category(Category.스타워즈)
                .productCondition(ProductCondition.MISB)
                .inspectionStatus(InspectionStatus.PENDING)
                .build());
    }

    private Auction createAuction(Long productId, Long sellerId, int startMinOffset, int endMinOffset, int startPrice, int tickSize) {
        LocalDateTime now = LocalDateTime.now();
        Auction auction = Auction.builder()
                .productId(productId)
                .sellerId(sellerId)
                .startTime(now.plusMinutes(startMinOffset))
                .endTime(now.plusMinutes(endMinOffset))
                .startPrice(startPrice)
                .tickSize(tickSize)
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

        auction.updateCurrentPrice(amount); // 입찰 시 현재가 갱신 반영
        auctionRepository.save(auction);
    }

    private void publishCreatedEvent(Auction auction) {
        eventPublisher.publishEvent(new AuctionCreatedEvent(auction.getId(), auction.getEndTime()));
    }
}