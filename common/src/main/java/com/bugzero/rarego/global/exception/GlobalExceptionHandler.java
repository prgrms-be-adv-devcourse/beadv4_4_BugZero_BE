package com.bugzero.rarego.global.exception;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.ExceptionResponseDto;

import jakarta.servlet.http.HttpServletRequest;
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

	@ExceptionHandler(InvalidDataAccessApiUsageException.class)
	public ExceptionResponseDto handleInvalidDataAccessApiUsageException(
		InvalidDataAccessApiUsageException e) {
		log.error("InvalidDataAccessApiUsageException 발생: {}", e.getMessage(), e);
		
		return ExceptionResponseDto.from(ErrorType.INVALID_INPUT, "데이터 처리 중 잘못된 요청이 발생했습니다: " + e.getMessage());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ExceptionResponseDto handleMethodArgumentTypeMismatchException(
		MethodArgumentTypeMismatchException e) {

		log.warn("MethodArgumentTypeMismatchException 발생: {}", e.getMessage());

		String errorMessage = String.format("파라미터 '%s'의 값이 올바르지 않습니다. (입력값: %s)",
			e.getName(), e.getValue());

		return ExceptionResponseDto.from(ErrorType.INVALID_INPUT, errorMessage);
	}

	@ExceptionHandler(Exception.class)
	public ExceptionResponseDto handleException(Exception e, HttpServletRequest request) {
		log.error("[Unhandled Exception] - URL: {} {}, Error: {}",
			request.getMethod(),
			request.getRequestURI(),
			e.getMessage(),
			e);
		return ExceptionResponseDto.from(ErrorType.INTERNAL_SERVER_ERROR, e.getMessage());
	}
}
