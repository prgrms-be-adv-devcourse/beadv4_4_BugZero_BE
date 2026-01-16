package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils; // ✅ 필수 추가

import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;

@ExtendWith(MockitoExtension.class)
class PaymentSettlementProcessorTest {
	@InjectMocks
	private PaymentSettlementProcessor processor;

	@Mock
	private PaymentSupport paymentSupport;

	@Mock
	private PaymentTransactionRepository paymentTransactionRepository;

	@Test
	@DisplayName("process 성공: 판매자 정산금(PAID) 및 시스템 수수료(FEE) 입금과 내역 저장")
	void process_success() {
		// ✅ [1] @Value 필드 주입 (단위 테스트에서는 Reflection으로 주입해야 함)
		Long systemMemberId = 2L;
		ReflectionTestUtils.setField(processor, "systemMemberId", systemMemberId);

		// given
		Long settlementId = 1L;
		Long sellerId = 100L;

		int settlementAmount = 10000;
		int feeAmount = 1000;

		// Mock 객체 생성
		Settlement settlement = mock(Settlement.class);

		Wallet sellerWallet = mock(Wallet.class);
		PaymentMember seller = mock(PaymentMember.class);

		// ✅ [2] 시스템 관련 Mock 추가
		Wallet systemWallet = mock(Wallet.class);
		PaymentMember systemMember = mock(PaymentMember.class);

		// Stubbing 1: 정산 정보
		given(paymentSupport.findSettlementByIdForUpdate(settlementId)).willReturn(settlement);
		given(settlement.getId()).willReturn(settlementId);
		given(settlement.getStatus()).willReturn(SettlementStatus.READY);
		given(settlement.getSettlementAmount()).willReturn(settlementAmount);
		given(settlement.getFeeAmount()).willReturn(feeAmount);

		given(settlement.getSeller()).willReturn(seller);
		given(seller.getId()).willReturn(sellerId);

		// Stubbing 2: 판매자 지갑
		given(paymentSupport.findWalletByMemberIdForUpdate(sellerId)).willReturn(sellerWallet);
		given(sellerWallet.getMember()).willReturn(seller); // Transaction 저장 시 NPE 방지
		given(sellerWallet.getBalance()).willReturn(50000); // 스냅샷용 임의 잔액

		// Stubbing 3: 시스템 지갑 (수수료가 > 0 이므로 호출됨)
		given(paymentSupport.findWalletByMemberIdForUpdate(systemMemberId)).willReturn(systemWallet);
		given(systemWallet.getMember()).willReturn(systemMember); // Transaction 저장 시 NPE 방지
		given(systemWallet.getBalance()).willReturn(0);

		// when
		processor.process(settlementId);

		// then
		// 1. 지갑 입금 확인
		verify(sellerWallet).addBalance(settlementAmount); // 판매자
		verify(systemWallet).addBalance(feeAmount);        // 시스템

		// 2. 정산 상태 변경 확인
		verify(settlement).complete();

		// 3. 거래 내역 저장 검증 (총 2번: 판매자 1번 + 시스템 1번)
		ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);

		// ✅ times(2)로 두 번의 호출을 모두 잡습니다.
		verify(paymentTransactionRepository, times(2)).save(captor.capture());

		List<PaymentTransaction> savedTransactions = captor.getAllValues();

		// [검증 A] 판매자용 Transaction
		PaymentTransaction sellerTx = savedTransactions.get(0); // 순서상 먼저 저장됨
		assertThat(sellerTx.getWallet()).isEqualTo(sellerWallet);
		assertThat(sellerTx.getBalanceDelta()).isEqualTo(settlementAmount);
		assertThat(sellerTx.getTransactionType()).isEqualTo(WalletTransactionType.SETTLEMENT_PAID);

		// [검증 B] 시스템용 Transaction
		PaymentTransaction systemTx = savedTransactions.get(1); // 나중에 저장됨
		assertThat(systemTx.getWallet()).isEqualTo(systemWallet);
		assertThat(systemTx.getBalanceDelta()).isEqualTo(feeAmount);
		assertThat(systemTx.getTransactionType()).isEqualTo(WalletTransactionType.SETTLEMENT_FEE);
	}

	@Test
	@DisplayName("process 스킵: 이미 READY 상태가 아니면 0 반환")
	void process_skip_if_not_ready() {
		// given
		Long settlementId = 1L;
		Settlement settlement = mock(Settlement.class);

		given(paymentSupport.findSettlementByIdForUpdate(settlementId)).willReturn(settlement);
		given(settlement.getStatus()).willReturn(SettlementStatus.DONE);

		// when
		int result = processor.process(settlementId);

		// then
		assertThat(result).isEqualTo(0);
		verify(paymentSupport, never()).findWalletByMemberIdForUpdate(any());
	}

	@Test
	@DisplayName("fail 성공: 정산 상태를 FAILED로 변경")
	void fail_success() {
		// given
		Long settlementId = 1L;
		Settlement settlement = mock(Settlement.class);
		given(paymentSupport.findSettlementByIdForUpdate(settlementId)).willReturn(settlement);
		given(settlement.getStatus()).willReturn(SettlementStatus.READY);

		// when
		processor.fail(settlementId);

		// then
		verify(settlement).fail();
	}
}