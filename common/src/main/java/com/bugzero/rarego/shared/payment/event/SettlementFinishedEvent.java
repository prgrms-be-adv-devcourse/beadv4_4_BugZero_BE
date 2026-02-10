package com.bugzero.rarego.shared.payment.event;

import java.util.List;

import com.bugzero.rarego.shared.payment.dto.SettlementResponseDto;

public record SettlementFinishedEvent(
	List<SettlementResponseDto> settlements,
	int totalCount,
	int totalAmount
) {
	public static SettlementFinishedEvent of(List<SettlementResponseDto> settlements) {
		int totalAmount = settlements.stream()
			.mapToInt(SettlementResponseDto::settlementAmount)
			.sum();

		return new SettlementFinishedEvent(settlements, settlements.size(), totalAmount);
	}
}
