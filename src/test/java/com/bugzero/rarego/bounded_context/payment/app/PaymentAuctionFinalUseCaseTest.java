package com.bugzero.rarego.bounded_context.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.bounded_context.payment.domain.Deposit;
import com.bugzero.rarego.bounded_context.payment.domain.DepositStatus;
import com.bugzero.rarego.bounded_context.payment.domain.PaymentMember;
import com.bugzero.rarego.bounded_context.payment.domain.PaymentTransaction;
import com.bugzero.rarego.bounded_context.payment.domain.Wallet;
import com.bugzero.rarego.bounded_context.payment.in.dto.AuctionFinalPaymentRequestDto;
import com.bugzero.rarego.bounded_context.payment.in.dto.AuctionFinalPaymentResponseDto;
import com.bugzero.rarego.bounded_context.payment.out.DepositRepository;
import com.bugzero.rarego.bounded_context.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.bounded_context.payment.out.SettlementRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;
import com.bugzero.rarego.shared.auction.port.AuctionOrderPort;

@ExtendWith(MockitoExtension.class)
class PaymentAuctionFinalUseCaseTest {

	@InjectMocks
	private PaymentAuctionFinalUseCase paymentAuctionFinalUseCase;

	@Mock
	private AuctionOrderPort auctionOrderPort;

	@Mock
	private DepositRepository depositRepository;

	@Mock
	private PaymentTransactionRepository transactionRepository;

	@Mock
	private SettlementRepository settlementRepository;

