package com.bugzero.rarego.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ExceptionResponseDto> handleCustomException(CustomException e) {
		log.error("CustomException 발생: {}", e.getMessage());
		return ResponseEntity.status(e.getErrorType().getHttpStatus())
			.body(ExceptionResponseDto.to(e.getErrorType().getHttpStatus(), e.getErrorType().getMessage()));
	}

}
