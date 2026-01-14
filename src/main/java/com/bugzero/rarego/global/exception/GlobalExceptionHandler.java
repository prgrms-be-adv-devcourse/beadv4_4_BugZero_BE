package com.bugzero.rarego.global.exception;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.ExceptionResponseDto;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ExceptionResponseDto handleCustomException(CustomException e) {
		log.error("CustomException 발생: {}", e.getMessage());
		return ExceptionResponseDto.from(e.getErrorType(), e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ExceptionResponseDto handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		log.error("MethodArgumentNotValidException 발생: {}", e.getMessage());

		// 에러가 발생한 필드 중 첫 번째 필드의 에러 메시지만 가져온다.
		String errorMessage = e.getBindingResult()
				.getAllErrors()
				.getFirst()
				.getDefaultMessage();

		return ExceptionResponseDto.from(ErrorType.INVALID_INPUT, errorMessage);
	}
}
