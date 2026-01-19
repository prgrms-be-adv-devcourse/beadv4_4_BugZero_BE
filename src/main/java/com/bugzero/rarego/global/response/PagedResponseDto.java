package com.bugzero.rarego.global.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

public record PagedResponseDto<T>(
	List<T> data,
	PageDto pageDto
) {
	// Dto를 페이지로 받을 때
	public static <T> PagedResponseDto<T> from(Page<T> page) {
		return new PagedResponseDto<>(page.getContent(), PageDto.from(page));
	}

	// Entity page를 dto로 변환하면서 받을 때
	public static <R, T> PagedResponseDto<T> from(Page<R> page, Function<R, T> mapper) {
		List<T> content = page.getContent().stream()
			.map(mapper)
			.toList();

		return new PagedResponseDto<>(content, PageDto.from(page));
	}
}
