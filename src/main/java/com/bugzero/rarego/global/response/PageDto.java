package com.bugzero.rarego.global.response;

import org.springframework.data.domain.Page;

public record PageDto(
	int currentPage,
	int limit,
	long totalItems,
	int totalPages,
	boolean hasNext,
	boolean hasPrevious
) {
	public static PageDto from(Page<?> page) {
		return new PageDto(
			page.getNumber() + 1, // 페이지 번호는 0부터 시작하므로 +1
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages(),
			page.hasNext(),
			page.hasPrevious()
		);
	}
}
