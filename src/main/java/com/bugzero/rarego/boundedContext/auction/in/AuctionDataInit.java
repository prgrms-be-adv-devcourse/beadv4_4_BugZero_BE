package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;

@Configuration
@Slf4j
@Profile("local") // local 환경에서만 실행
public class AuctionDataInit {

    private final AuctionDataInit self;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    public AuctionDataInit(
            @Lazy AuctionDataInit self,
            AuctionRepository auctionRepository,
            BidRepository bidRepository
    ) {
        this.self = self;
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
    }

    @Bean
    public ApplicationRunner auctionBaseInitDataRunner() {
        return args -> {
            self.makeBaseAuctionData();
        };
    }

    public void makeBaseAuctionData() {
        if (auctionRepository.count() > 0) {
            log.info("이미 경매 데이터가 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("=== 경매 테스트 데이터 초기화 시작 ===");

        // 1. 종료 + 입찰 있음 (낙찰 대상)
        Auction auction1 = createAuction(
                1L,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                10_000,
                1_000
        );
        auction1.forceStartForTest();
        auctionRepository.save(auction1);

        bidRepository.save(createBid(auction1.getId(), 101L, 15_000));
        bidRepository.save(createBid(auction1.getId(), 102L, 20_000));
        bidRepository.save(createBid(auction1.getId(), 103L, 25_000));

        // 2. 종료 + 입찰 없음 (유찰 대상)
        Auction auction2 = createAuction(
                2L,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusMinutes(30),
                20_000,
                2_000
        );
        auction2.forceStartForTest();
        auctionRepository.save(auction2);

        // 3. 종료 + 입찰 1건 (낙찰 대상)
        Auction auction3 = createAuction(
                3L,
                LocalDateTime.now().minusHours(3),
                LocalDateTime.now().minusMinutes(10),
                30_000,
                3_000
        );
        auction3.forceStartForTest();
        auctionRepository.save(auction3);

        bidRepository.save(createBid(auction3.getId(), 104L, 35_000));

        // 4. 아직 진행 중 (처리 대상 아님)
        Auction auction4 = createAuction(
                4L,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                40_000,
                4_000
        );
        auction4.forceStartForTest();
        auctionRepository.save(auction4);

        bidRepository.save(createBid(auction4.getId(), 105L, 45_000));

        log.info("=== 경매 테스트 데이터 초기화 완료 ===");
        log.info("- 낙찰 대상: 2건 (auction1, auction3)");
        log.info("- 유찰 대상: 1건 (auction2)");
        log.info("- 진행 중: 1건 (auction4)");
    }

    private Auction createAuction(
            Long productId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int startPrice,
            int tickSize
    ) {
        return Auction.builder()
                .productId(productId)
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
