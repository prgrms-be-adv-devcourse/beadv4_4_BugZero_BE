package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

	@BeforeEach
	void setUp() {
		// @Value 주입을 위한 설정
		ReflectionTestUtils.setField(processor, "systemMemberId", 2L);
	}

	@Test
	@DisplayName("processSellerDeposit 성공: 판매자에게 정산금(PAID) 입금 및 상태 완료 변경")
	void processSellerDeposit_success() {
		// given
		Long settlementId = 1L;
		Long sellerId = 100L;
		int settlementAmount = 10000;

		Settlement settlement = mock(Settlement.class);
		Wallet sellerWallet = mock(Wallet.class);
		PaymentMember seller = mock(PaymentMember.class);

		given(paymentSupport.findSettlementByIdForUpdate(settlementId)).willReturn(settlement);
		given(settlement.getStatus()).willReturn(SettlementStatus.READY);
		given(settlement.getId()).willReturn(settlementId);
		given(settlement.getSettlementAmount()).willReturn(settlementAmount);
		given(settlement.getSeller()).willReturn(seller);
		given(seller.getId()).willReturn(sellerId);

		given(paymentSupport.findWalletByMemberIdForUpdate(sellerId)).willReturn(sellerWallet);
		given(sellerWallet.getMember()).willReturn(seller);

		// when
		processor.processSellerDeposit(settlementId);

		// then
		verify(sellerWallet).addBalance(settlementAmount);
		verify(settlement).complete(); // ✅ 상태 변경 확인 중요
		verify(paymentTransactionRepository).save(any());
	}

	@Test
	@DisplayName("processSystemDeposit 성공: 합산된 수수료를 시스템 지갑에 입금")
	void processSystemDeposit_success() {
		// given
		int totalFeeAmount = 5000;
		Long systemMemberId = 2L;
		Wallet systemWallet = mock(Wallet.class);
		PaymentMember systemMember = mock(PaymentMember.class);

		given(paymentSupport.findWalletByMemberIdForUpdate(systemMemberId)).willReturn(systemWallet);
		given(systemWallet.getMember()).willReturn(systemMember);

		// when
		processor.processSystemDeposit(totalFeeAmount);

		// then
		verify(systemWallet).addBalance(totalFeeAmount);

		ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
		verify(paymentTransactionRepository).save(captor.capture());

		assertThat(captor.getValue().getTransactionType()).isEqualTo(WalletTransactionType.SETTLEMENT_FEE);
		assertThat(captor.getValue().getBalanceDelta()).isEqualTo(totalFeeAmount);
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