package com.bugzero.rarego.bounded_context.payment.app;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.bounded_context.payment.domain.Settlement;
import com.bugzero.rarego.bounded_context.payment.domain.SettlementStatus;
import com.bugzero.rarego.bounded_context.payment.in.dto.SettlementResponseDto;
import com.bugzero.rarego.bounded_context.payment.out.SettlementRepository;
import com.bugzero.rarego.global.response.PagedResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentGetSettlementsUseCase {
	private final SettlementRepository settlementRepository;
	private final PaymentSupport paymentSupport;

	// sellerId, status, createdAt 복합 인덱스 고려
	@Transactional(readOnly = true)
	public PagedResponseDto<SettlementResponseDto> getSettlements(String memberPublicId, int page, int size,
		SettlementStatus status, LocalDate from, LocalDate to) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

		LocalDateTime fromDateTime = (from != null) ? from.atStartOfDay() : null;
		LocalDateTime toDateTime = (to != null) ? to.plusDays(1).atStartOfDay() : null;

		Long memberId = paymentSupport.findMemberByPublicId(memberPublicId).getId();

		Page<Settlement> settlements = settlementRepository.searchSettlements(memberId, status, fromDateTime,
			toDateTime, pageable);

		return PagedResponseDto.from(settlements, SettlementResponseDto::from);
	}
}
