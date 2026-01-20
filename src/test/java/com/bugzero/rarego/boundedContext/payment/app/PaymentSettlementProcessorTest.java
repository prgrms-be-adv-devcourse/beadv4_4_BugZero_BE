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
		// given
		Long systemMemberId = 2L;
		ReflectionTestUtils.setField(processor, "systemMemberId", systemMemberId);

		Long settlementId = 1L;
		Long sellerId = 100L;
		int settlementAmount = 10000;
		int feeAmount = 1000;

		Settlement settlement = mock(Settlement.class);
		Wallet sellerWallet = mock(Wallet.class);
		PaymentMember seller = mock(PaymentMember.class);
		Wallet systemWallet = mock(Wallet.class);
		PaymentMember systemMember = mock(PaymentMember.class);

		// Stubbing: 정산 정보
		given(paymentSupport.findSettlementByIdForUpdate(settlementId)).willReturn(settlement);
		given(settlement.getId()).willReturn(settlementId);
		given(settlement.getStatus()).willReturn(SettlementStatus.READY);
		given(settlement.getSettlementAmount()).willReturn(settlementAmount);
		given(settlement.getFeeAmount()).willReturn(feeAmount);
		given(settlement.getSeller()).willReturn(seller);
		given(seller.getId()).willReturn(sellerId);

		// Stubbing: 판매자 지갑
		given(paymentSupport.findWalletByMemberIdForUpdate(sellerId)).willReturn(sellerWallet);
		given(sellerWallet.getMember()).willReturn(seller);
		given(sellerWallet.getBalance()).willReturn(50000);

		// Stubbing: 시스템 지갑
		given(paymentSupport.findWalletByMemberIdForUpdate(systemMemberId)).willReturn(systemWallet);
		given(systemWallet.getMember()).willReturn(systemMember);
		given(systemWallet.getBalance()).willReturn(1000);

		// when
		processor.process(settlementId); // 반환값 없음(void)

		// then
		// 1. 지갑 입금 확인
		verify(sellerWallet).addBalance(settlementAmount);
		verify(systemWallet).addBalance(feeAmount);

		// 2. 정산 상태 변경 확인
		verify(settlement).complete();

		// 3. 거래 내역 저장 검증
		ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
		verify(paymentTransactionRepository, times(2)).save(captor.capture());

		List<PaymentTransaction> savedTransactions = captor.getAllValues();

		// 판매자 트랜잭션 검증
		PaymentTransaction sellerTx = savedTransactions.get(0);
		assertThat(sellerTx.getTransactionType()).isEqualTo(WalletTransactionType.SETTLEMENT_PAID);
		assertThat(sellerTx.getBalanceDelta()).isEqualTo(settlementAmount);

		// 시스템 트랜잭션 검증
		PaymentTransaction systemTx = savedTransactions.get(1);
		assertThat(systemTx.getTransactionType()).isEqualTo(WalletTransactionType.SETTLEMENT_FEE);
		assertThat(systemTx.getBalanceDelta()).isEqualTo(feeAmount);
	}

	@Test
	@DisplayName("process 스킵: 이미 READY 상태가 아니면 아무 작업도 하지 않아야 함")
	void process_skip_if_not_ready() {
		// given
		Long settlementId = 1L;
		Settlement settlement = mock(Settlement.class);

		given(paymentSupport.findSettlementByIdForUpdate(settlementId)).willReturn(settlement);
		given(settlement.getStatus()).willReturn(SettlementStatus.DONE);

		// when
		processor.process(settlementId); // void 이므로 실행만 함

		// then
		// READY가 아니면 지갑 조회 및 상태 변경이 일어나지 않아야 함
		verify(paymentSupport, never()).findWalletByMemberIdForUpdate(any());
		verify(settlement, never()).complete();
		verifyNoInteractions(paymentTransactionRepository);
	}

	@Test
	@DisplayName("fail 성공: 정산 상태가 READY인 경우에만 FAILED로 변경")
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