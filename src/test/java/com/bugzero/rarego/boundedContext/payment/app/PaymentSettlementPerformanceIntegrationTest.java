package com.bugzero.rarego.boundedContext.payment.app;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.out.PaymentMemberRepository;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;
import com.bugzero.rarego.boundedContext.payment.out.WalletRepository;

@SpringBootTest
@ActiveProfiles("test")
class PaymentSettlementPerformanceIntegrationTest {
	@Autowired
	private PaymentProcessSettlementUseCase optimizedUseCase; // 현재 로직 (Bulk)
	@Autowired
	private BadPerformanceProcessor badProcessor; // 비교군 (One-by-One)

	@Autowired
	private PaymentMemberRepository memberRepository;
	@Autowired
	private WalletRepository walletRepository;
	@Autowired
	private SettlementRepository settlementRepository;
	@Autowired
	private PaymentTransactionRepository transactionRepository;

	private final int DATA_SIZE = 100; // 데이터 100개
	private final Long SYSTEM_ID = 2L;

	@BeforeEach
	void setUp() {
		// 데이터 초기화
		transactionRepository.deleteAll();
		settlementRepository.deleteAll();
		walletRepository.deleteAll();
		memberRepository.deleteAll();

		// 시스템 지갑 생성
		createMemberAndWallet(SYSTEM_ID, "system");

		// 판매자 지갑 생성
		createMemberAndWallet(100L, "seller");
	}

	@Test
	@DisplayName("성능 비교: [메모리 합산(Bulk)] vs [매 건 시스템지갑 락(One-by-One)]")
	void compare_performance() {
		StopWatch stopWatch = new StopWatch("정산 성능 비교");

		// ==========================================
		// CASE 1: 기존 방식 (매번 시스템 지갑 Update) - SLOW
		// ==========================================
		prepareSettlementData(DATA_SIZE); // 데이터 100개 생성

		stopWatch.start("1. 매 건 시스템 지갑 갱신 (One-by-One)");

		List<Settlement> settlements = settlementRepository.findAllByStatus(SettlementStatus.READY);
		for (Settlement s : settlements) {
			// "나쁜" 프로세서 호출
			badProcessor.processOneWithSystemUpdate(s.getId(), SYSTEM_ID);
		}

		stopWatch.stop();
		System.out.println("CASE 1 완료: 시스템 잔액 = " + walletRepository.findById(SYSTEM_ID).get().getBalance());

		// 데이터 초기화 (공정한 비교를 위해)
		transactionRepository.deleteAll();
		settlementRepository.deleteAll();
		walletRepository.findById(SYSTEM_ID).ifPresent(w -> {
			w.pay(w.getBalance()); // 잔액 0원 리셋
			walletRepository.save(w);
		});
		prepareSettlementData(DATA_SIZE); // 데이터 다시 100개 생성

		// ==========================================
		// CASE 2: 개선된 방식 (메모리 합산 후 1회 Update) - FAST
		// ==========================================
		stopWatch.start("2. 메모리 합산 처리 (Bulk Update)");

		// 현재 작성하신 UseCase 로직 실행
		optimizedUseCase.processSettlements(DATA_SIZE);

		stopWatch.stop();
		System.out.println("CASE 2 완료: 시스템 잔액 = " + walletRepository.findById(SYSTEM_ID).get().getBalance());

		// ==========================================
		// 결과 출력
		// ==========================================
		System.out.println(stopWatch.prettyPrint());
	}

	// --- Helper Methods ---

	private void prepareSettlementData(int count) {
		PaymentMember seller = memberRepository.findById(100L).orElseThrow();
		List<Settlement> list = new ArrayList<>();

		for (int i = 0; i < count; i++) {
			list.add(Settlement.builder()
				.auctionId(System.nanoTime() + i)
				.seller(seller)
				.salesAmount(11000)
				.settlementAmount(10000)
				.feeAmount(1000)
				.status(SettlementStatus.READY)
				.build());
		}
		settlementRepository.saveAll(list);
	}

	private void createMemberAndWallet(Long id, String name) {
		PaymentMember member = PaymentMember.builder()
			.id(id).publicId(UUID.randomUUID().toString())
			.email(name + "@test.com").nickname(name)
			.createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
			.build();
		memberRepository.save(member);

		walletRepository.save(Wallet.builder().member(member).balance(0).holdingAmount(0).build());
	}
}

/**
 * 성능 비교를 위해 임시로 만든 "나쁜" 프로세서
 * - 실제 프로덕션 코드에는 없고 테스트 패키지에만 존재
 */
@Component
class BadPerformanceProcessor {
	@Autowired
	private PaymentSupport paymentSupport;
	@Autowired
	private WalletRepository walletRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void processOneWithSystemUpdate(Long settlementId, Long systemId) {
		// 1. 정산 조회
		Settlement settlement = paymentSupport.findSettlementByIdForUpdate(settlementId);

		// 2. 판매자 입금
		Wallet sellerWallet = paymentSupport.findWalletByMemberIdForUpdate(settlement.getSeller().getId());
		sellerWallet.addBalance(settlement.getSettlementAmount());
		settlement.complete();

		// 3. [비효율의 핵심] 매번 시스템 지갑을 락 걸고 조회해서 업데이트
		Wallet systemWallet = walletRepository.findByMemberIdForUpdate(systemId).orElseThrow();
		systemWallet.addBalance(settlement.getFeeAmount());
		// 트랜잭션 종료 시점에 SystemWallet Update 쿼리 발생
	}
}