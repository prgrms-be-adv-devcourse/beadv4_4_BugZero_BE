package com.bugzero.rarego.boundedContext.payment.in;

import java.time.LocalDateTime;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.payment.app.PaymentFacade;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;
import com.bugzero.rarego.boundedContext.payment.in.dto.AuctionFinalPaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.AuctionFinalPaymentResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.WalletTransactionResponseDto;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "결제 관련 API")
public class PaymentController {
	private final PaymentFacade paymentFacade;
	private final JobOperator jobOperator;
	private final Job settlementJob;

	@Operation(summary = "결제 요청", description = "토스페이먼츠 결제를 요청합니다")
	@PostMapping("/charges")
	public SuccessResponseDto<PaymentRequestResponseDto> requestPayment(
		// TODO: 추후 인증 구현시 @AuthenticationPrincipal로 변경 필요
		// 현재는 테스트를 위해 Query Parameter로 받음
		@RequestParam Long memberId,
		@Valid @RequestBody PaymentRequestDto requestDto
	) {
		return SuccessResponseDto.from(SuccessType.CREATED, paymentFacade.requestPayment(memberId, requestDto));
	}

	@Operation(summary = "결제 승인", description = "토스페이먼츠 결제 승인 후 지갑에 충전합니다")
	@PostMapping("/charges/confirm")
	public SuccessResponseDto<PaymentConfirmResponseDto> confirmPayment(
		// TODO: 추후 인증 구현시 @AuthenticationPrincipal로 변경 필요
		// 현재는 테스트를 위해 Query Parameter로 받음
		@RequestParam Long memberId,
		@Valid @RequestBody PaymentConfirmRequestDto requestDto
	) {
		return SuccessResponseDto.from(SuccessType.OK, paymentFacade.confirmPayment(memberId, requestDto));
	}

	@Operation(summary = "낙찰 결제", description = "낙찰자가 최종 결제를 완료합니다 (보증금 + 잔금)")
	@PostMapping("/auctions/{auctionId}")
	public SuccessResponseDto<AuctionFinalPaymentResponseDto> auctionFinalPayment(
		// TODO: 추후 인증 구현시 @AuthenticationPrincipal로 변경 필요
		@RequestParam Long memberId,
		@PathVariable Long auctionId,
		@Valid @RequestBody AuctionFinalPaymentRequestDto requestDto
	) {
		return SuccessResponseDto.from(SuccessType.OK,
			paymentFacade.auctionFinalPayment(memberId, auctionId, requestDto));
	}

	@Operation(summary = "지갑 거래 내역 조회", description = "지갑 거래 내역을 조회합니다.")
	@GetMapping("/me/wallet-transactions")
	public SuccessResponseDto<PagedResponseDto<WalletTransactionResponseDto>> getWalletTransactions(
		// TODO: 추후 인증 구현시 @AuthenticationPrincipal로 변경 필요
		@RequestParam Long memberId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size,
		@RequestParam(required = false) WalletTransactionType transactionType
	) {
		PagedResponseDto<WalletTransactionResponseDto> response = paymentFacade.getWalletTransactions(memberId, page,
			size,
			transactionType);

		return SuccessResponseDto.from(SuccessType.OK, response);
	}

	// 로컬 정산 배치 테스트용 api
	@Hidden
	@PostMapping("/settlement")
	public SuccessResponseDto<Void> runSettlementJob() {
		try {
			// JobParameter에 실행 시간을 넣어 중복 실행 방지
			JobParameters jobParameters = new JobParametersBuilder()
				.addLocalDateTime("runAt", LocalDateTime.now())
				.toJobParameters();

			jobOperator.start(settlementJob, jobParameters);

			return SuccessResponseDto.from(SuccessType.OK);
		} catch (Exception e) {
			throw new CustomException(ErrorType.SETTLEMENT_BATCH_FAILED);
		}
	}
}
