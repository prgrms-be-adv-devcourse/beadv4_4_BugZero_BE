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
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;
import com.bugzero.rarego.boundedContext.payment.out.WalletRepository;

@SpringBootTest
class PaymentProcessSettlementUseCaseIntegrationTest {
	@Autowired
	private PaymentProcessSettlementUseCase useCase; // 변수명 일치

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
		// 데이터 초기화
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
					int count = useCase.processSettlements(10);
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
		// 1. 정산 처리에 성공한 건수는 총 1건이어야 함
		assertThat(successCount.get()).isEqualTo(1);

		// 2. 판매자 지갑 잔액은 10,000원이어야 함
		Wallet sellerWallet = walletRepository.findByMemberId(100L).get();
		assertThat(sellerWallet.getBalance()).isEqualTo(10000);

		// 3. 시스템 지갑 잔액도 1,000원이어야 함
		Wallet systemWallet = walletRepository.findByMemberId(SYSTEM_ID).get();
		assertThat(systemWallet.getBalance()).isEqualTo(1000);
	}

	@Test
	@DisplayName("부분 성공 테스트: 지갑이 없는 판매자(실패)와 정상 판매자(성공)가 섞여 있어도 배치는 멈추지 않는다")
	void partial_success_integration_test() {
		// given
		// [수정] ID 충돌 방지를 위해 200, 300번대 ID 명시적 할당

		// 1. 정상 판매자 (ID: 200, 지갑 있음)
		PaymentMember normalSeller = createMember(200L, "normal");
		createWallet(normalSeller);
		createSettlement(normalSeller, 10000, 1000);

		// 2. 오류 판매자 (ID: 300, 지갑 생성 안 함 -> WALLET_NOT_FOUND 유발)
		PaymentMember errorSeller = createMember(300L, "error");
		// createWallet(errorSeller); // ❌ 지갑 생성 누락시킴 (의도적)
		createSettlement(errorSeller, 20000, 2000);

		// when
		// 배치 실행 (총 2건 처리 시도)
		int successCount = useCase.processSettlements(10);

		// then
		// 1. 성공 건수는 1건이어야 함 (정상 판매자만 성공)
		assertThat(successCount).isEqualTo(1);

		// 2. 정상 판매자는 DONE 상태여야 함
		Settlement normalSettlement = settlementRepository.findAll().stream()
			.filter(s -> s.getSeller().getId().equals(200L))
			.findFirst().get();
		assertThat(normalSettlement.getStatus()).isEqualTo(SettlementStatus.DONE);

		// 3. 정상 판매자 지갑 잔액 확인 (입금됨)
		Wallet normalWallet = walletRepository.findByMemberId(200L).get();
		assertThat(normalWallet.getBalance()).isEqualTo(10000);

		// 4. 오류 판매자는 FAILED 상태여야 함 (롤백되지 않고 상태 변경됨)
		Settlement errorSettlement = settlementRepository.findAll().stream()
			.filter(s -> s.getSeller().getId().equals(300L))
			.findFirst().get();
		assertThat(errorSettlement.getStatus()).isEqualTo(SettlementStatus.FAILED);

		// 5. 시스템 지갑에는 성공한 건(1000원)만 들어와야 함 (setUp 때 0원 시작 가정)
		// 참고: 동시성 테스트가 먼저 돌아서 1000원이 있을 수도 있으니,
		// 정확히 하려면 setUp()에서 deleteAll을 하거나, 증가분만 체크해야 합니다.
		// 여기선 setUp()에서 deleteAll()하므로 1000원이 맞음.
		Wallet systemWallet = walletRepository.findByMemberId(SYSTEM_ID).get();
		assertThat(systemWallet.getBalance()).isEqualTo(1000);
	}

	// --- Helper Methods ---

	// [수정] ID를 파라미터로 받도록 변경 (레플리카 개념 반영)
	private PaymentMember createMember(Long id, String name) {
		PaymentMember member = PaymentMember.builder()
			.id(id) // ✅ ID 직접 할당
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

	// 기존 편의 메서드도 내부적으로 위 메서드 호출하도록 수정
	private void createMemberAndWallet(Long id, String name) {
		PaymentMember member = createMember(id, name);
		createWallet(member);
	}

	private void createSettlement(PaymentMember seller, int settlementAmount, int feeAmount) {
		Settlement settlement = Settlement.builder()
			.auctionId(System.nanoTime()) // Unique ID 보장 (currentTimeMillis보다 충돌 가능성 낮음)
			.seller(seller)
			.salesAmount(settlementAmount + feeAmount)
			.feeAmount(feeAmount)
			.settlementAmount(settlementAmount)
			.status(SettlementStatus.READY)
			.build();
		settlementRepository.save(settlement);
	}
}