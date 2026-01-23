package com.bugzero.rarego.boundedContext.payment.in;

import com.bugzero.rarego.boundedContext.payment.app.PaymentFacade;
import com.bugzero.rarego.boundedContext.payment.in.dto.RefundResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.payment.dto.DepositHoldRequestDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/payments")
@RequiredArgsConstructor
@Tag(name = "Internal - Payment", description = "내부 결제 API (시스템 전용)")
@Hidden
public class InternalPaymentController {
    private final PaymentFacade paymentFacade;

    @Operation(summary = "보증금 예치", description = "경매 입찰 시 보증금을 예치합니다")
    @PostMapping("/deposits/hold")
    public SuccessResponseDto<DepositHoldResponseDto> holdDeposit(
            @Valid @RequestBody DepositHoldRequestDto request) {
        return SuccessResponseDto.from(SuccessType.CREATED, paymentFacade.holdDeposit(request));
    }

    @Operation(summary = "환불 처리", description = "운영자가 결제 완료된 건을 환불 처리합니다")
    @PostMapping("/refunds/{auctionId}")
    public SuccessResponseDto<RefundResponseDto> processRefund(
            @PathVariable Long auctionId) {
        return SuccessResponseDto.from(SuccessType.OK, paymentFacade.processRefund(auctionId));
    }

    @Operation(summary = "처리 중인 주문이 있는지 확인", description = "사용자가 처리 중인 주문이 있는지 확인합니다")
    @GetMapping("/members/{publicId}/orders/processing")
    public SuccessResponseDto<Boolean> hasProcessingOrders(@PathVariable String publicId) {
        return SuccessResponseDto.from(SuccessType.OK, paymentFacade.hasProcessingOrders(publicId));
    }
}
