package com.bugzero.rarego.boundedContext.payment.app;

import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

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
import com.bugzero.rarego.boundedContext.payment.out.PaymentMemberRepository;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.boundedContext.payment.out.SettlementFeeRepository;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;
import com.bugzero.rarego.boundedContext.payment.out.WalletRepository;

@SpringBootTest(properties = "custom.payment.settlement.holdDays=-1")
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

	@Autowired
	private SettlementFeeRepository settlementFeeRepository;

	private final Long SYSTEM_ID = 2L;

	@BeforeEach
	void setUp() {
		// [핵심 2] FK 제약조건 순서에 맞춰 자식 테이블부터 삭제
		settlementFeeRepository.deleteAll();
		paymentTransactionRepository.deleteAll();
		settlementRepository.deleteAll();
		walletRepository.deleteAll();
		memberRepository.deleteAll();

		// 1. 시스템 유저 및 지갑 생성
		createMemberAndWallet(SYSTEM_ID, "system");

		// 2. 판매자 생성 (기본 테스트용)
		createMemberAndWallet(100L, "seller");
	}

	@Test
	@DisplayName("동시성 테스트: 동시에 5개 스레드가 정산을 시도해도 중복 정산 및 중복 입금이 없어야 한다")
	void concurrency_double_spending_check() throws InterruptedException {
		// given
		PaymentMember seller = memberRepository.findById(100L).get();
		// 1건 생성 (수수료 1000원)
		createSettlement(seller, 10000, 1000);

		int threadCount = 5;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		// when
		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					// SKIP LOCKED 덕분에 5개 스레드가 경합해도 1개만 처리됨
					useCase.processSettlements(10);
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();

		// then
		// 1. 정산 상태 검증 (DONE)
		Settlement settlement = settlementRepository.findAll().get(0);
		assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.DONE);

		// 2. 판매자 잔액 검증 (중복 입금 시 20000원이 되므로 10000원인지 확인)
		Wallet sellerWallet = walletRepository.findByMemberId(100L).get();
		assertThat(sellerWallet.getBalance()).isEqualTo(10000);

		// 3. 시스템 잔액 검증 (수수료 1000원 확인)
		Wallet systemWallet = walletRepository.findByMemberId(SYSTEM_ID).get();
		assertThat(systemWallet.getBalance()).isEqualTo(1000);

		// 4. 트랜잭션 수 검증 (판매자 1건 + 시스템 1건 = 2건)
		assertThat(paymentTransactionRepository.count()).isEqualTo(2);

		// 5. 수수료 대기열이 비워졌는지 확인 (처리 완료 후 삭제됨)
		assertThat(settlementFeeRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("부분 성공 테스트: 실패 건은 재시도 대기 상태가 되고, 성공 건은 이벤트 리스너를 통해 수수료가 처리된다")
	void partial_success_integration_test() {
		// given
		PaymentMember normalSeller = createMember(200L, "normal");
		createWallet(normalSeller);
		createSettlement(normalSeller, 10000, 1000);

		PaymentMember errorSeller = createMember(300L, "error");
		// 지갑 미생성 -> 에러 유도
		createSettlement(errorSeller, 20000, 2000);

		// when
		// [중요] UseCase 자체에 @Transactional이 붙어있음.
		// 테스트 메서드가 @Transactional이 아니어야(SpringBootTest 기본값) 커밋 이벤트가 발생함.
		int successCount = useCase.processSettlements(10);

		// then
		assertThat(successCount).isEqualTo(1);

		// 1. 정상 판매자 검증
		Wallet normalWallet = walletRepository.findByMemberId(200L).get();
		assertThat(normalWallet.getBalance()).isEqualTo(10000);

		// 2. [수정] 오류 판매자 상태 검증
		// 이제 한 번 실패했다고 바로 FAILED가 아님!
		Settlement errorSettlement = findSettlementBySellerId(300L);
		// 상태는 여전히 READY (다음 배치가 집어가야 하니까)
		// 대신 retryCount가 1이어야 함
		assertThat(errorSettlement.getStatus()).isEqualTo(SettlementStatus.READY);
		assertThat(errorSettlement.getTryCount()).isEqualTo(1);

		// 3. [수정] 시스템 지갑 & 수수료 검증 (비동기 대기)
		// AFTER_COMMIT 리스너가 돌 때까지 최대 2초 기다림
		await().atMost(2, SECONDS).untilAsserted(() -> {
			Wallet systemWallet = walletRepository.findByMemberId(SYSTEM_ID).get();
			// 수수료 1000원 입금 확인
			assertThat(systemWallet.getBalance()).isEqualTo(1000);
		});

		// 4. 수수료 대기열 비워졌는지 확인 (역시 비동기 대기)
		await().atMost(2, SECONDS).untilAsserted(() -> {
			assertThat(settlementFeeRepository.findAll()).isEmpty();
		});
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