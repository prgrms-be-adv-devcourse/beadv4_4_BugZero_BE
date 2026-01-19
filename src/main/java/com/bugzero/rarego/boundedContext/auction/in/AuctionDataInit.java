package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionCreatedEvent;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
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
    private final AuctionOrderRepository auctionOrderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void run(String... args) {
        if (auctionRepository.count() > 0) {
            log.info("경매 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("경매 전체 테스트 데이터 초기화 시작...");

        // 0. 회원 생성
        AuctionMember seller = createOrGetMember(1L, "seller@test.com", "판매자_제로");
        AuctionMember me = createOrGetMember(2L, "me@test.com", "입찰자_나"); // buyer 역할
        AuctionMember competitor = createOrGetMember(3L, "comp@test.com", "경쟁자_A");

        // me를 buyer로 사용하기 위해 변수 할당 (기존 코드와의 호환성)
        AuctionMember buyer = me;

        // ==========================================
        // [Part 1] 경매 및 입찰 데이터 생성
        // ==========================================
        log.info("--- [Part 1] API 연동 테스트 데이터 ---");

        // 1. [진행중] 입찰이 있는 경매
        Product product1 = createProduct(seller.getId(), "[1-1] 레고 밀레니엄 팔콘", 10_000);
        // 시작: 1시간 전, 종료: 24시간 후 (분 단위)
        Auction auction1 = createAuction(product1.getId(), seller.getId(), -60, 1440, 10_000, 1_000);

        createBid(auction1, competitor, 15_000);
        createBid(auction1, me, 20_000);
        createBid(auction1, competitor, 25_000);

        // 2. [진행중] 고가 경매 (입찰 경쟁)
        Product productForHighPrice = createProduct(seller.getId(), "[1-2] 맥북 프로 M3", 1_000_000);
        Auction normalAuction = createAuction(productForHighPrice.getId(), seller.getId(), 0, 4320, 1_000_000, 50_000); // 3일 후 종료

        createBid(normalAuction, competitor, 1_050_000);
        createBid(normalAuction, me, 1_100_000);

        // 3. [종료] 패찰된 경매
        Product product3 = createProduct(seller.getId(), "[1-3] 아이폰 15 (패찰)", 2_000_000);
        // 시작: 3시간 전, 종료: 2시간 전 (이미 종료됨)
        createAuction(product3.getId(), seller.getId(), -180, -120, 2_000_000, 50_000);


        // ==========================================
        // [Part 2] 상태별/스케줄링 테스트 데이터
        // ==========================================
        log.info("--- [Part 2] 상태별 시나리오 데이터 ---");

        // 3-1. 종료 + 입찰 있음 (낙찰 대상)
        Product product4 = createProduct(seller.getId(), "테스트 상품 1 (낙찰대상)", 10_000);
        // 시작: 2분 전, 종료: 1분 전
        Auction auctionEndWithBid = createAuction(product4.getId(), product4.getSellerId(), -2, -1, 10_000, 1_000);
        createBid(auctionEndWithBid, buyer, 15_000);
        createBid(auctionEndWithBid, seller, 20_000); // 판매자가 입찰? 테스트라 가정
        createBid(auctionEndWithBid, buyer, 25_000);

        // 3-2. 종료 + 입찰 없음 (유찰 대상)
        Product product5 = createProduct(seller.getId(), "테스트 상품 2 (유찰대상)", 20_000);
        // 시작: 2분 전, 종료: 현재 (0분)
        createAuction(product5.getId(), product5.getSellerId(), -2, 0, 20_000, 2_000);

        // 3-3. 종료 + 입찰 1건 (낙찰 대상)
        Product product6 = createProduct(seller.getId(), "테스트 상품 3 (단독입찰)", 30_000);
        // 시작: 3분 전, 종료: 현재 (0분)
        Auction auctionEndWithOneBid = createAuction(product6.getId(), product6.getSellerId(), -3, 0, 30_000, 3_000);
        createBid(auctionEndWithOneBid, buyer, 35_000);

        // 3-4. 진행 중 + 1분 후 종료 (동적 스케줄링 테스트용)
        Product product7 = createProduct(seller.getId(), "테스트 상품 4 (1분후 종료)", 40_000);
        Auction auctionSoonEnd1 = createAuction(product7.getId(), product7.getSellerId(), -1, 1, 40_000, 4_000);
        createBid(auctionSoonEnd1, buyer, 45_000);

        eventPublisher.publishEvent(
            new AuctionCreatedEvent(
                auctionSoonEnd1.getId(),
                auctionSoonEnd1.getEndTime()));

        // 3-5. 진행 중 + 5분 후 종료 (동적 스케줄링 테스트용)
        Product product8 = createProduct(seller.getId(), "테스트 상품 5 (5분후 종료)", 50_000);
        Auction auctionSoonEnd5 = createAuction(product8.getId(), product8.getSellerId(), -1, 5, 50_000, 5_000);
        createBid(auctionSoonEnd5, buyer, 55_000);

        eventPublisher.publishEvent(
            new AuctionCreatedEvent(
                auctionSoonEnd5.getId(),
                auctionSoonEnd5.getEndTime()));

        log.info("=== 경매 테스트 데이터 초기화 완료 ===");

        // ==========================================
        // [Part 3] 낙찰(주문) 테스트 데이터 생성 (추가됨)
        // ==========================================
        log.info("--- [Part 3] 낙찰 및 주문 데이터 생성 ---");

        // 1. 낙찰될 상품 생성
        Product soldProduct = createProduct(seller.getId(), "[낙찰 완료] 레고 타이타닉", 500_000);

        // 2. 종료된 경매 생성 (시작: 5시간 전, 종료: 1시간 전)
        Auction endedAuction = createAuction(soldProduct.getId(), seller.getId(), -300, -60, 500_000, 50_000);

        // 3. 입찰 생성 (buyer가 60만원에 입찰)
        createBid(endedAuction, buyer, 600_000);

        // 4. 경매 상태 강제 종료 (ENDED)
        endedAuction.end();
        auctionRepository.save(endedAuction);

        // 5. 주문 정보(AuctionOrder) 생성 및 저장
        AuctionOrder order = AuctionOrder.builder()
            .auctionId(endedAuction.getId())
            .sellerId(seller.getId())
            .bidderId(buyer.getId())
            .finalPrice(600_000)
            .build(); // status 기본값 PROCESSING

        auctionOrderRepository.save(order); // DB에 저장

        log.info("낙찰 데이터 생성 완료! Auction ID: {}, Order ID 생성을 위해 DB 확인 필요", endedAuction.getId());
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

    // startMinutesOffset: 현재 시간 기준 시작 시간 (분)
    // endMinutesOffset: 현재 시간 기준 종료 시간 (분)
    private Auction createAuction(
        Long productId,
        Long sellerId,
        int startMinutesOffset,
        int endMinutesOffset,
        int startPrice,
        int tickSize
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.plusMinutes(startMinutesOffset);
        LocalDateTime endTime = now.plusMinutes(endMinutesOffset);

        Auction auction = Auction.builder()
            .productId(productId)
            .sellerId(sellerId)
            .startTime(startTime)
            .endTime(endTime)
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

        // 경매 현재가 갱신
        auction.updateCurrentPrice(amount);
        auctionRepository.save(auction);
    }
}