package com.bugzero.rarego.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
	public ResponseEntity<ExceptionResponseDto> handleCustomException(CustomException e) {
		log.error("CustomException 발생: {}", e.getMessage());
		ExceptionResponseDto body = ExceptionResponseDto.from(e.getErrorType(), e.getMessage());
		return ResponseEntity
			.status(e.getErrorType().getHttpStatus())
			.body(body);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ExceptionResponseDto> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		log.error("MethodArgumentNotValidException 발생: {}", e.getMessage());

		// 에러가 발생한 필드 중 첫 번째 필드의 에러 메시지만 가져온다.
		String errorMessage = e.getBindingResult()
			.getAllErrors()
			.getFirst()
			.getDefaultMessage();

		ExceptionResponseDto body = ExceptionResponseDto.from(ErrorType.INVALID_INPUT, errorMessage);
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(body);
	}
}