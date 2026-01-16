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
import com.bugzero.rarego.boundedContext.payment.out.PaymentMemberRepository;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;
import com.bugzero.rarego.boundedContext.payment.out.WalletRepository;

@SpringBootTest
class PaymentProcessSettlementUseCaseIntegrationTest {
	@Autowired
	private PaymentProcessSettlementUseCase paymentProcessSettlementUseCase;

	@Autowired
	private PaymentMemberRepository memberRepository;

	@Autowired
	private WalletRepository walletRepository;

	@Autowired
	private SettlementRepository settlementRepository;

	private final Long SYSTEM_ID = 2L;

	@BeforeEach
	void setUp() {
		// 데이터 초기화
		settlementRepository.deleteAll();
		walletRepository.deleteAll();
		memberRepository.deleteAll();

		// 1. 시스템 유저 및 지갑 생성 (ID: 2)
		createMemberAndWallet(SYSTEM_ID, "system");

		// 2. 판매자 생성 (ID: 100)
		createMemberAndWallet(100L, "seller");
	}

	@Test
	@DisplayName("동시성 테스트: 동시에 5개 스레드가 정산을 시도해도, 중복 정산되지 않아야 한다")
	void concurrency_double_spending_check() throws InterruptedException {
		// given
		PaymentMember seller = memberRepository.findById(100L).get();
		// 정산 데이터는 딱 1건만 존재
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
					// 동시에 진입 시도
					int count = paymentProcessSettlementUseCase.processSettlements(10);
					if (count > 0)
						successCount.addAndGet(count);
				} catch (Exception e) {
					failCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await(); // 모든 스레드가 끝날 때까지 대기

		// then
		// 1. 정산 처리에 성공한 건수는 총 1건이어야 함 (5건이면 망함)
		assertThat(successCount.get()).isEqualTo(1);

		// 2. 판매자 지갑 잔액은 10,000원이어야 함 (중복 입금 시 20,000원 이상 됨)
		Wallet sellerWallet = walletRepository.findByMemberId(100L).get();
		assertThat(sellerWallet.getBalance()).isEqualTo(10000);

		// 3. 시스템 지갑 잔액도 1,000원이어야 함
		Wallet systemWallet = walletRepository.findByMemberId(SYSTEM_ID).get();
		assertThat(systemWallet.getBalance()).isEqualTo(1000);
	}

	private void createMemberAndWallet(Long id, String name) {
		PaymentMember member = PaymentMember.builder()
			.id(id) // ID 강제 할당 (테스트용)
			.publicId(UUID.randomUUID().toString())
			.email(name + "@test.com")
			.nickname(name)
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();
		memberRepository.save(member);

		Wallet wallet = Wallet.builder()
			.member(member)
			.balance(0)
			.holdingAmount(0)
			.build();
		walletRepository.save(wallet);
	}

	private void createSettlement(PaymentMember seller, int settlementAmount, int feeAmount) {
		Settlement settlement = Settlement.builder()
			.auctionId(System.currentTimeMillis()) // Random ID
			.seller(seller)
			.salesAmount(settlementAmount + feeAmount)
			.feeAmount(feeAmount)
			.settlementAmount(settlementAmount)
			.status(SettlementStatus.READY)
			.build();
		settlementRepository.save(settlement);
	}
}
