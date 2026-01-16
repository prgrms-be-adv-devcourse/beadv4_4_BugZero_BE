package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;

@ExtendWith(MockitoExtension.class)
class PaymentSettlementProcessorTest {

	@InjectMocks
	private PaymentSettlementProcessor processor;

	@Mock
	private PaymentSupport paymentSupport;

	@Test
	@DisplayName("process 성공: 지갑 잔액 증가 및 정산 완료 상태 변경")
	void process_success() {
		// given
		Long settlementId = 1L;
		Long sellerId = 100L;
		int amount = 10000;
		int fee = 1000;

		Settlement settlement = mock(Settlement.class);
		PaymentMember seller = mock(PaymentMember.class);
		Wallet wallet = mock(Wallet.class);

		// Mock Stubbing
		given(paymentSupport.findSettlementByIdForUpdate(settlementId)).willReturn(settlement);
		given(settlement.getStatus()).willReturn(SettlementStatus.READY);
		given(settlement.getSeller()).willReturn(seller);
		given(seller.getId()).willReturn(sellerId);
		given(settlement.getSettlementAmount()).willReturn(amount);
		given(settlement.getFeeAmount()).willReturn(fee);

		given(paymentSupport.findWalletByMemberIdForUpdate(sellerId)).willReturn(wallet);

		// when
		int resultFee = processor.process(settlementId);

		// then
		assertThat(resultFee).isEqualTo(fee);
		verify(wallet).addBalance(amount); // 지갑 입금 확인
		verify(settlement).complete();     // 상태 완료 변경 확인
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