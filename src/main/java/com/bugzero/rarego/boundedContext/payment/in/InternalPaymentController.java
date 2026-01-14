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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/payments/deposits")
@RequiredArgsConstructor
public class InternalPaymentController {
    private final PaymentFacade paymentFacade;

    @PostMapping("/hold")
    public SuccessResponseDto<DepositHoldResponseDto> holdDeposit(
            @Valid @RequestBody DepositHoldRequestDto request) {
        return SuccessResponseDto.from(SuccessType.CREATED, paymentFacade.holdDeposit(request));
    }
}
