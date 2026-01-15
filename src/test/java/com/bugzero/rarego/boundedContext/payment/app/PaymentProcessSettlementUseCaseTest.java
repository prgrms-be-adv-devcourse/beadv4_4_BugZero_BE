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
import org.springframework.test.context.ActiveProfiles;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.out.PaymentMemberRepository;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;
import com.bugzero.rarego.boundedContext.payment.out.WalletRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.member.domain.MemberRole;
import com.bugzero.rarego.shared.member.domain.Provider;

@SpringBootTest
@ActiveProfiles("test")
class PaymentProcessSettlementUseCaseTest {

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
	@DisplayName("정상 흐름: 판매자에게 정산금 입금, 시스템에 수수료 입금, 상태 완료 처리")
	void normal_flow() {
		// given
		PaymentMember seller = memberRepository.findById(100L).get();
		createSettlement(seller, 10000, 1000); // 정산금 1만, 수수료 1천

		// when
		int processed = paymentProcessSettlementUseCase.processSettlements(10);

		// then
		assertThat(processed).isEqualTo(1);

		// 판매자 지갑 확인
		Wallet sellerWallet = walletRepository.findByMemberId(100L).get();
		assertThat(sellerWallet.getBalance()).isEqualTo(10000);

		// 시스템 지갑 확인
		Wallet systemWallet = walletRepository.findByMemberId(SYSTEM_ID).get();
		assertThat(systemWallet.getBalance()).isEqualTo(1000);

		// 정산 상태 확인
		Settlement settlement = settlementRepository.findAll().get(0);
		assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.DONE);
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
			.role(MemberRole.USER)
			.provider(Provider.GOOGLE)
			.providerId("pid_" + name)
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

	@Test
	@DisplayName("예외 테스트: 판매자 지갑이 없으면 전체 롤백되어야 한다")
	void rollback_when_seller_wallet_missing() {
		// given
		// 1. 판매자 회원은 있지만, 지갑(Wallet)은 생성하지 않음 (데이터 불일치 상황)
		PaymentMember noWalletSeller = PaymentMember.builder()
			.id(999L)
			.publicId(UUID.randomUUID().toString())
			.email("nowallet@test.com")
			.nickname("지갑없음")
			.role(MemberRole.USER)
			.provider(Provider.GOOGLE)
			.providerId("pid_nowallet")
			.build();
		memberRepository.save(noWalletSeller);

		// 2. 정산 데이터 생성
		createSettlement(noWalletSeller, 10000, 1000);

		// when & then
		// 예외가 발생하는지 검증
		assertThatThrownBy(() -> paymentProcessSettlementUseCase.processSettlements(10))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.WALLET_NOT_FOUND);

		// [중요] 롤백 검증: 정산 상태가 DONE으로 바뀌지 않고 READY로 남아있어야 함
		Settlement settlement = settlementRepository.findAll().get(0);
		assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.READY);

		// 시스템 지갑 잔액도 0원이어야 함 (수수료 입금 롤백)
		Wallet systemWallet = walletRepository.findByMemberId(SYSTEM_ID).get();
		assertThat(systemWallet.getBalance()).isEqualTo(0);
	}

	@Test
	@DisplayName("예외 테스트: 시스템 지갑이 없으면 판매자 정산까지 모두 롤백되어야 한다")
	void rollback_when_system_wallet_missing() {
		// given
		// 1. 시스템 지갑 삭제 (강제로 오류 상황 연출)
		walletRepository.deleteByMemberId(SYSTEM_ID);

		// 2. 정상 판매자 및 정산 데이터 준비
		PaymentMember seller = memberRepository.findById(100L).get();
		createSettlement(seller, 50000, 5000);

		// when & then
		// 우리가 만든 로직(affectedRows == 0)에 의해 예외가 터져야 함
		assertThatThrownBy(() -> paymentProcessSettlementUseCase.processSettlements(10))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.WALLET_NOT_FOUND);

		// [중요] 롤백 검증
		// 1. 판매자 지갑 잔액이 늘어나지 않았어야 함
		Wallet sellerWallet = walletRepository.findByMemberId(100L).get();
		assertThat(sellerWallet.getBalance()).isEqualTo(0);

		// 2. 정산 데이터 상태가 DONE이 아니어야 함
		Settlement settlement = settlementRepository.findAll().get(0);
		assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.READY);
	}

	@Test
	@DisplayName("청크 테스트: 데이터가 limit보다 많을 때, limit 개수만큼만 처리해야 한다")
	void check_chunk_size_limit() {
		// given
		PaymentMember seller = memberRepository.findById(100L).get();

		// 데이터 15개 생성
		for (int i = 0; i < 15; i++) {
			createSettlement(seller, 1000, 100);
		}

		// when
		// limit을 10으로 설정하여 호출
		int processedCount = paymentProcessSettlementUseCase.processSettlements(10);

		// then
		// 1. 처리된 개수는 10개여야 함
		assertThat(processedCount).isEqualTo(10);

		// 2. DB 상태 확인
		long doneCount = settlementRepository.findAll().stream()
			.filter(s -> s.getStatus() == SettlementStatus.DONE)
			.count();

		long readyCount = settlementRepository.findAll().stream()
			.filter(s -> s.getStatus() == SettlementStatus.READY)
			.count();

		assertThat(doneCount).isEqualTo(10); // 10개 처리됨
		assertThat(readyCount).isEqualTo(5); // 5개 남음
	}
}