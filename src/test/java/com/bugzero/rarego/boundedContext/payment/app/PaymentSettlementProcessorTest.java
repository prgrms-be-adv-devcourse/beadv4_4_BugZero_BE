package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

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
import com.bugzero.rarego.boundedContext.payment.domain.SettlementFee;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.boundedContext.payment.out.SettlementFeeRepository;

@ExtendWith(MockitoExtension.class)
class PaymentSettlementProcessorTest {
	@InjectMocks
	private PaymentSettlementProcessor processor;

	@Mock
	private PaymentSupport paymentSupport;

	@Mock
	private PaymentTransactionRepository paymentTransactionRepository;

	@Mock // [New] 추가
	private SettlementFeeRepository settlementFeeRepository;

	@BeforeEach
	void setUp() {
		// @Value 주입을 위한 설정
		ReflectionTestUtils.setField(processor, "systemMemberId", 2L);
	}

	@Test
	@DisplayName("processSellerDeposit 성공: 정산금 입금, 상태 완료, 수수료 대기열 저장까지 수행되어야 한다")
	void processSellerDeposit_success() {
		// given
		Long sellerId = 100L;
		int settlementAmount = 10000;
		int feeAmount = 1000;

		// Mock 객체 생성
		Settlement settlement = mock(Settlement.class);
		Wallet sellerWallet = mock(Wallet.class);
		PaymentMember seller = mock(PaymentMember.class);

		// Stubbing (행위 정의)
		given(settlement.getStatus()).willReturn(SettlementStatus.READY);
		given(settlement.getSettlementAmount()).willReturn(settlementAmount);
		given(settlement.getFeeAmount()).willReturn(feeAmount); // 수수료 저장 위해 필요
		given(settlement.getSeller()).willReturn(seller);

		given(seller.getId()).willReturn(sellerId);

		given(paymentSupport.findWalletByMemberIdForUpdate(sellerId)).willReturn(sellerWallet);
		given(sellerWallet.getMember()).willReturn(seller);

		// when
		// [변경] ID 대신 엔티티 자체를 넘김
		boolean result = processor.processSellerDeposit(settlement);

		// then
		assertThat(result).isTrue();

		// 1. 지갑 잔액 증가 검증
		verify(sellerWallet).addBalance(settlementAmount);

		// 2. 정산 상태 완료 검증
		verify(settlement).complete();

		// 3. 트랜잭션 기록 저장 검증
		verify(paymentTransactionRepository).save(any(PaymentTransaction.class));

		// 4. [New] 수수료 대기열(SettlementFee) 저장 검증 (핵심)
		ArgumentCaptor<SettlementFee> feeCaptor = ArgumentCaptor.forClass(SettlementFee.class);
		verify(settlementFeeRepository).save(feeCaptor.capture());

		assertThat(feeCaptor.getValue().getFeeAmount()).isEqualTo(feeAmount);
	}

	@Test
	@DisplayName("processFees 성공: 대기열의 수수료를 합산하여 시스템 지갑에 입금하고 대기열을 비운다")
	void processFees_success() {
		// given
		int limit = 10;
		Long systemMemberId = 2L;

		// 수수료 데이터 2건 준비 (1000원, 2000원)
		SettlementFee fee1 = mock(SettlementFee.class);
		given(fee1.getFeeAmount()).willReturn(1000);

		SettlementFee fee2 = mock(SettlementFee.class);
		given(fee2.getFeeAmount()).willReturn(2000);

		List<SettlementFee> fees = List.of(fee1, fee2);
		int expectedTotalFee = 3000;

		Wallet systemWallet = mock(Wallet.class);
		PaymentMember systemMember = mock(PaymentMember.class);

		// Stubbing
		given(settlementFeeRepository.findAllForBatch(limit)).willReturn(fees);
		given(paymentSupport.findWalletByMemberIdForUpdate(systemMemberId)).willReturn(systemWallet);
		given(systemWallet.getMember()).willReturn(systemMember);

		// when
		processor.processFees(limit);

		// then
		// 1. 시스템 지갑에 합산 금액(3000원) 입금 확인
		verify(systemWallet).addBalance(expectedTotalFee);

		// 2. 트랜잭션 기록 확인
		ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
		verify(paymentTransactionRepository).save(txCaptor.capture());
		assertThat(txCaptor.getValue().getTransactionType()).isEqualTo(WalletTransactionType.SETTLEMENT_FEE);
		assertThat(txCaptor.getValue().getBalanceDelta()).isEqualTo(expectedTotalFee);

		// 3. [New] 처리된 수수료 데이터 삭제 확인 (Queue 비우기)
		verify(settlementFeeRepository).deleteAllInBatch(fees);
	}

	@Test
	@DisplayName("processFees: 처리할 수수료가 없으면 아무 작업도 하지 않는다")
	void processFees_empty() {
		// given
		given(settlementFeeRepository.findAllForBatch(anyInt())).willReturn(List.of());

		// when
		processor.processFees(10);

		// then
		verify(paymentSupport, never()).findWalletByMemberIdForUpdate(anyLong());
		verify(settlementFeeRepository, never()).deleteAllInBatch(anyList());
	}
}