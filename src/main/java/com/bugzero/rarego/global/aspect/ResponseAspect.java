package com.bugzero.rarego.global.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.standard.response.ResponseDto;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * API 응답 처리용 Aspect
 * 컨트롤러나 예외 핸들러가 반환하는 ResponseDto 객체를 가로채서,
 * DTO 내부에 정의된 상태 코드(status)를 실제 HTTP 응답의 상태 코드로 설정합니다.
 * 이를 통해 컨트롤러나 예외 핸들러에서 ResponseEntity로 감싸지 않고 Dto만 반환해도 올바른 HTTP Status가 전송됩니다.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class ResponseAspect {
	private final HttpServletResponse response;

	@Pointcut("within(@org.springframework.web.bind.annotation.RestController *) || " +
		"within(@org.springframework.web.bind.annotation.RestControllerAdvice *)")
	public void controllerOrAdvice() {
	}

	@Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
		"@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
		"@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
		"@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
		"@annotation(org.springframework.web.bind.annotation.PatchMapping) || " +
		"@annotation(org.springframework.web.bind.annotation.RequestMapping)")
	public void httpMapping() {
	}

	@Pointcut("@annotation(org.springframework.web.bind.annotation.ExceptionHandler)")
	public void exceptionHandler() {
	}

	@Pointcut("@annotation(org.springframework.web.bind.annotation.ResponseBody)")
	public void responseBody() {
	}

	@Around("(controllerOrAdvice() && (httpMapping() || exceptionHandler())) || responseBody()")
	public Object responseAspect(ProceedingJoinPoint joinPoint) throws Throwable {
		Object result = joinPoint.proceed();

		if (result instanceof ResponseDto responseDto) {
			response.setStatus(responseDto.status());
		}

		return result;
	}
}