	@Mock
	private PaymentSupport paymentSupport;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(paymentAuctionFinalUseCase, "paymentTimeoutDays", 3);
	}

	@Test
	@DisplayName("성공: 낙찰 결제 완료")
	void finalPayment_Success() {
		// given
		String memberPublicId = "uuid-member-1"; // String ID
		Long memberId = 1L; // Internal ID
		Long sellerId = 5L;
		Long auctionId = 100L;
		int finalPrice = 100000;
		int depositAmount = 10000;
		int expectedPaymentAmount = finalPrice - depositAmount;

		AuctionFinalPaymentRequestDto request = new AuctionFinalPaymentRequestDto(
			"홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");

		AuctionOrderDto order = new AuctionOrderDto(1L, auctionId, sellerId, memberId, finalPrice, "PROCESSING",
			LocalDateTime.now());

		// Member Mocking
		PaymentMember buyer = mock(PaymentMember.class);
		given(buyer.getId()).willReturn(memberId);
		given(buyer.getPublicId()).willReturn(memberPublicId);

		PaymentMember seller = mock(PaymentMember.class);

		Deposit deposit = Deposit.create(buyer, auctionId, depositAmount);
		Wallet wallet = Wallet.builder().balance(200000).holdingAmount(depositAmount).build();

		// [중요] Public ID로 조회 시 Member 반환 (ID 포함)
		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(buyer);

		// 이후 로직은 memberId(Long)를 사용하므로 기존 Mock 유지
		given(auctionOrderPort.findByAuctionId(auctionId)).willReturn(Optional.of(order));
		given(depositRepository.findByMemberIdAndAuctionId(memberId, auctionId))
			.willReturn(Optional.of(deposit));
		given(paymentSupport.findWalletByMemberIdForUpdate(memberId)).willReturn(wallet);

		// recordTransaction 등에서 재조회 하는 경우를 위한 Mock
		given(paymentSupport.findMemberById(memberId)).willReturn(buyer);
		given(paymentSupport.findMemberById(sellerId)).willReturn(seller);

		// when
		AuctionFinalPaymentResponseDto response = paymentAuctionFinalUseCase.finalPayment(memberPublicId, auctionId,
			request);

		// then
		assertThat(response.auctionId()).isEqualTo(auctionId);
		assertThat(response.finalPrice()).isEqualTo(finalPrice);
		assertThat(response.depositAmount()).isEqualTo(depositAmount);
		assertThat(response.paidAmount()).isEqualTo(expectedPaymentAmount);
		assertThat(response.status()).isEqualTo("PAID");

		// Wallet 잔액 검증
		int expectedBalance = 200000 - depositAmount - expectedPaymentAmount;
		assertThat(wallet.getBalance()).isEqualTo(expectedBalance);
		assertThat(wallet.getHoldingAmount()).isEqualTo(0);

		// Deposit 상태 검증
		assertThat(deposit.getStatus()).isEqualTo(DepositStatus.USED);

		// 트랜잭션 이력 2건 (보증금 사용, 잔금 결제)
		verify(transactionRepository, times(2)).save(any(PaymentTransaction.class));
		verify(auctionOrderPort).completeOrder(auctionId);
	}

	@Test
	@DisplayName("실패: 주문 정보 없음")
	void finalPayment_OrderNotFound() {
		// given
		String memberPublicId = "uuid-member-1";
		Long memberId = 1L;
		Long auctionId = 100L;
		AuctionFinalPaymentRequestDto request = new AuctionFinalPaymentRequestDto(
			"홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");

		PaymentMember buyer = mock(PaymentMember.class);
		given(buyer.getId()).willReturn(memberId);

		// [중요] Public ID -> Member 매핑
		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(buyer);

		given(auctionOrderPort.findByAuctionId(auctionId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> paymentAuctionFinalUseCase.finalPayment(memberPublicId, auctionId, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUCTION_ORDER_NOT_FOUND);
	}

	@Test
	@DisplayName("실패: 낙찰자 아님")
	void finalPayment_NotWinner() {
		// given
		String memberPublicId = "uuid-member-1";
		Long memberId = 1L;
		Long auctionId = 100L;
		Long winnerId = 999L; // 다른 사람

		AuctionFinalPaymentRequestDto request = new AuctionFinalPaymentRequestDto(
			"홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");
		AuctionOrderDto order = new AuctionOrderDto(1L, auctionId, 5L, winnerId, 100000, "PROCESSING",
			LocalDateTime.now());

		PaymentMember buyer = mock(PaymentMember.class);
		given(buyer.getId()).willReturn(memberId);
		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(buyer);

		given(auctionOrderPort.findByAuctionId(auctionId)).willReturn(Optional.of(order));

		// when & then
		assertThatThrownBy(() -> paymentAuctionFinalUseCase.finalPayment(memberPublicId, auctionId, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.NOT_AUCTION_WINNER);
	}

	@Test
	@DisplayName("실패: 주문 상태가 PROCESSING 아님")
	void finalPayment_InvalidOrderStatus() {
		// given
		String memberPublicId = "uuid-member-1";
		Long memberId = 1L;
		Long auctionId = 100L;

		AuctionFinalPaymentRequestDto request = new AuctionFinalPaymentRequestDto(
			"홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");
		AuctionOrderDto order = new AuctionOrderDto(1L, auctionId, 5L, memberId, 100000, "SUCCESS",
			LocalDateTime.now()); // 이미 완료

		PaymentMember buyer = mock(PaymentMember.class);
		given(buyer.getId()).willReturn(memberId);
		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(buyer);

		given(auctionOrderPort.findByAuctionId(auctionId)).willReturn(Optional.of(order));

		// when & then
		assertThatThrownBy(() -> paymentAuctionFinalUseCase.finalPayment(memberPublicId, auctionId, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.INVALID_ORDER_STATUS);
	}

	@Test
	@DisplayName("실패: 보증금 없음")
	void finalPayment_DepositNotFound() {
		// given
		String memberPublicId = "uuid-member-1";
		Long memberId = 1L;
		Long auctionId = 100L;

		AuctionFinalPaymentRequestDto request = new AuctionFinalPaymentRequestDto(
			"홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");
		AuctionOrderDto order = new AuctionOrderDto(1L, auctionId, 5L, memberId, 100000, "PROCESSING",
			LocalDateTime.now());

		PaymentMember buyer = mock(PaymentMember.class);
		given(buyer.getId()).willReturn(memberId);
		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(buyer);

		given(auctionOrderPort.findByAuctionId(auctionId)).willReturn(Optional.of(order));
		given(depositRepository.findByMemberIdAndAuctionId(memberId, auctionId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> paymentAuctionFinalUseCase.finalPayment(memberPublicId, auctionId, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.DEPOSIT_NOT_FOUND);
	}

	@Test
	@DisplayName("실패: 잔액 부족")
	void finalPayment_InsufficientBalance() {
		// given
		String memberPublicId = "uuid-member-1";
		Long memberId = 1L;
		Long auctionId = 100L;
		int finalPrice = 100000;
		int depositAmount = 10000;

		AuctionFinalPaymentRequestDto request = new AuctionFinalPaymentRequestDto(
			"홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");
		AuctionOrderDto order = new AuctionOrderDto(1L, auctionId, 5L, memberId, finalPrice, "PROCESSING",
			LocalDateTime.now());

		PaymentMember buyer = mock(PaymentMember.class);
		given(buyer.getId()).willReturn(memberId);
		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(buyer);

		Deposit deposit = Deposit.create(buyer, auctionId, depositAmount);
		Wallet wallet = Wallet.builder().balance(50000).holdingAmount(depositAmount).build(); // 잔액 부족

		given(auctionOrderPort.findByAuctionId(auctionId)).willReturn(Optional.of(order));
		given(depositRepository.findByMemberIdAndAuctionId(memberId, auctionId))
			.willReturn(Optional.of(deposit));
		given(paymentSupport.findWalletByMemberIdForUpdate(memberId)).willReturn(wallet);
		given(paymentSupport.findMemberById(memberId)).willReturn(buyer);

		// when & then
		assertThatThrownBy(() -> paymentAuctionFinalUseCase.finalPayment(memberPublicId, auctionId, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.INSUFFICIENT_BALANCE);
	}
}