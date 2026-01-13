package com.bugzero.rarego.global.response;

public record SuccessResponseDto<T>(
	Integer status,
	String message,
	T data
) {
	// SuccessType만 받는 경우 (data 없음)
	public static SuccessResponseDto<Void> from(SuccessType successType) {
		return new SuccessResponseDto<>(
			successType.getStatus(),
			successType.getMessage(),
			null
		);
	}

	// SuccessType + data 받는 경우
	public static <T> SuccessResponseDto<T> from(SuccessType successType, T data) {
		return new SuccessResponseDto<>(
			successType.getStatus(),
			successType.getMessage(),
			data
		);
	}
}