package com.bugzero.rarego.global.response;

import java.util.List;

import org.springframework.data.domain.Page;

public record PagedResponseDto<T>(
	List<T> data,
	PageDto pageDto
) {
	public static <T> PagedResponseDto<T> from(Page<T> page) {
		return new PagedResponseDto<>(page.getContent(), PageDto.from(page));
	}
}
