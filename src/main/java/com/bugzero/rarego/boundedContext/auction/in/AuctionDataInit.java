package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionCreatedEvent;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Configuration
@Slf4j
@Profile("dev")
public class AuctionDataInit {

    private final AuctionDataInit self;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AuctionDataInit(
            @Lazy AuctionDataInit self,
            AuctionRepository auctionRepository,
            BidRepository bidRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.self = self;
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
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

        // 4. 진행 중 + 1분 후 종료 (동적 스케줄링 테스트용)
        Auction auction4 = createAuction(
                4L,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusMinutes(1), // 1분 후 종료
                40_000,
                4_000
        );
        auction4.forceStartForTest();
        auctionRepository.save(auction4);
        bidRepository.save(createBid(auction4.getId(), 105L, 45_000));

        // 동적 스케줄링 등록 (1분 후 자동 정산됨)
        eventPublisher.publishEvent(
                AuctionCreatedEvent.builder()
                        .auctionId(auction4.getId())
                        .endTime(auction4.getEndTime())
                        .build()
        );

        // 5. 진행 중 + 5분 후 종료 (동적 스케줄링 테스트용)
        Auction auction5 = createAuction(
                5L,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusMinutes(5), // 5분 후 종료
                50_000,
                5_000
        );
        auction5.forceStartForTest();
        auctionRepository.save(auction5);
        bidRepository.save(createBid(auction5.getId(), 106L, 55_000));

        // 동적 스케줄링 등록 (5분 후 자동 정산됨)
        eventPublisher.publishEvent(
                AuctionCreatedEvent.builder()
                        .auctionId(auction5.getId())
                        .endTime(auction5.getEndTime())
                        .build()
        );

        log.info("=== 경매 테스트 데이터 초기화 완료 ===");
        log.info("- 낙찰 대상 (종료됨): 2건 (auction1, auction3)");
        log.info("- 유찰 대상 (종료됨): 1건 (auction2)");
        log.info("- 진행 중 (1분 후 자동 정산): 1건 (auction4)");
        log.info("- 진행 중 (5분 후 자동 정산): 1건 (auction5)");
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