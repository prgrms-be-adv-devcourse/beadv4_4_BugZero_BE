package com.bugzero.rarego.boundedContext.payment.app;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;
import com.bugzero.rarego.boundedContext.payment.in.dto.WalletTransactionResponseDto;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.global.response.PagedResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentGetWalletTransactionsUseCase {
	private final PaymentSupport paymentSupport;
	private final PaymentTransactionRepository paymentTransactionRepository;

	@Transactional(readOnly = true)
	public PagedResponseDto<WalletTransactionResponseDto> getWalletTransactions(Long memberId, int page, int size,
		WalletTransactionType transactionType) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

		Page<PaymentTransaction> transactions = paymentTransactionRepository.findAllByMemberIdAndTransactionType(
			memberId,
			transactionType, pageable);

		return PagedResponseDto.from(transactions, WalletTransactionResponseDto::from);
	}
}
