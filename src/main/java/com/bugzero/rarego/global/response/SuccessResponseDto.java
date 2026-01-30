package com.bugzero.rarego.global.response;

import com.bugzero.rarego.standard.response.ResponseDto;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuccessResponseDto<T>(
	Integer status,
	String message,
	T data
) implements ResponseDto {
	// SuccessType만 받는 경우 (data 없음)
	public static SuccessResponseDto<Void> from(SuccessType successType) {
		return new SuccessResponseDto<>(
			successType.getHttpStatus(),
			successType.getMessage(),
			null
		);
	}

	// SuccessType + data 받는 경우
	public static <T> SuccessResponseDto<T> from(SuccessType successType, T data) {
		return new SuccessResponseDto<>(
			successType.getHttpStatus(),
			successType.getMessage(),
			data
		);
	}
}