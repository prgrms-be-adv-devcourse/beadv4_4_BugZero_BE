package com.bugzero.rarego.boundedContext.payment.app;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
	private final PaymentTransactionRepository paymentTransactionRepository;
	private final PaymentSupport paymentSupport;

	@Transactional(readOnly = true)
	public PagedResponseDto<WalletTransactionResponseDto> getWalletTransactions(String memberPublicId, int page,
		int size, WalletTransactionType transactionType, LocalDate from, LocalDate to) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

		LocalDateTime fromDateTime = (from != null) ? from.atStartOfDay() : null;
		LocalDateTime toDateTime = (to != null) ? to.atTime(LocalTime.MAX) : null;

		Long memberId = paymentSupport.findMemberByPublicId(memberPublicId).getId();

		Page<PaymentTransaction> transactions = paymentTransactionRepository.findAllByMemberIdAndTransactionType(
			memberId, transactionType, fromDateTime, toDateTime, pageable);

		return PagedResponseDto.from(transactions, WalletTransactionResponseDto::from);
	}
}
