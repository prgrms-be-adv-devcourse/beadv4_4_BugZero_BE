package com.bugzero.rarego.app;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.domain.Auction;
import com.bugzero.rarego.domain.Bid;
import com.bugzero.rarego.in.dto.AuctionAutoSettleResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionSettleExpiredUseCase {

	private final AuctionSettlementSupport support;

	// 전체 트랜잭션을 걸지 않음 (내부의 REQUIRES_NEW가 개별 관리하도록 함)
	public AuctionAutoSettleResponseDto execute() {
		LocalDateTime now = LocalDateTime.now();
		List<Auction> auctions = support.findExpiredAuctions(now);

		int success = 0;
		int fail = 0;
		List<AuctionAutoSettleResponseDto.SettlementDetail> details = new ArrayList<>();

		for (Auction auction : auctions) {
			try {
				// 개별 트랜잭션 실행
				support.processSettlement(auction.getId());

				// 성공 결과 수집
				if (support.hasBids(auction.getId())) {
					Bid winningBid = support.findWinningBid(auction.getId());
					details.add(AuctionAutoSettleResponseDto.SettlementDetail.success(
						auction.getId(), winningBid.getBidderId()));
				} else {
					details.add(AuctionAutoSettleResponseDto.SettlementDetail.failed(auction.getId()));
				}
				success++;
			} catch (Exception e) {
				log.error("경매 {} 정산 실패 - 다음 경매로 넘어갑니다.", auction.getId(), e);
				details.add(AuctionAutoSettleResponseDto.SettlementDetail.failed(auction.getId()));
				fail++;
			}
		}

		return AuctionAutoSettleResponseDto.from(now, auctions, success, fail, details);
	}
}
