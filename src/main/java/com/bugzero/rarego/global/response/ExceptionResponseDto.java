package com.bugzero.rarego.global.response;

import com.bugzero.rarego.standard.response.ResponseDto;

public record ExceptionResponseDto(Integer status, String message) implements ResponseDto {

	public static ExceptionResponseDto to(Integer status, String message) {
		return new ExceptionResponseDto(status, message);
	}
}
