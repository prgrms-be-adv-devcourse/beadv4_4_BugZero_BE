package com.bugzero.rarego.global.exception;

public record ExceptionResponseDto(Integer status, String message) {

	public static ExceptionResponseDto to(Integer status, String message) {
		return new ExceptionResponseDto(status, message);
	}
}
