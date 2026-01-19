package com.bugzero.rarego.boundedContext.payment.app;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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

	public PagedResponseDto<SettlementResponseDto> getSettlements(Long memberId, int page, int size,
		SettlementStatus status, LocalDateTime from, LocalDateTime to) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

		Page<Settlement> settlements = settlementRepository.findAllBySellerIdAndStatus(memberId, status, from, to,
			pageable);

		return PagedResponseDto.from(settlements, SettlementResponseDto::from);
	}
}
