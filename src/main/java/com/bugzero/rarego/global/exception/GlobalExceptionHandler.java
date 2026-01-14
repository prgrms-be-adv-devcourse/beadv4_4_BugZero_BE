package com.bugzero.rarego.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.bugzero.rarego.global.response.ExceptionResponseDto;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ExceptionResponseDto handleCustomException(CustomException e) {
		log.error("CustomException 발생: {}", e.getMessage());
		return ExceptionResponseDto.to(e.getErrorType().getHttpStatus(), e.getErrorType().getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ExceptionResponseDto handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		log.error("MethodArgumentNotValidException 발생: {}", e.getMessage());
		return ExceptionResponseDto.to(HttpStatus.BAD_REQUEST.value(), e.getMessage());
	}
}
