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

    private final AuctionDataInit self;
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

        log.info("=== 경매 테스트 데이터 초기화 시작 ===");

        // ========== 1. 회원 데이터 생성 ==========
        AuctionMember seller = createOrGetMember(
                1L,
                "seller@auction.com",
                "AuctionSeller"
        );

        AuctionMember buyer = createOrGetMember(
                2L,
                "buyer@auction.com",
                "AuctionBuyer"
        );

        log.info("회원 데이터 생성 완료 - Seller: {}, Buyer: {}", seller.getId(), buyer.getId());

        // ========== 2. 상품 + 정상 경매 생성 ==========
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

        // ========== 3. 정산 테스트용 경매 생성 ==========

        // 3-1. 종료 + 입찰 있음 (낙찰 대상)
        Product product2 = createProduct(seller.getId(), "테스트 상품 1", 10_000);
        Auction auction1 = createAuctionSimple(
                product2.getId(),
                product2.getSellerId(),
                LocalDateTime.now().minusHours(2),  // 2시간 전 시작
                LocalDateTime.now().minusHours(1),  // 1시간 전 종료
                10_000,
                1_000
        );
        auction1.forceStartForTest();
        auction1.end();  // 종료 상태로
        auctionRepository.save(auction1);
        bidRepository.save(createBid(auction1.getId(), buyer.getId(), 15_000));
        bidRepository.save(createBid(auction1.getId(), seller.getId(), 20_000));
        bidRepository.save(createBid(auction1.getId(), buyer.getId(), 25_000));

        // 3-2. 종료 + 입찰 없음 (유찰 대상)
        Product product3 = createProduct(seller.getId(), "테스트 상품 2", 20_000);
        Auction auction2 = createAuctionSimple(
                product3.getId(),
                product3.getSellerId(),
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now(),  // 지금 종료
                20_000,
                2_000
        );
        auction2.forceStartForTest();
        auction2.end();
        auctionRepository.save(auction2);

        // 3-3. 종료 + 입찰 1건 (낙찰 대상)
        Product product4 = createProduct(seller.getId(), "테스트 상품 3", 30_000);
        Auction auction3 = createAuctionSimple(
                product4.getId(),
                product4.getSellerId(),
                LocalDateTime.now().minusHours(3),
                LocalDateTime.now(),
                30_000,
                3_000
        );
        auction3.forceStartForTest();
        auction3.end();
        auctionRepository.save(auction3);
        bidRepository.save(createBid(auction3.getId(), buyer.getId(), 35_000));

        // 3-4. 진행 중 + 10분 후 종료 (SSE 테스트용)
        Product product5 = createProduct(seller.getId(), "테스트 상품 4", 40_000);
        Auction auction4 = createAuctionSimple(
                product5.getId(),
                product5.getSellerId(),
                LocalDateTime.now(),                    // 현재 시각
                LocalDateTime.now().plusMinutes(2),    // 2분 후
                40_000,
                4_000
        );
        auction4.forceStartForTest();  // IN_PROGRESS
        auctionRepository.save(auction4);
        bidRepository.save(createBid(auction4.getId(), buyer.getId(), 45_000));

        eventPublisher.publishEvent(
                new AuctionCreatedEvent(
                        auction4.getId(),
                        auction4.getEndTime()));

        log.info("경매 4번 생성: start={}, end={}, status={}",
                auction4.getStartTime(), auction4.getEndTime(), auction4.getStatus());

        // 3-5. 진행 중 + 30분 후 종료 (SSE 테스트용)
        Product product6 = createProduct(seller.getId(), "테스트 상품 5", 50_000);
        Auction auction5 = createAuctionSimple(
                product6.getId(),
                product6.getSellerId(),
                LocalDateTime.now(),                    // 현재 시각
                LocalDateTime.now().plusMinutes(3),    // 3분 후
                50_000,
                5_000
        );
        auction5.forceStartForTest();  // IN_PROGRESS
        auctionRepository.save(auction5);
        bidRepository.save(createBid(auction5.getId(), buyer.getId(), 55_000));

        eventPublisher.publishEvent(
                new AuctionCreatedEvent(
                        auction5.getId(),
                        auction5.getEndTime()));

        log.info("경매 5번 생성: start={}, end={}, status={}",
                auction5.getStartTime(), auction5.getEndTime(), auction5.getStatus());

        log.info("=== 경매 테스트 데이터 초기화 완료 ===");
        log.info("- 정상 경매: 1건 (3일 후 종료)");
        log.info("- 낙찰 대상 (종료됨): 2건 (auction1, auction3)");
        log.info("- 유찰 대상 (종료됨): 1건 (auction2)");
        log.info("- 진행 중 (2분 후 자동 정산): 1건 (경매 4번)");
        log.info("- 진행 중 (3분 후 자동 정산): 1건 (경매 5번)");
    }

    private AuctionMember createOrGetMember(Long id, String email, String nickname) {
        return auctionMemberRepository.findById(id)
                .orElseGet(() -> {
                    AuctionMember member = AuctionMember.builder()
                            .id(id)
                            .publicId(UUID.randomUUID().toString())
                            .email(email)
                            .nickname(nickname)
                            .build();
                    return auctionMemberRepository.save(member);
                });
    }

    private Product createProduct(Long sellerId, String name, int basePrice) {
        Product product = Product.builder()
                .sellerId(sellerId)
                .name(name)
                .description("테스트용 상품입니다.")
                .category(Category.스타워즈)
                .productCondition(ProductCondition.MISB)
                .inspectionStatus(InspectionStatus.PENDING)
                .build();
        return productRepository.save(product);
    }

    // 시간을 직접 지정
    private Auction createAuctionSimple(
            Long productId,
            Long sellerId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int startPrice,
            int tickSize
    ) {
        return Auction.builder()
                .productId(productId)
                .sellerId(sellerId)
                .startTime(startTime)
                .endTime(endTime)
                .startPrice(startPrice)
                .tickSize(tickSize)
                .build();
    }

    private Bid createBid(Long auctionId, Long bidderId, int bidAmount) {
        return Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .bidAmount(bidAmount)
                .build();
    }
}