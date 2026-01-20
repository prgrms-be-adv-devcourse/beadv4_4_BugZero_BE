package com.bugzero.rarego.boundedContext.payment.app;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.in.dto.SettlementResponseDto;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;
import com.bugzero.rarego.global.response.PagedResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentGetSettlementsUseCase {
	private final SettlementRepository settlementRepository;
	private final PaymentSupport paymentSupport;

	@Transactional(readOnly = true)
	public PagedResponseDto<SettlementResponseDto> getSettlements(String memberPublicId, int page, int size,
		SettlementStatus status, LocalDate from, LocalDate to) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

		LocalDateTime fromDateTime = (from != null) ? from.atStartOfDay() : null;
		LocalDateTime toDateTime = (to != null) ? to.atTime(java.time.LocalTime.MAX) : null;

		Long memberId = paymentSupport.findMemberByPublicId(memberPublicId).getId();

		Page<Settlement> settlements = settlementRepository.findAllBySellerIdAndStatus(memberId, status, fromDateTime,
			toDateTime, pageable);

		return PagedResponseDto.from(settlements, SettlementResponseDto::from);
	}
}
