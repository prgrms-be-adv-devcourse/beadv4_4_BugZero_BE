package com.bugzero.rarego.global.response;

import com.bugzero.rarego.standard.response.ResponseDto;

public record ExceptionResponseDto(
		Integer status,
		int code,
		String message) implements ResponseDto {

	public static ExceptionResponseDto from(ErrorType errorType) {
		return new ExceptionResponseDto(
				errorType.getHttpStatus(),
				errorType.getCode(),
				errorType.getMessage());
	}

	public static ExceptionResponseDto from(ErrorType errorType, String message) {
		return new ExceptionResponseDto(
				errorType.getHttpStatus(),
				errorType.getCode(),
				message);
	}

	public static ExceptionResponseDto from(Integer status, int code, String message) {
		return new ExceptionResponseDto(status, code, message);
	}
}
