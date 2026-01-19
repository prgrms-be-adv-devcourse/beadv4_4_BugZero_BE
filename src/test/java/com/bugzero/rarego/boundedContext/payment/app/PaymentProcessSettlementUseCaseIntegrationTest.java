package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;
import com.bugzero.rarego.boundedContext.payment.out.PaymentMemberRepository;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;
import com.bugzero.rarego.boundedContext.payment.out.WalletRepository;

@SpringBootTest
class PaymentProcessSettlementUseCaseIntegrationTest {
	@Autowired
	private PaymentProcessSettlementUseCase useCase;

	@Autowired
	private PaymentMemberRepository memberRepository;

	@Autowired
	private WalletRepository walletRepository;

	@Autowired
	private SettlementRepository settlementRepository;

	@Autowired
	private PaymentTransactionRepository paymentTransactionRepository;

	private final Long SYSTEM_ID = 2L;

	@BeforeEach
	void setUp() {
		// 삭제 순서 중요 (자식 -> 부모)
		paymentTransactionRepository.deleteAll();
		settlementRepository.deleteAll();
		walletRepository.deleteAll();
		memberRepository.deleteAll();

		// 1. 시스템 유저 및 지갑 생성 (ID: 2)
		createMemberAndWallet(SYSTEM_ID, "system");

		// 2. [동시성 테스트용] 판매자 생성 (ID: 100)
		createMemberAndWallet(100L, "seller");
	}

	@Test
	@DisplayName("동시성 테스트: 동시에 5개 스레드가 정산을 시도해도, 중복 정산되지 않아야 한다")
	void concurrency_double_spending_check() throws InterruptedException {
		// given
		PaymentMember seller = memberRepository.findById(100L).get();
		// 정산 데이터는 딱 1건만 존재 (10000원 정산, 1000원 수수료)
		createSettlement(seller, 10000, 1000);

		int threadCount = 5;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();

		// when
		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					useCase.processSettlements(10);
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();

		// then
		Settlement settlement = settlementRepository.findAll().get(0);
		assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.DONE);

		Wallet sellerWallet = walletRepository.findByMemberId(100L).get();
		assertThat(sellerWallet.getBalance()).isEqualTo(10000);

		Wallet systemWallet = walletRepository.findByMemberId(SYSTEM_ID).get();
		assertThat(systemWallet.getBalance()).isEqualTo(1000);

		assertThat(paymentTransactionRepository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("부분 성공 테스트: 지갑이 없는 판매자(실패)와 정상 판매자(성공)가 섞여 있어도 배치는 멈추지 않는다")
	void partial_success_integration_test() {
		// given
		// 1. 정상 판매자 (ID: 200, 지갑 있음)
		PaymentMember normalSeller = createMember(200L, "normal");
		createWallet(normalSeller);
		createSettlement(normalSeller, 10000, 1000);

		// 2. 오류 판매자 (ID: 300, 지갑 없음 -> WALLET_NOT_FOUND 유발)
		PaymentMember errorSeller = createMember(300L, "error");
		// createWallet(errorSeller); ❌ 지갑 생성 누락시킴
		createSettlement(errorSeller, 20000, 2000);

		// when
		// 배치 실행 (총 2건 처리 시도)
		int successCount = useCase.processSettlements(10);

		// then
		// 1. 성공 건수는 1건이어야 함 (정상 판매자만 성공)
		assertThat(successCount).isEqualTo(1);

		// 2. 정상 판매자 처리 검증
		Settlement normalSettlement = findSettlementBySellerId(200L);
		assertThat(normalSettlement.getStatus()).isEqualTo(SettlementStatus.DONE);

		Wallet normalWallet = walletRepository.findByMemberId(200L).get();
		assertThat(normalWallet.getBalance()).isEqualTo(10000); // 10000원 입금됨

		// 3. 오류 판매자 처리 검증
		Settlement errorSettlement = findSettlementBySellerId(300L);
		assertThat(errorSettlement.getStatus()).isEqualTo(SettlementStatus.FAILED); // FAILED 상태 변경됨

		// 4. 시스템 지갑 검증
		// setUp()에서 초기화되었으므로, 성공한 1건의 수수료(1000원)만 있어야 함
		Wallet systemWallet = walletRepository.findByMemberId(SYSTEM_ID).get();
		assertThat(systemWallet.getBalance()).isEqualTo(1000);

		// 5. 트랜잭션 타입 검증 (중요)
		// 시스템 수수료 입금 건이 'SETTLEMENT_FEE' 타입으로 잘 저장되었는지 확인
		boolean hasSystemFeeTx = paymentTransactionRepository.findAll().stream()
			.anyMatch(tx -> tx.getTransactionType() == WalletTransactionType.SETTLEMENT_FEE
				&& tx.getBalanceDelta() == 1000);
		assertThat(hasSystemFeeTx).isTrue();
	}

	// --- Helper Methods ---

	private PaymentMember createMember(Long id, String name) {
		PaymentMember member = PaymentMember.builder()
			.id(id)
			.publicId(UUID.randomUUID().toString())
			.email(name + "@test.com")
			.nickname(name)
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();
		return memberRepository.save(member);
	}

	private void createWallet(PaymentMember member) {
		Wallet wallet = Wallet.builder()
			.member(member)
			.balance(0)
			.holdingAmount(0)
			.build();
		walletRepository.save(wallet);
	}

	private void createMemberAndWallet(Long id, String name) {
		PaymentMember member = createMember(id, name);
		createWallet(member);
	}

	private void createSettlement(PaymentMember seller, int settlementAmount, int feeAmount) {
		Settlement settlement = Settlement.builder()
			.auctionId(System.nanoTime())
			.seller(seller)
			.salesAmount(settlementAmount + feeAmount)
			.feeAmount(feeAmount)
			.settlementAmount(settlementAmount)
			.status(SettlementStatus.READY)
			.build();
		settlementRepository.save(settlement);
	}

	private Settlement findSettlementBySellerId(Long sellerId) {
		return settlementRepository.findAll().stream()
			.filter(s -> s.getSeller().getId().equals(sellerId))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Settlement not found for seller " + sellerId));
	}
}