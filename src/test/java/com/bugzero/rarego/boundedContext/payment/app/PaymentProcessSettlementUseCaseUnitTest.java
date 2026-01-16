package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;
import com.bugzero.rarego.boundedContext.payment.out.WalletRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

@ExtendWith(MockitoExtension.class)
class PaymentProcessSettlementUseCaseUnitTest {

	@InjectMocks
	private PaymentProcessSettlementUseCase useCase;

	@Mock
	private PaymentSupport paymentSupport; // 새로 추가된 의존성

	@Mock
	private SettlementRepository settlementRepository;

	@Mock
	private WalletRepository walletRepository;

	private final Long SYSTEM_ID = 2L;

	@BeforeEach
	void setUp() {
		// @Value("${custom.payment.systemMemberId}") 값을 주입하기 위해 ReflectionTestUtils 사용
		// 단위 테스트는 스프링 컨텍스트를 띄우지 않으므로 직접 넣어줘야 합니다.
		ReflectionTestUtils.setField(useCase, "systemMemberId", SYSTEM_ID);
	}

	@Test
	@DisplayName("정상 흐름: 판매자 지갑 잔액 증가, 정산 완료 처리, 시스템 수수료 입금")
	void normal_flow() {
		// given
		int limit = 10;
		Long sellerId = 100L;

		// 1. 판매자 & 지갑 & 정산 데이터 생성
		PaymentMember seller = createMember(sellerId, "seller");
		Wallet sellerWallet = createWallet(seller, 0); // 초기 잔액 0원
		Settlement settlement = createSettlement(seller, 10000, 1000); // 정산금 1만, 수수료 1천

		// 2. Mocking - 정산 대기 목록 조회
		given(settlementRepository.findAllByStatus(eq(SettlementStatus.READY), any(Pageable.class)))
			.willReturn(List.of(settlement));

		// 3. Mocking - 판매자 지갑 조회 (PaymentSupport)
		// 로직 내부에서 sellerIds를 추출해서 호출하므로, Map 형태로 리턴해야 함
		given(paymentSupport.findWalletsByMemberIdsForUpdate(anyList()))
			.willReturn(Map.of(sellerId, sellerWallet));

		// 4. Mocking - 시스템 지갑 수수료 입금 (성공 시 1 리턴 가정)
		// increaseBalance(memberId, amount) -> return affectedRows
		given(walletRepository.increaseBalance(eq(SYSTEM_ID), eq(1000)))
			.willReturn(1);

		// when
		int processedCount = useCase.processSettlements(limit);

		// then
		assertThat(processedCount).isEqualTo(1);

		// 검증 1: 판매자 지갑 잔액이 10,000원 늘었는지 (객체 상태 검증)
		assertThat(sellerWallet.getBalance()).isEqualTo(10000);

		// 검증 2: 정산 상태가 DONE(완료)로 변경되었는지
		assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.DONE);

		// 검증 3: 시스템 지갑 입금 메서드가 호출되었는지
		verify(walletRepository, times(1)).increaseBalance(eq(SYSTEM_ID), eq(1000));
	}

	@Test
	@DisplayName("빈 목록: 처리할 정산 건이 없으면 0을 반환하고 종료")
	void return_zero_when_empty() {
		// given
		given(settlementRepository.findAllByStatus(eq(SettlementStatus.READY), any(Pageable.class)))
			.willReturn(new ArrayList<>()); // 빈 리스트

		// when
		int result = useCase.processSettlements(10);

		// then
		assertThat(result).isEqualTo(0);

		// 검증: 이후 로직(지갑 조회 등)이 실행되지 않아야 함
		verify(paymentSupport, never()).findWalletsByMemberIdsForUpdate(anyList());
	}

	@Test
	@DisplayName("예외 테스트: 판매자 지갑을 찾을 수 없으면 WALLET_NOT_FOUND 예외 발생")
	void fail_when_seller_wallet_missing() {
		// given
		Long sellerId = 100L;
		PaymentMember seller = createMember(sellerId, "seller");
		Settlement settlement = createSettlement(seller, 10000, 1000);

		// 정산 데이터는 있음
		given(settlementRepository.findAllByStatus(eq(SettlementStatus.READY), any(Pageable.class)))
			.willReturn(List.of(settlement));

		// 하지만 지갑 조회 시 빈 Map 반환 (지갑 없음)
		given(paymentSupport.findWalletsByMemberIdsForUpdate(anyList()))
			.willReturn(Map.of());

		// when & then
		assertThatThrownBy(() -> useCase.processSettlements(10))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.WALLET_NOT_FOUND);
	}

	@Test
	@DisplayName("예외 테스트: 시스템 지갑 업데이트 실패(affectedRows=0) 시 예외 발생")
	void fail_when_system_wallet_update_fails() {
		// given
		Long sellerId = 100L;
		PaymentMember seller = createMember(sellerId, "seller");
		Wallet sellerWallet = createWallet(seller, 0);
		Settlement settlement = createSettlement(seller, 10000, 1000); // 수수료 1000원 존재

		given(settlementRepository.findAllByStatus(eq(SettlementStatus.READY), any(Pageable.class)))
			.willReturn(List.of(settlement));

		given(paymentSupport.findWalletsByMemberIdsForUpdate(anyList()))
			.willReturn(Map.of(sellerId, sellerWallet));

		// [핵심] 시스템 지갑 업데이트 시 0 반환 (업데이트 실패 시뮬레이션)
		given(walletRepository.increaseBalance(eq(SYSTEM_ID), eq(1000)))
			.willReturn(0);

		// when & then
		assertThatThrownBy(() -> useCase.processSettlements(10))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.WALLET_NOT_FOUND);
	}

	// --- Helper Methods ---
	private PaymentMember createMember(Long id, String name) {
		return PaymentMember.builder()
			.id(id)
			.publicId(UUID.randomUUID().toString())
			.build();
	}

	private Wallet createWallet(PaymentMember member, int balance) {
		// Wallet 객체 생성 (실제 도메인 로직에 따라 빌더 사용)
		return Wallet.builder()
			.member(member)
			.balance(balance)
			.build();
	}

	private Settlement createSettlement(PaymentMember seller, int settlementAmount, int feeAmount) {
		// Settlement 객체 생성
		// status가 READY 상태여야 함
		return Settlement.builder()
			.seller(seller)
			.settlementAmount(settlementAmount)
			.feeAmount(feeAmount)
			.status(SettlementStatus.READY)
			.build();
	}
}