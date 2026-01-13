package com.bugzero.rarego.global.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ExceptionResponseDto handleCustomException(CustomException e) {
		log.error("CustomException 발생: {}", e.getMessage());
		return ExceptionResponseDto.to(e.getErrorType().getHttpStatus(), e.getErrorType().getMessage());
	}

}
