package com.bugzero.rarego.global.response;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SuccessType {

	OK(HttpStatus.OK.value(), "요청이 성공적으로 처리되었습니다."),
	CREATED(HttpStatus.CREATED.value(), "생성이 완료되었습니다.");

	private final Integer httpStatus;
	private final String message;
}
