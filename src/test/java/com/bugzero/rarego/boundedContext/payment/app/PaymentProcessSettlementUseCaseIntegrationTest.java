package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
		paymentTransactionRepository.deleteAll();
		settlementRepository.deleteAll();
		walletRepository.deleteAll();
		memberRepository.deleteAll();

		// 1. 시스템 유저 및 지갑 생성
		createMemberAndWallet(SYSTEM_ID, "system");

		// 2. 판매자 생성
		createMemberAndWallet(100L, "seller");
	}

	@Test
	@DisplayName("동시성 테스트: 동시에 5개 스레드가 정산을 시도해도 중복 정산 및 중복 입금이 없어야 한다")
	void concurrency_double_spending_check() throws InterruptedException {
		// given
		PaymentMember seller = memberRepository.findById(100L).get();
		// 딱 1건만 존재
		createSettlement(seller, 10000, 1000);

		int threadCount = 5;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		// when
		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					// 여러 스레드가 동시에 이 메서드를 호출
					useCase.processSettlements(10);
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();

		// then
		// 1. 정산 상태 검증
		Settlement settlement = settlementRepository.findAll().get(0);
		assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.DONE);

		// 2. 판매자 잔액 검증 (중복 입금 확인)
		Wallet sellerWallet = walletRepository.findByMemberId(100L).get();
		assertThat(sellerWallet.getBalance()).isEqualTo(10000);

		// 3. 시스템 잔액 검증 (Bulk 입금 중복 확인)
		Wallet systemWallet = walletRepository.findByMemberId(SYSTEM_ID).get();
		assertThat(systemWallet.getBalance()).isEqualTo(1000);

		// 4. 트랜잭션 수 검증 (판매자 입금 1건 + 시스템 수수료 입금 1건 = 2건)
		assertThat(paymentTransactionRepository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("부분 성공 테스트: 실패 건이 있어도 성공 건의 수수료만 합산되어 시스템 지갑에 들어간다")
	void partial_success_integration_test() {
		// given
		// 1. 정상 판매자 (수수료 1000원)
		PaymentMember normalSeller = createMember(200L, "normal");
		createWallet(normalSeller);
		createSettlement(normalSeller, 10000, 1000);

		// 2. 오류 판매자 (수수료 2000원 - 지갑이 없어 FAILED 유발)
		PaymentMember errorSeller = createMember(300L, "error");
		createSettlement(errorSeller, 20000, 2000);

		// when
		// 배치 실행
		int successCount = useCase.processSettlements(10);

		// then
		assertThat(successCount).isEqualTo(1); // 1건만 성공

		// 정상 판매자 입금 확인
		Wallet normalWallet = walletRepository.findByMemberId(200L).get();
		assertThat(normalWallet.getBalance()).isEqualTo(10000);

		// 오류 판매자 상태 확인
		Settlement errorSettlement = findSettlementBySellerId(300L);
		assertThat(errorSettlement.getStatus()).isEqualTo(SettlementStatus.FAILED);

		// ✅ 시스템 지갑 검증: 성공한 1건의 수수료(1000원)만 입금되어야 함
		Wallet systemWallet = walletRepository.findByMemberId(SYSTEM_ID).get();
		assertThat(systemWallet.getBalance()).isEqualTo(1000);

		// 트랜잭션 타입 검증
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