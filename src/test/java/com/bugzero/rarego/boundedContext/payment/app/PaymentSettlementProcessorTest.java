package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
	@DisplayName("process 성공: 지갑 잔액 증가 및 정산 완료 상태 변경")
	void process_success() {
		// given
		Long settlementId = 1L;
		Long sellerId = 100L; // 판매자 ID 추가
		int depositAmount = 10000;
		int expectedBalance = 15000;

		// Mock 객체 생성
		Settlement settlement = mock(Settlement.class);
		Wallet wallet = mock(Wallet.class);
		PaymentMember seller = mock(PaymentMember.class); // ✅ Seller Mock 필요

		// Stubbing 1: 정산 정보
		given(paymentSupport.findSettlementByIdForUpdate(settlementId)).willReturn(settlement);
		given(settlement.getStatus()).willReturn(SettlementStatus.READY);
		given(settlement.getSettlementAmount()).willReturn(depositAmount);
		given(settlement.getFeeAmount()).willReturn(1000);

		// 🚨 [필수 추가] 이게 없으면 settlement.getSeller().getId()에서 NPE 발생
		given(settlement.getSeller()).willReturn(seller);
		given(seller.getId()).willReturn(sellerId);

		// Stubbing 2: 지갑 정보
		given(paymentSupport.findWalletByMemberIdForUpdate(sellerId)).willReturn(wallet);
		given(wallet.getBalance()).willReturn(expectedBalance);

		// 🚨 [필수 추가] 이게 없으면 Transaction 생성 시 wallet.getMember()에서 NPE 발생
		given(wallet.getMember()).willReturn(seller);

		// when
		processor.process(settlementId);

		// then
		// 1. 저장된 트랜잭션 데이터 검증
		ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
		verify(paymentTransactionRepository).save(captor.capture());

		PaymentTransaction savedTx = captor.getValue();
		assertThat(savedTx.getBalanceDelta()).isEqualTo(depositAmount);
		assertThat(savedTx.getBalanceAfter()).isEqualTo(expectedBalance);
		assertThat(savedTx.getHoldingDelta()).isEqualTo(0);
		// ✅ 타입 검증 추가 (중요)
		assertThat(savedTx.getTransactionType()).isEqualTo(WalletTransactionType.SETTLEMENT_PAID);

		// 2. 사이드 이펙트 검증 (실제 로직 호출 여부)
		verify(wallet).addBalance(depositAmount); // 지갑에 돈을 넣었는지
		verify(settlement).complete();            // 정산 상태를 바꿨는지
	}

	@Test
	@DisplayName("process 스킵: 이미 READY 상태가 아니면 0 반환")
	void process_skip_if_not_ready() {
		// given
		Long settlementId = 1L;
		Settlement settlement = mock(Settlement.class);

		// Stubbing: 정산 정보를 조회했더니 이미 DONE 상태임
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
		verify(settlement).fail(); // 상태 실패 변경 확인
	}
}