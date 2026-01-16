package com.bugzero.rarego.boundedContext.payment.in;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.payment.app.PaymentFacade;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.payment.dto.DepositHoldRequestDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/payments/deposits")
@RequiredArgsConstructor
@Tag(name = "Internal - Payment", description = "내부 결제 API (시스템 전용)")
@Hidden
public class InternalPaymentController {
    private final PaymentFacade paymentFacade;

    @Operation(summary = "보증금 예치", description = "경매 입찰 시 보증금을 예치합니다")
    @PostMapping("/hold")
    public SuccessResponseDto<DepositHoldResponseDto> holdDeposit(
            @Valid @RequestBody DepositHoldRequestDto request) {
        return SuccessResponseDto.from(SuccessType.CREATED, paymentFacade.holdDeposit(request));
    }
}
