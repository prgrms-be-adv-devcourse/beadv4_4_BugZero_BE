package com.bugzero.rarego.app;

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

import com.bugzero.rarego.domain.Deposit;
import com.bugzero.rarego.domain.DepositStatus;
import com.bugzero.rarego.domain.PaymentMember;
import com.bugzero.rarego.domain.PaymentTransaction;
import com.bugzero.rarego.domain.Settlement;
import com.bugzero.rarego.domain.Wallet;
import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.in.dto.AuctionFinalPaymentRequestDto;
import com.bugzero.rarego.in.dto.AuctionFinalPaymentResponseDto;
import com.bugzero.rarego.out.AuctionOrderApiClient;
import com.bugzero.rarego.out.DepositRepository;
import com.bugzero.rarego.out.PaymentTransactionRepository;
import com.bugzero.rarego.out.SettlementRepository;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;
import com.bugzero.rarego.shared.payment.event.AuctionPaymentCompletedEvent;

@ExtendWith(MockitoExtension.class)
class PaymentAuctionFinalUseCaseTest {

	@InjectMocks
	private PaymentAuctionFinalUseCase paymentAuctionFinalUseCase;

	@Mock
	private AuctionOrderApiClient auctionOrderApiClient;

	@Mock
	private DepositRepository depositRepository;

	@Mock
	private PaymentTransactionRepository transactionRepository;

	@Mock
	private SettlementRepository settlementRepository;

	@Mock
	private PaymentSupport paymentSupport;

	@Mock
	private EventPublisher eventPublisher;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(paymentAuctionFinalUseCase, "paymentTimeoutDays", 3);
	}

	@Test
	@DisplayName("성공: 낙찰 결제 완료 (정산 생성 및 이벤트 발행 포함)")
	void finalPayment_Success() {
		// given
		String memberPublicId = "uuid-member-1";
		Long memberId = 1L;   // 구매자
		Long sellerId = 5L;   // 판매자
		Long auctionId = 100L;
		int finalPrice = 100000;
		int depositAmount = 10000;
		int expectedPaymentAmount = finalPrice - depositAmount;

		// Request DTO
		AuctionFinalPaymentRequestDto request = new AuctionFinalPaymentRequestDto(
			"홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");

		// Order DTO (낙찰자 ID 일치, 상태 PROCESSING, 날짜 최신)
		AuctionOrderDto order = new AuctionOrderDto(
			1L, auctionId, sellerId, memberId, finalPrice, "PROCESSING", LocalDateTime.now());

		// 구매자 & 판매자 객체 생성 (Builder 사용 가정)
		PaymentMember buyer = PaymentMember.builder()
			.id(memberId)
			.publicId(memberPublicId)
			.build();

		PaymentMember seller = PaymentMember.builder()
			.id(sellerId)
			.build();

		// 보증금 (HOLD 상태)
		Deposit deposit = Deposit.builder()
			.member(buyer)
			.auctionId(auctionId)
			.amount(depositAmount) // getAmount() 대응
			.status(DepositStatus.HOLD)
			.build();

		// 지갑 (잔액 충분)
		Wallet wallet = Wallet.builder()
			.member(buyer)
			.balance(200000)
			.holdingAmount(depositAmount)
			.build();

		// --- Stubbing (Mock 행동 정의) ---

		// 1. 유저 식별 (PublicId -> Member)
		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(buyer);

		// 2. 주문 조회
		given(auctionOrderApiClient.getOrder(auctionId)).willReturn(order);

		// 3. 보증금 조회
		given(depositRepository.findByMemberIdAndAuctionId(memberId, auctionId))
			.willReturn(Optional.of(deposit));

		// 4. 지갑 조회 (Lock)
		given(paymentSupport.findWalletByMemberIdForUpdate(memberId)).willReturn(wallet);

		// 5. 트랜잭션 기록 및 정산 생성을 위한 멤버 조회
		// UseCase 로직상 buyer와 seller를 각각 조회함
		given(paymentSupport.findMemberById(memberId)).willReturn(buyer);   // Step 4에서 호출
		given(paymentSupport.findMemberById(sellerId)).willReturn(seller);  // Step 8에서 호출

		// when
		AuctionFinalPaymentResponseDto response = paymentAuctionFinalUseCase.finalPayment(memberPublicId, auctionId,
			request);

		// then
		// 1. 응답값 검증
		assertThat(response.auctionId()).isEqualTo(auctionId);
		assertThat(response.paidAmount()).isEqualTo(expectedPaymentAmount);
		assertThat(response.status()).isEqualTo("PAID"); // Response DTO 생성 로직 확인 필요

		// 2. Wallet 잔액 및 홀딩 차감 검증
		// 200,000 - 10,000(보증금사용) - 90,000(잔금결제) = 100,000
		assertThat(wallet.getBalance()).isEqualTo(100000);
		assertThat(wallet.getHoldingAmount()).isEqualTo(0);

		// 3. Deposit 상태 변경 검증 (use() 호출 여부)
		// Deposit 엔티티 내부 로직에 따라 상태가 변경되었는지 확인
		// assertThat(deposit.getStatus()).isEqualTo(DepositStatus.USED); // 엔티티 구현에 따라 주석 해제

		// 4. 외부 호출 검증 (Verify)
		verify(transactionRepository, times(2)).save(any(PaymentTransaction.class)); // 거래내역 2건
		verify(auctionOrderApiClient).completeOrder(auctionId); // 주문 완료 요청
		verify(settlementRepository).save(any(Settlement.class)); // 정산 정보 저장 (NEW)
		verify(eventPublisher).publish(any(AuctionPaymentCompletedEvent.class)); // 이벤트 발행 (NEW)
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

		given(auctionOrderApiClient.getOrder(auctionId))
			.willThrow(new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));

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

		given(auctionOrderApiClient.getOrder(auctionId)).willReturn(order);

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

		given(auctionOrderApiClient.getOrder(auctionId)).willReturn(order);

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

		given(auctionOrderApiClient.getOrder(auctionId)).willReturn(order);
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

		given(auctionOrderApiClient.getOrder(auctionId)).willReturn(order);
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
