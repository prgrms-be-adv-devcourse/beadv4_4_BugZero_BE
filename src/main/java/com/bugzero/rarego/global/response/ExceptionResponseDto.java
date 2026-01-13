package com.bugzero.rarego.global.response;

public record ExceptionResponseDto(Integer status, String message) {

	public static ExceptionResponseDto to(Integer status, String message) {
		return new ExceptionResponseDto(status, message);
	}
}
