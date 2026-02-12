package com.bugzero.rarego.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.out.AuctionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionSettleOneUseCase {

	private final AuctionRepository auctionRepository;
	private final AuctionSettlementSupport support;

	public void execute(Long auctionId) {
		try {
			support.processSettlement(auctionId);
			log.info("경매 {} 정산 처리 완료", auctionId);

		} catch (CustomException e) {
			// 이미 다른 스레드에 의해 정산된 경우, 에러가 아닌 정상 흐름으로 간주
			if (e.getErrorType() == ErrorType.AUCTION_NOT_FOUND_OR_ALREADY_SETTLED) {
				log.warn("경매 {}는 이미 종료되었거나 정산 대상이 아닙니다.", auctionId);
				return;
			}
			throw e;
		}
	}
}
