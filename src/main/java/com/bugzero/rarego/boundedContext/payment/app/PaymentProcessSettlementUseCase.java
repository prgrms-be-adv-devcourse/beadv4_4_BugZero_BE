package com.bugzero.rarego.boundedContext.payment.app;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;
import com.bugzero.rarego.boundedContext.payment.out.WalletRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessSettlementUseCase {
	private final PaymentSupport paymentSupport;
	private final SettlementRepository settlementRepository;
	private final WalletRepository walletRepository;

	@Value("${custom.payment.systemMemberId}")
	private Long systemMemberId;

	// TODO: Retry 도입
	@Transactional
	public int processSettlements(int limit) {
		List<Settlement> settlements = settlementRepository.findAllByStatus(SettlementStatus.READY,
			PageRequest.of(0, limit));

		if (settlements.isEmpty()) { // 정산 대상이 없으면 0 반환
			return 0;
		}

		// 정산 회원 ID 추출
		// 데드락 방지로 순서대로 wallet에 락을 걸기 위해 정렬
		List<Long> sellerIds = settlements.stream()
			.map(s -> s.getSeller().getId())
			.distinct()
			.sorted()
			.toList();

		// 판매자 지갑 조회 (순서대로 락 획득)
		Map<Long, Wallet> walletMap = paymentSupport.findWalletsByMemberIdsForUpdate(sellerIds);

		int totalSystemFee = 0; // 정산 수수료 합산용

		// 정산 수행
		for (Settlement settlement : settlements) {
			Wallet wallet = walletMap.get(settlement.getSeller().getId());

			if (wallet == null) {
				throw new CustomException(ErrorType.WALLET_NOT_FOUND);
			}

			wallet.addBalance(settlement.getSettlementAmount());
			totalSystemFee += settlement.getFeeAmount();

			settlement.complete();
		}

		if (totalSystemFee > 0) {
			int affectRows = walletRepository.increaseBalance(systemMemberId, totalSystemFee);

			if (affectRows == 0) {
				log.error("시스템 지갑 누락으로 정산 실패");
				throw new CustomException(ErrorType.WALLET_NOT_FOUND);
			}
		}

		return settlements.size();
	}
}
