package com.bugzero.rarego.global.response;

public record SuccessResponseDto<T>(
	Integer status,
	String message,
	T data
) {
	public static <T> SuccessResponseDto<T> of(Integer status, String message, T data) {
		return new SuccessResponseDto<>(status, message, data);
	}

	public static SuccessResponseDto<Void> of(Integer status, String message) {
		return new SuccessResponseDto<>(status, message, null);
	}
}
