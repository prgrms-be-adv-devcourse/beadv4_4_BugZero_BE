package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
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

	@Mock
	private SettlementFeeRepository settlementFeeRepository;

	private final Long SYSTEM_MEMBER_ID = 2L;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(processor, "systemMemberId", SYSTEM_MEMBER_ID);
	}

	@Test
	@DisplayName("processSellerDeposit 성공: 정산금 입금 + 상태 완료 + [핵심] 수수료 대기열 저장(save)")
	void processSellerDeposit_success() {
		// given
		Long settlementId = 1L;
		Long sellerId = 100L;
		int settlementAmount = 10000;
		int feeAmount = 1000;

		Settlement settlement = mock(Settlement.class);
		Wallet sellerWallet = mock(Wallet.class);
		PaymentMember seller = mock(PaymentMember.class);

		given(paymentSupport.findSettlementByIdForUpdate(settlementId)).willReturn(settlement);
		given(settlement.getStatus()).willReturn(SettlementStatus.READY);
		given(settlement.getId()).willReturn(settlementId);
		given(settlement.getSettlementAmount()).willReturn(settlementAmount);
		given(settlement.getFeeAmount()).willReturn(feeAmount);
		given(settlement.getSeller()).willReturn(seller);
		given(seller.getId()).willReturn(sellerId);

		given(paymentSupport.findWalletByMemberIdForUpdate(sellerId)).willReturn(sellerWallet);
		given(sellerWallet.getMember()).willReturn(seller);

		// when
		processor.processSellerDeposit(settlementId);

		// then
		verify(sellerWallet).addBalance(settlementAmount);
		verify(settlement).complete();
		verify(paymentTransactionRepository).save(any(PaymentTransaction.class));

		// [검증] 수수료 대기열에 저장되었는지 확인
		ArgumentCaptor<SettlementFee> feeCaptor = ArgumentCaptor.forClass(SettlementFee.class);
		verify(settlementFeeRepository).save(feeCaptor.capture());

		assertThat(feeCaptor.getValue().getFeeAmount()).isEqualTo(feeAmount);
		assertThat(feeCaptor.getValue().getSettlement()).isEqualTo(settlement);
	}

	@Test
	@DisplayName("processFees 성공: 조회(Lock) -> 합산 -> 입금 -> 삭제가 원자적으로 수행된다")
	void processFees_success() {
		// given
		int size = 1000;

		// 수수료 더미 데이터 생성 (1000원 + 2000원 = 3000원 예상)
		SettlementFee fee1 = mock(SettlementFee.class);
		SettlementFee fee2 = mock(SettlementFee.class);
		given(fee1.getFeeAmount()).willReturn(1000);
		given(fee2.getFeeAmount()).willReturn(2000);
		List<SettlementFee> fees = List.of(fee1, fee2);

		// 1. Repository 조회 Mock (비관적 락이 걸린 메서드 호출 가정)
		given(settlementFeeRepository.findAllForBatch(any(Pageable.class))).willReturn(fees);

		// 2. 시스템 지갑 Mock
		Wallet systemWallet = mock(Wallet.class);
		PaymentMember systemMember = mock(PaymentMember.class);
		given(paymentSupport.findWalletByMemberIdForUpdate(SYSTEM_MEMBER_ID)).willReturn(systemWallet);
		given(systemWallet.getMember()).willReturn(systemMember);

		// when
		processor.processFees(size);

		// then
		// 1. 시스템 지갑에 합산 금액(3000원) 입금 확인
		verify(systemWallet).addBalance(3000);

		// 2. 거래 내역 저장 확인
		ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
		verify(paymentTransactionRepository).save(txCaptor.capture());
		assertThat(txCaptor.getValue().getTransactionType()).isEqualTo(WalletTransactionType.SETTLEMENT_FEE);
		assertThat(txCaptor.getValue().getBalanceDelta()).isEqualTo(3000);

		// 3. [핵심] 대기열 삭제 확인 (Bulk Delete)
		verify(settlementFeeRepository).deleteAllInBatch(fees);
	}

	@Test
	@DisplayName("processFees: 조회된 수수료가 없으면 아무 작업도 하지 않는다")
	void processFees_empty() {
		// given
		given(settlementFeeRepository.findAllForBatch(any(Pageable.class))).willReturn(Collections.emptyList());

		// when
		processor.processFees(1000);

		// then
		// 시스템 지갑 조회나 입금이 일어나면 안 됨
		verify(paymentSupport, never()).findWalletByMemberIdForUpdate(anyLong());
		verify(settlementFeeRepository, never()).deleteAllInBatch(anyList());
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
